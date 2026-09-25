package com.chinmay.edgegate.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilTest {

    @Test fun percentilesOverWindow() {
        val s = RollingStats(capacity = 10)
        (1..10).forEach { s.add(it.toDouble()) }
        assertEquals(5.5, s.mean(), 1e-9)
        assertEquals(5.0, s.percentile(50.0), 1e-9)
        assertEquals(9.0, s.percentile(90.0), 1e-9)
        s.add(100.0) // evicts 1.0
        assertEquals(10, s.count)
        assertEquals(100.0, s.percentile(100.0), 1e-9)
    }

    @Test fun csvEscapesCommasQuotesAndFormulas() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""))
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"))
        val csv = CsvExporter.toCsv(
            listOf(GateLogRow("2026-09-25 10:00", "KA01AB1234", "ENTRY", "ALLOW", "RESIDENT", 0.93f, "AI", null))
        )
        assertTrue(csv.lines()[1].startsWith("2026-09-25 10:00,KA01AB1234,ENTRY,ALLOW,RESIDENT,0.93,AI,"))
    }
}
