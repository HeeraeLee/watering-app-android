package com.watering.app.core.service

import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.WaterEntry
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsInsightServiceTest {

    private fun entryAt(day: Int, hour: Int) = WaterEntry(
        timestampMillis = LocalDateTime.of(2026, 1, day, hour, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        amount = 200,
        drinkType = DrinkType.WATER
    )

    // 8~22시를 2시간 버킷 7개로 나눴을 때, 14~16시(오후 2~4시) 버킷만 엔트리가 없고
    // 나머지 6개 버킷엔 매일 1개씩 기록 — 10일치라 데이터 충분, 패턴도 뚜렷해야 한다
    // (16시를 명시적으로 포함시켜 14~16 버킷만 유일한 공백이 되도록 함 — 그렇지 않으면 16~18도
    // 우연히 0이 돼 동점 처리 로직에 의존하게 됨)
    private fun daysWithGapAt14to16(dayCount: Int = 10): List<DayRecord> =
        (1..dayCount).map { day ->
            val entries = listOf(8, 10, 12, 16, 18, 20).map { hour -> entryAt(day, hour) }
            DayRecord(dateKey = "2026-01-%02d".format(day), entries = entries, goal = 8)
        }

    @Test
    fun calculateTimeOfDayInsight_뚜렷한공백버킷이있으면해당구간을반환한다() {
        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = daysWithGapAt14to16(),
            activeStartHour = 8,
            activeEndHour = 22
        )

        assertEquals(TimeOfDayInsightResult.Found(startHour = 14, endHour = 16), result)
    }

    @Test
    fun calculateTimeOfDayInsight_기록된날이7일미만이면데이터부족() {
        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = daysWithGapAt14to16(dayCount = 6),
            activeStartHour = 8,
            activeEndHour = 22
        )

        assertEquals(TimeOfDayInsightResult.InsufficientData, result)
    }

    @Test
    fun calculateTimeOfDayInsight_정확히7일이면계산한다() {
        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = daysWithGapAt14to16(dayCount = 7),
            activeStartHour = 8,
            activeEndHour = 22
        )

        assertEquals(TimeOfDayInsightResult.Found(startHour = 14, endHour = 16), result)
    }

    @Test
    fun calculateTimeOfDayInsight_활성시간대가4시간뿐이면버킷이2개라데이터부족() {
        val days = (1..10).map { day ->
            DayRecord(dateKey = "2026-01-%02d".format(day), entries = listOf(entryAt(day, 9)), goal = 8)
        }

        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = days,
            activeStartHour = 8,
            activeEndHour = 12
        )

        assertEquals(TimeOfDayInsightResult.InsufficientData, result)
    }

    @Test
    fun calculateTimeOfDayInsight_활성시간대가정확히6시간이면버킷3개로계산한다() {
        // 8-10, 10-12 버킷에만 매일 기록, 12-14는 공백 — 3버킷 경계에서도 패턴 인식돼야 함
        val days = (1..10).map { day ->
            val entries = listOf(8, 10).map { hour -> entryAt(day, hour) }
            DayRecord(dateKey = "2026-01-%02d".format(day), entries = entries, goal = 8)
        }

        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = days,
            activeStartHour = 8,
            activeEndHour = 14
        )

        assertEquals(TimeOfDayInsightResult.Found(startHour = 12, endHour = 14), result)
    }

    @Test
    fun calculateTimeOfDayInsight_모든버킷이균일하면패턴없음() {
        val days = (1..10).map { day ->
            val entries = listOf(8, 10, 12, 14, 16, 18, 20).map { hour -> entryAt(day, hour) }
            DayRecord(dateKey = "2026-01-%02d".format(day), entries = entries, goal = 8)
        }

        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = days,
            activeStartHour = 8,
            activeEndHour = 22
        )

        assertEquals(TimeOfDayInsightResult.NoClearPattern, result)
    }

    @Test
    fun calculateTimeOfDayInsight_엔트리가전혀없으면데이터부족() {
        val days = (1..10).map { day -> DayRecord(dateKey = "2026-01-%02d".format(day), goal = 8) }

        val result = StatsInsightService.calculateTimeOfDayInsight(
            days = days,
            activeStartHour = 8,
            activeEndHour = 22
        )

        assertEquals(TimeOfDayInsightResult.InsufficientData, result)
    }

    private fun entryOf(amount: Int, drinkType: DrinkType) = WaterEntry(
        timestampMillis = 0L,
        amount = amount,
        drinkType = drinkType
    )

    @Test
    fun calculateHydrationVolumeMl_물만있으면환산없이그대로합산한다() {
        val entries = listOf(entryOf(200, DrinkType.WATER), entryOf(200, DrinkType.WATER))

        val result = StatsInsightService.calculateHydrationVolumeMl(entries)

        assertEquals(400, result)
    }

    @Test
    fun calculateHydrationVolumeMl_음료별환산율을적용해합산한다() {
        // 물 200ml(1.0) + 커피 200ml(0.7) = 200 + 140 = 340
        val entries = listOf(entryOf(200, DrinkType.WATER), entryOf(200, DrinkType.COFFEE))

        val result = StatsInsightService.calculateHydrationVolumeMl(entries)

        assertEquals(340, result)
    }

    @Test
    fun calculateHydrationVolumeMl_소수점은반올림한다() {
        // 333ml * 0.85(JUICE) = 283.05 -> 283
        val entries = listOf(entryOf(333, DrinkType.JUICE))

        val result = StatsInsightService.calculateHydrationVolumeMl(entries)

        assertEquals(283, result)
    }

    @Test
    fun calculateHydrationVolumeMl_엔트리가없으면0이다() {
        val result = StatsInsightService.calculateHydrationVolumeMl(emptyList())

        assertEquals(0, result)
    }

    @Test
    fun recommendedGoalCups_체중과컵크기로목표를계산한다() {
        // 60kg * 33ml = 1980ml, / 200ml = 9.9 -> 반올림 10잔
        val result = StatsInsightService.recommendedGoalCups(weightKg = 60.0, cupSizeMl = 200)

        assertEquals(10, result)
    }

    @Test
    fun recommendedGoalCups_컵크기가다르면결과도달라진다() {
        // 60kg * 33ml = 1980ml, / 300ml = 6.6 -> 반올림 7잔
        val result = StatsInsightService.recommendedGoalCups(weightKg = 60.0, cupSizeMl = 300)

        assertEquals(7, result)
    }

    @Test
    fun recommendedGoalCups_최소1잔으로클램프된다() {
        // 1kg * 33ml = 33ml, / 200ml = 0.165 -> 반올림 0 -> 최소 1로 클램프
        val result = StatsInsightService.recommendedGoalCups(weightKg = 1.0, cupSizeMl = 200)

        assertEquals(1, result)
    }

    @Test
    fun recommendedGoalCups_최대20잔으로클램프된다() {
        // 200kg * 33ml = 6600ml, / 150ml = 44 -> 최대 20으로 클램프
        val result = StatsInsightService.recommendedGoalCups(weightKg = 200.0, cupSizeMl = 150)

        assertEquals(20, result)
    }
}
