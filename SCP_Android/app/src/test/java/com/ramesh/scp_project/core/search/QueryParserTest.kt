package com.ramesh.scp_project.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueryParserTest {

    private val parser = QueryParser()

    @Test
    fun `parse extracts merchant and relative time filters`() {
        val parsed = parser.parse("amazon last week laptop invoice")

        assertEquals("laptop invoice", parsed.text)
        assertEquals(7, parsed.timeFilterDays)
        assertEquals("amazon", parsed.merchant)
    }

    @Test
    fun `parse keeps free text when no shortcut is present`() {
        val parsed = parser.parse("tax receipt april")

        assertEquals("tax receipt april", parsed.text)
        assertNull(parsed.timeFilterDays)
        assertNull(parsed.merchant)
    }
}
