package com.watering.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayRecordTest {

    private fun entry(amount: Int) =
        WaterEntry(timestampMillis = 0L, amount = amount, drinkType = DrinkType.WATER)

    @Test
    fun totalCount_기록량이컵크기보다작으면_비례한크레딧만인정된다() {
        val record = DayRecord(
            dateKey = "2026-07-17",
            entries = listOf(entry(200)),
            goal = 2,
            cupSize = 887
        )

        assertEquals(200.0 / 887.0, record.totalCount, 0.0001)
    }

    @Test
    fun totalCount_여러기록의ml합을컵크기로나눈값이다() {
        val record = DayRecord(
            dateKey = "2026-07-17",
            entries = listOf(entry(200), entry(1800)),
            goal = 2,
            cupSize = 887
        )

        assertEquals(2000.0 / 887.0, record.totalCount, 0.0001)
    }

    @Test
    fun isAchieved_ml비례합이목표를넘으면true다() {
        val record = DayRecord(
            dateKey = "2026-07-17",
            entries = listOf(entry(887), entry(887)),
            goal = 2,
            cupSize = 887
        )

        assertTrue(record.isAchieved)
    }

    @Test
    fun achievementRate_목표가0이면0이다() {
        val record = DayRecord(dateKey = "2026-07-17", entries = listOf(entry(200)), goal = 0, cupSize = 887)

        assertEquals(0.0, record.achievementRate, 0.0001)
    }

    @Test
    fun totalCount_컵크기가0이하면0으로방어한다() {
        val record = DayRecord(dateKey = "2026-07-17", entries = listOf(entry(200)), goal = 2, cupSize = 0)

        assertEquals(0.0, record.totalCount, 0.0001)
    }

    // 오너 제보 재현: 887ml x 2잔 목표에서 200ml 커스텀 기록 1건은 50%(1잔)가 아니라
    // 약 11%(200/1774)여야 한다 — "다른 음료 선택"으로 소량을 기록해 목표를 손쉽게
    // "달성"할 수 있던 악용 경로를 막기 위한 회귀 테스트(2026-07-17)
    @Test
    fun achievementRate_소량기록으로목표를악용달성할수없다() {
        val record = DayRecord(
            dateKey = "2026-07-17",
            entries = listOf(entry(200)),
            goal = 2,
            cupSize = 887
        )

        assertEquals(0.1127, record.achievementRate, 0.001)
        assertFalse(record.isAchieved)
    }
}
