package com.chinmay.edgegate.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlateNormalizerTest {

    @Test fun cleanStandardPlate() {
        val p = PlateNormalizer.parse("KA 01 AB 1234")!!
        assertEquals("KA01AB1234", p.plate)
        assertEquals("KA 01 AB 1234", p.display)
        assertEquals(0, p.corrections)
    }

    @Test fun punctuationAndLowercaseAreIgnored() {
        assertEquals("MH12DE0045", PlateNormalizer.normalize("mh-12 de.0045"))
    }

    @Test fun fixesDigitLetterConfusionByPosition() {
        // O in the number part -> 0, 8 in the series -> B, I in RTO -> 1
        val p = PlateNormalizer.parse("KA0I 8B 12O4")!!
        assertEquals("KA01BB1204", p.plate)
        assertEquals(3, p.corrections)
    }

    @Test fun stripsIndHologramText() {
        assertEquals("TN09CK4321", PlateNormalizer.normalize("IND TN 09 CK 4321"))
    }

    @Test fun delhiSingleDigitRto() {
        assertEquals("DL3CAB1234", PlateNormalizer.normalize("DL 3C AB 1234"))
    }

    @Test fun bharatSeries() {
        val p = PlateNormalizer.parse("22 BH 1234 AA")!!
        assertEquals("22BH1234AA", p.plate)
        assertEquals(PlateFormat.BHARAT, p.format)
        assertEquals("22 BH 1234 AA", p.display)
    }

    @Test fun rejectsUnknownStateCode() {
        assertNull(PlateNormalizer.normalize("XX01AB1234"))
    }

    @Test fun rejectsRandomText() {
        assertNull(PlateNormalizer.normalize("SUPERMARKET"))
        assertNull(PlateNormalizer.normalize("OPEN 24X7"))
        assertNull(PlateNormalizer.normalize(""))
    }

    @Test fun twoRowPlateFromSeparateLines() {
        val p = PlateNormalizer.bestFromLines(listOf("Shop No 4", "KA 05", "MX 7788"))!!
        assertEquals("KA05MX7788", p.plate)
    }

    @Test fun prefersExactReadOverCorrectedRead() {
        // "KA01AB123" is valid as 3-digit number; must not be "corrected" to KA01A8123.
        assertEquals("KA01AB123", PlateNormalizer.normalize("KA01AB123"))
    }
}
