package com.chinmay.edgegate.core

/** One row of the gate register, as exported for the society office / auditors. */
data class GateLogRow(
    val timestamp: String,
    val plate: String,
    val direction: String,
    val action: String,
    val category: String,
    val confidence: Float,
    val source: String,
    val note: String?,
)

object CsvExporter {
    private val HEADER = listOf("timestamp", "plate", "direction", "action", "category", "confidence", "source", "note")

    fun toCsv(rows: List<GateLogRow>): String = buildString {
        appendLine(HEADER.joinToString(","))
        for (r in rows) {
            appendLine(
                listOf(
                    r.timestamp, r.plate, r.direction, r.action, r.category,
                    "%.2f".format(java.util.Locale.US, r.confidence), r.source, r.note ?: "",
                ).joinToString(",") { escape(it) }
            )
        }
    }

    /** RFC 4180 quoting + guard against spreadsheet formula injection. */
    internal fun escape(value: String): String {
        var v = value
        if (v.isNotEmpty() && v[0] in "=+-@") v = "'$v"
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else v
    }
}
