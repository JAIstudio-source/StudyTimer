package com.madeby.JAI

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WeekHelperTest {

    private fun utc(y: Int, m: Int, d: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y, m, d, 12, 0, 0)
        }

    @Test
    fun mondayOf_midweek_returnsSameMonday() {
        val mon = WeekHelper.mondayOf(utc(2026, Calendar.JULY, 15))
        assertEquals(Calendar.MONDAY, mon.get(Calendar.DAY_OF_WEEK))
        assertEquals(13, mon.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun mondayOf_sunday_belongsToFollowingWeek() {
        val mon = WeekHelper.mondayOf(utc(2026, Calendar.JULY, 19))
        assertEquals(Calendar.MONDAY, mon.get(Calendar.DAY_OF_WEEK))
        assertEquals(13, mon.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun mondayOf_monday_isIdentity() {
        val mon = WeekHelper.mondayOf(utc(2026, Calendar.JULY, 13))
        assertEquals(13, mon.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, mon.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun mondayOffset_isZeroBasedFromMonday() {
        assertEquals(2, WeekHelper.mondayOffset(utc(2026, Calendar.JULY, 15)))
        assertEquals(6, WeekHelper.mondayOffset(utc(2026, Calendar.JULY, 19)))
        assertEquals(5, WeekHelper.mondayOffset(utc(2026, Calendar.JULY, 18)))
        assertEquals(0, WeekHelper.mondayOffset(utc(2026, Calendar.JULY, 13)))
    }

    @Test
    fun mondayOf_sameDayAcrossTimezones_isStable() {
        val wed = utc(2026, Calendar.JULY, 15)
        val mon = WeekHelper.mondayOf(wed)
        assertEquals(13, mon.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, mon.get(Calendar.HOUR_OF_DAY))
    }
}
