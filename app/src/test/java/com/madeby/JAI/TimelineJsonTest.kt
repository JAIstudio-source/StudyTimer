package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineJsonTest {

    private fun e(ts: Long, state: String, id: String? = null) = TimelineEntry(ts, state, id)

    @Test
    fun empty_roundTrips() {
        val raw = timelineToJsonString(emptyList())
        assertEquals("[]", raw)
        assertEquals(emptyList<TimelineEntry>(), parseTimelineJson(raw))
    }

    @Test
    fun basic_roundTrips() {
        val entries = listOf(e(1700000000000L, "STUDYING"), e(1700000001800L, "IDLE"))
        val raw = timelineToJsonString(entries)
        assertEquals("[{\"t\":1700000000000,\"s\":\"STUDYING\"},{\"t\":1700000001800,\"s\":\"IDLE\"}]", raw)
        assertEquals(entries, parseTimelineJson(raw))
    }

    @Test
    fun withId_roundTrips() {
        val entries = listOf(e(123L, "BREAK", "abc-123"))
        val raw = timelineToJsonString(entries)
        assertEquals("[{\"t\":123,\"s\":\"BREAK\",\"id\":\"abc-123\"}]", raw)
        assertEquals(entries, parseTimelineJson(raw))
    }

    @Test
    fun emptyId_becomesNull_onParse() {
        val raw = timelineToJsonString(listOf(e(1L, "STUDYING", "")))
        assertNull(parseTimelineJson(raw).single().id)
    }

    @Test
    fun escapedStrings_roundTrip() {
        val entries = listOf(e(7L, "S\"T", "quote\\back\nslash\tid"))
        val raw = timelineToJsonString(entries)
        assertEquals(entries, parseTimelineJson(raw))
    }

    @Test
    fun parsesLegacyOrgJsonOutput() {
        val legacy = "[{\"t\":1700000000000,\"s\":\"STUDYING\",\"id\":\"x\\\"y\"}," +
            "{\"t\":1700000000001,\"s\":\"IDLE\"}]"
        val parsed = parseTimelineJson(legacy)
        assertEquals(2, parsed.size)
        assertEquals(1700000000000L, parsed[0].timestamp)
        assertEquals("STUDYING", parsed[0].state)
        assertEquals("x\"y", parsed[0].id)
        assertEquals(1700000000001L, parsed[1].timestamp)
        assertEquals("IDLE", parsed[1].state)
        assertNull(parsed[1].id)
    }

    @Test
    fun handlesUnicodeEscape() {
        val parsed = parseTimelineJson("[{\"t\":1,\"s\":\"a\\u00e9b\"}]")
        assertEquals("aéb", parsed.single().state)
    }

    @Test
    fun malformedInput_returnsEmpty() {
        assertTrue(parseTimelineJson("").isEmpty())
        assertTrue(parseTimelineJson("not json").isEmpty())
        assertTrue(parseTimelineJson("{").isEmpty())
    }

    @Test
    fun missingKeys_useDefaults() {
        val parsed = parseTimelineJson("[{\"t\":42}]")
        assertEquals(TimelineEntry(42L, "", null), parsed.single())
    }

    @Test
    fun insertSorted_intoEmpty() {
        assertEquals(listOf(e(5L, "X")), insertEntrySorted(emptyList(), e(5L, "X")))
    }

    @Test
    fun insertSorted_beforeAndAfter() {
        val base = listOf(e(1L, "A"), e(3L, "B"))
        assertEquals(listOf(e(0L, "Z"), e(1L, "A"), e(3L, "B")), insertEntrySorted(base, e(0L, "Z")))
        assertEquals(listOf(e(1L, "A"), e(2L, "M"), e(3L, "B")), insertEntrySorted(base, e(2L, "M")))
        assertEquals(listOf(e(1L, "A"), e(3L, "B"), e(9L, "Y")), insertEntrySorted(base, e(9L, "Y")))
    }

    @Test
    fun insertSorted_afterEqualTimestamps() {
        val base = listOf(e(1L, "A"), e(1L, "B"))
        val out = insertEntrySorted(base, e(1L, "C"))
        assertEquals(listOf(e(1L, "A"), e(1L, "B"), e(1L, "C")), out)
    }

    @Test
    fun removeEntryWithTs_removesAllMatches() {
        val entries = listOf(e(1L, "A"), e(2L, "B"), e(1L, "C"))
        assertEquals(listOf(e(2L, "B")), removeEntryWithTs(entries, 1L))
    }

    @Test
    fun moveEntry_resorts() {
        val entries = listOf(e(1L, "A"), e(2L, "B"), e(5L, "C"))
        val out = moveEntryInList(entries, 1L, 3L)
        assertEquals(listOf(e(2L, "B"), e(3L, "A"), e(5L, "C")), out)
    }

    @Test
    fun moveEntry_returnsNull_whenAbsent() {
        assertNull(moveEntryInList(listOf(e(1L, "A")), 99L, 3L))
    }

    @Test
    fun removeDayEntries_filtersByDate() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val base = 1_700_000_000_000L
        val entries = listOf(
            e(base, "STUDYING"),
            e(base + 86400000L, "IDLE"),
            e(base + 2 * 86400000L, "BREAK"),
            e(0L, "BAD")
        )
        val day = fmt.format(Date(base))
        val out = removeDayEntries(entries, day, fmt)
        assertEquals(2, out.size)
        assertEquals(base + 86400000L, out[0].timestamp)
        assertEquals(base + 2 * 86400000L, out[1].timestamp)
    }
}
