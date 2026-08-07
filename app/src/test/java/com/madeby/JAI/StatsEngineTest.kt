package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsEngineTest {

    private val base = 1_000_000_000_000L

    private fun t(min: Long) = base + min * 60_000L

    private fun studying(min: Long) = TimelineEntry(t(min), "STUDYING")
    private fun manualFocus(min: Long) = TimelineEntry(t(min), "MANUAL_FOCUS")
    private fun breakState(min: Long) = TimelineEntry(t(min), "BREAK")
    private fun idle(min: Long) = TimelineEntry(t(min), "IDLE")

    @Test
    fun emptyTimeline_producesNoBlocks() {
        val parsed = parseDayBlocks(emptyList())
        assertTrue(parsed.sessions.isEmpty())
        assertTrue(parsed.breaks.isEmpty())
        assertNull(parsed.openFocusStart)
        assertNull(parsed.openBreakStart)
    }

    @Test
    fun singleFocusSession_isClosedByIdle() {
        val parsed = parseDayBlocks(listOf(studying(0), idle(30)))
        assertEquals(1, parsed.sessions.size)
        val s = parsed.sessions[0]
        assertEquals(t(0), s.startMs)
        assertEquals(t(30), s.endMs)
        assertEquals(1800L, s.secs)
        assertEquals(false, s.manual)
        assertTrue(parsed.breaks.isEmpty())
    }

    @Test
    fun focusBreakFocus_producesTwoSessionsAndOneBreak() {
        val parsed = parseDayBlocks(listOf(
            studying(0),
            breakState(25),
            studying(30),
            idle(60)
        ))
        assertEquals(2, parsed.sessions.size)
        assertEquals(1500L, parsed.sessions[0].secs)
        assertEquals(1800L, parsed.sessions[1].secs)
        assertEquals(1, parsed.breaks.size)
        assertEquals(300L, parsed.breaks[0].secs)
    }

    @Test
    fun manualFocus_isMarkedManual() {
        val parsed = parseDayBlocks(listOf(manualFocus(0), idle(45)))
        assertEquals(1, parsed.sessions.size)
        assertEquals(2700L, parsed.sessions[0].secs)
        assertTrue(parsed.sessions[0].manual)
    }

    @Test
    fun idleOnly_closesNothing() {
        val parsed = parseDayBlocks(listOf(idle(0), idle(10)))
        assertTrue(parsed.sessions.isEmpty())
        assertTrue(parsed.breaks.isEmpty())
    }

    @Test
    fun openFocusAtEnd_isReportedAsOpen() {
        val parsed = parseDayBlocks(listOf(studying(0)))
        assertTrue(parsed.sessions.isEmpty())
        assertEquals(t(0), parsed.openFocusStart)
        assertEquals(false, parsed.openFocusManual)
    }

    @Test
    fun consecutiveFocusLongerThanDay_skipsFirstSession() {
        val second = 26L * 60L
        val parsed = parseDayBlocks(listOf(
            studying(0),
            studying(second),
            idle(second + 60L)
        ))
        assertEquals(1, parsed.sessions.size)
        assertEquals(3600L, parsed.sessions[0].secs)
    }

    @Test
    fun consecutiveFocusWithinDay_countsBothSessions() {
        val parsed = parseDayBlocks(listOf(
            studying(0),
            studying(30),
            idle(60)
        ))
        assertEquals(2, parsed.sessions.size)
        assertEquals(1800L, parsed.sessions[0].secs)
        assertEquals(1800L, parsed.sessions[1].secs)
    }

    @Test
    fun openBreakAtEnd_isReportedAsOpen() {
        val parsed = parseDayBlocks(listOf(breakState(0)))
        assertTrue(parsed.breaks.isEmpty())
        assertEquals(t(0), parsed.openBreakStart)
    }
}
