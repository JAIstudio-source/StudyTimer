package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class StreakCalculatorTest {

    private fun base(): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.JULY, 13, 12, 0, 0)
        }

    private fun date(base: Calendar, dayOffset: Int): Date {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = base.timeInMillis }
        c.add(Calendar.DAY_OF_YEAR, dayOffset)
        return c.time
    }

    @Test
    fun qualifyingThreshold_is2700Seconds() {
        assertEquals(2700L, StreakCalculator.QUALIFYING_SECS)
    }

    @Test
    fun longestStreak_dayExactlyAtThresholdQualifies() {
        val b = base()
        val days = listOf(date(b, 0) to StreakCalculator.QUALIFYING_SECS)
        assertEquals(1, StreakCalculator.longestStreak(days))
    }

    @Test
    fun longestStreak_dayBelowThresholdDoesNotQualify() {
        val b = base()
        val days = listOf(date(b, 0) to (StreakCalculator.QUALIFYING_SECS - 1L))
        assertEquals(0, StreakCalculator.longestStreak(days))
    }

    @Test
    fun longestStreak_consecutiveQualifiedDays() {
        val b = base()
        val days = listOf(
            date(b, 0) to 2700L,
            date(b, 1) to 3000L,
            date(b, 2) to 3600L
        )
        assertEquals(3, StreakCalculator.longestStreak(days))
    }

    @Test
    fun longestStreak_breaksOnUnqualifiedDay() {
        val b = base()
        val days = listOf(
            date(b, 0) to 2700L,
            date(b, 1) to 3000L,
            date(b, 2) to 1000L,
            date(b, 3) to 2700L,
            date(b, 4) to 3600L
        )
        assertEquals(2, StreakCalculator.longestStreak(days))
    }

    @Test
    fun longestStreak_gapBreaksRun() {
        val b = base()
        val days = listOf(
            date(b, 0) to 2700L,
            date(b, 2) to 2700L,
            date(b, 3) to 2700L
        )
        assertEquals(2, StreakCalculator.longestStreak(days))
    }

    @Test
    fun longestStreak_emptyOrUnqualified() {
        assertEquals(0, StreakCalculator.longestStreak(emptyList()))
        val b = base()
        assertEquals(0, StreakCalculator.longestStreak(listOf(date(b, 0) to 1000L)))
    }

    @Test
    fun currentStreak_countsTodayWhenQualified() {
        val focus = mapOf(0 to 3000L, 1 to 2700L, 2 to 2700L, 3 to 0L)
        assertEquals(3, StreakCalculator.currentStreak(focusOn = { focus[it] ?: 0L }))
    }

    @Test
    fun currentStreak_unfinishedToday_keepsRunFromYesterday() {
        val focus = mapOf(0 to 500L, 1 to 2700L, 2 to 2700L, 3 to 0L)
        assertEquals(2, StreakCalculator.currentStreak(focusOn = { focus[it] ?: 0L }))
    }

    @Test
    fun currentStreak_brokenYesterday_returnsZero() {
        val focus = mapOf(0 to 500L, 1 to 100L, 2 to 2700L)
        assertEquals(0, StreakCalculator.currentStreak(focusOn = { focus[it] ?: 0L }))
    }

    @Test
    fun currentStreak_todayExtra_pushesOverThreshold() {
        val focus = mapOf(0 to 2000L, 1 to 2700L, 2 to 0L)
        assertEquals(2, StreakCalculator.currentStreak(focusOn = { focus[it] ?: 0L }, todayExtra = 700L))
    }

    @Test
    fun currentStreak_zeroData_returnsZero() {
        assertEquals(0, StreakCalculator.currentStreak(focusOn = { 0L }))
    }
}
