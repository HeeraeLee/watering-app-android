package com.watering.app.core.service

import com.watering.app.core.model.DayRecord
import java.time.Instant
import java.time.ZoneId

// 데이터 부족(기록일 미달)과 패턴 없음(데이터는 충분하지만 고르게 마심)은 사용자에게 다른 의미라
// 별개 결과로 구분한다 — 후자를 "데이터가 더 필요해요"로 뭉뚱그리면 이미 규칙적인 사용자에게
// 오해를 준다("더 기록하면 뭔가 나오나?" 같은).
sealed interface TimeOfDayInsightResult {
    data class Found(val startHour: Int, val endHour: Int) : TimeOfDayInsightResult
    data object InsufficientData : TimeOfDayInsightResult
    data object NoClearPattern : TimeOfDayInsightResult
}

// 순수 계산 로직 — DI 의존성이 없어 Hilt 주입 없이 object로 둔다.
object StatsInsightService {
    private const val BUCKET_HOURS = 2
    private const val MIN_DAYS_WITH_DATA = 7
    private const val MIN_ACTIVE_BUCKETS = 3
    private const val LOW_BUCKET_THRESHOLD_RATIO = 0.6

    // days는 최근 30일 이력(빈 날짜는 포함하지 않아도 됨), activeStartHour~activeEndHour는
    // 사용자의 알림 활성 시간대(UserSettings.notificationStart/End). 동일 기간을 버킷별로 비교하는
    // 것이라 일수로 나눈 평균이 아니라 버킷별 합계로 충분하다.
    fun calculateTimeOfDayInsight(
        days: List<DayRecord>,
        activeStartHour: Int,
        activeEndHour: Int
    ): TimeOfDayInsightResult {
        val daysWithData = days.count { it.entries.isNotEmpty() }
        if (daysWithData < MIN_DAYS_WITH_DATA) return TimeOfDayInsightResult.InsufficientData

        val bucketStarts = (activeStartHour until activeEndHour step BUCKET_HOURS).toList()
        if (bucketStarts.size < MIN_ACTIVE_BUCKETS) return TimeOfDayInsightResult.InsufficientData

        val allEntries = days.flatMap { it.entries }
        val bucketCounts = bucketStarts.associateWith { bucketStart ->
            allEntries.count { entry ->
                val hour = hourOf(entry.timestampMillis)
                hour in bucketStart until (bucketStart + BUCKET_HOURS)
            }
        }

        val meanCount = bucketCounts.values.average()
        if (meanCount <= 0.0) return TimeOfDayInsightResult.NoClearPattern

        val (minBucketStart, minCount) = bucketCounts.entries.minBy { it.value }.toPair()
        if (minCount >= meanCount * LOW_BUCKET_THRESHOLD_RATIO) return TimeOfDayInsightResult.NoClearPattern

        return TimeOfDayInsightResult.Found(startHour = minBucketStart, endHour = minBucketStart + BUCKET_HOURS)
    }

    private fun hourOf(timestampMillis: Long): Int =
        Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).hour
}
