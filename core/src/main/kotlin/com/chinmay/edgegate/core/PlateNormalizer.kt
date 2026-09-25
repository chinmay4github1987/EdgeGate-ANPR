package com.chinmay.edgegate.core

enum class PlateFormat { STANDARD, BHARAT }

/**
 * A plate string after cleaning and format-aware correction.
 *
 * @property plate       canonical form with no spaces, e.g. "KA01AB1234" – used as the DB key.
 * @property display     human form, e.g. "KA 01 AB 1234".
 * @property corrections how many characters were swapped by the confusion map (O->0, B->8 ...).
 *                       0 means OCR was already exactly right; higher = less trustworthy.
 */
data class PlateCandidate(
    val plate: String,
    val display: String,
    val format: PlateFormat,
    val corrections: Int,
    internal val layoutPenalty: Int = 0,
)

/**
 * Turns noisy OCR text into a valid Indian registration number.
 *
 * Why this matters for Edge AI: a small on-device OCR model is fast but makes
 * "shape" mistakes (0↔O, 8↔B, 5↔S, 1↔I). Instead of shipping a bigger model,
 * we exploit domain knowledge – the plate grammar tells us *which* positions
 * must be letters and which must be digits – and fix the error in microseconds
 * with zero extra compute on the NPU/GPU.
 *
 * Supported grammars:
 *  - Standard:  SS RR XXX NNNN   state(2 letters) RTO(1-2 digits) series(0-3 letters) number(1-4 digits)
 *               e.g. KA01AB1234, MH12DE0045, DL3CAB1234
 *  - Bharat:    YY BH NNNN XX    e.g. 22BH1234AA
 */
object PlateNormalizer {

    /** RTO state / UT codes in current use (includes legacy OR/UA and new TG). */
    val STATE_CODES: Set<String> = setOf(
        "AN", "AP", "AR", "AS", "BR", "CG", "CH", "DD", "DL", "DN", "GA", "GJ", "HP", "HR",
        "JH", "JK", "KA", "KL", "LA", "LD", "MH", "ML", "MN", "MP", "MZ", "NL", "OD", "OR",
        "PB", "PY", "RJ", "SK", "TG", "TN", "TR", "TS", "UA", "UK", "UP", "WB",
    )

    /** Letter that OCR produced where a digit is required. */
    private val LETTER_TO_DIGIT = mapOf(
        'O' to '0', 'Q' to '0', 'D' to '0', 'U' to '0',
        'I' to '1', 'L' to '1', 'J' to '1',
        'Z' to '2', 'S' to '5', 'B' to '8', 'G' to '6', 'T' to '7', 'A' to '4',
    )

    /** Digit that OCR produced where a letter is required. */
    private val DIGIT_TO_LETTER = mapOf(
        '0' to 'O', '1' to 'I', '2' to 'Z', '4' to 'A', '5' to 'S',
        '6' to 'G', '7' to 'T', '8' to 'B',
    )

    fun clean(raw: String): String =
        raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    /** Returns the best valid interpretation of [raw], or null if it cannot be a plate. */
    fun parse(raw: String): PlateCandidate? {
        val s = clean(raw)
        val variants = linkedSetOf(s)
        // High-security plates carry an "IND" hologram strip that OCR often reads.
        if (s.startsWith("IND")) variants += s.removePrefix("IND")
        if (s.endsWith("IND")) variants += s.removeSuffix("IND")
        return variants
            .mapNotNull { parseClean(it) }
            .minWithOrNull(compareBy({ it.corrections + it.layoutPenalty }, { it.corrections }))
    }

    /** Convenience: canonical plate or null. */
    fun normalize(raw: String): String? = parse(raw)?.plate

    private fun parseClean(s: String): PlateCandidate? {
        if (s.length !in 6..11) return null
        val candidates = mutableListOf<PlateCandidate>()

        parseBharat(s)?.let(candidates::add)

        for (rto in intArrayOf(2, 1)) {
            for (num in intArrayOf(4, 3, 2, 1)) {
                val series = s.length - 2 - rto - num
                if (series !in 0..3) continue
                val pattern = "LL" + "D".repeat(rto) + "L".repeat(series) + "D".repeat(num)
                val (fixed, cost) = coerce(s, pattern) ?: continue
                if (fixed.substring(0, 2) !in STATE_CODES) continue
                val rtoPart = fixed.substring(2, 2 + rto)
                if (rtoPart.all { it == '0' }) continue
                val numberPart = fixed.takeLast(num)
                if (numberPart.all { it == '0' }) continue
                // Most plates use a 2-digit RTO and 4-digit number (HSRP pads with zeros);
                // other layouts are legal but less likely, so they carry a small penalty.
                val penalty = (if (rto == 1) 1 else 0) + (4 - num)
                val seriesPart = fixed.substring(2 + rto, 2 + rto + series)
                val display = listOf(fixed.substring(0, 2), rtoPart, seriesPart, numberPart)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                candidates += PlateCandidate(fixed, display, PlateFormat.STANDARD, cost, penalty)
            }
        }
        return candidates.minWithOrNull(compareBy({ it.corrections + it.layoutPenalty }, { it.corrections }))
    }

    private fun parseBharat(s: String): PlateCandidate? {
        if (s.length !in 9..10) return null
        val pattern = "DDLLDDDD" + "L".repeat(s.length - 8)
        val (fixed, cost) = coerce(s, pattern) ?: return null
        if (fixed.substring(2, 4) != "BH") return null
        val year = fixed.substring(0, 2).toInt()
        if (year < 21) return null // BH series started in 2021
        val display = "${fixed.substring(0, 2)} BH ${fixed.substring(4, 8)} ${fixed.substring(8)}"
        return PlateCandidate(fixed, display, PlateFormat.BHARAT, cost)
    }

    /**
     * Forces each char of [s] into the class demanded by [pattern] ('L' letter, 'D' digit).
     * Returns the fixed string and how many chars were changed, or null if impossible.
     */
    private fun coerce(s: String, pattern: String): Pair<String, Int>? {
        if (s.length != pattern.length) return null
        val out = StringBuilder(s.length)
        var cost = 0
        for (i in s.indices) {
            val c = s[i]
            when (pattern[i]) {
                'L' -> if (c.isLetter()) out.append(c) else {
                    out.append(DIGIT_TO_LETTER[c] ?: return null); cost++
                }
                'D' -> if (c.isDigit()) out.append(c) else {
                    out.append(LETTER_TO_DIGIT[c] ?: return null); cost++
                }
            }
        }
        return out.toString() to cost
    }

    /**
     * OCR on a full frame returns many text lines (shop boards, stickers, two-row plates).
     * Try each line and each adjacent pair of lines (two-row plates on bikes/trucks),
     * and return the most plausible plate.
     */
    fun bestFromLines(lines: List<String>): PlateCandidate? {
        val options = buildList {
            addAll(lines)
            for (i in 0 until lines.size - 1) add(lines[i] + lines[i + 1])
            if (lines.size > 1) add(lines.joinToString(""))
        }
        return options
            .mapNotNull { parse(it) }
            .minWithOrNull(compareBy({ it.corrections + it.layoutPenalty }, { it.corrections }))
    }
}
