package com.watering.app.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.service.StatsInsightService
import com.watering.app.core.service.TimeOfDayInsightResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SmartStatsUiState(
    val monthStats: List<DayStat> = emptyList(),
    val insight: TimeOfDayInsightResult = TimeOfDayInsightResult.InsufficientData,
    // 최근 365일, 기록 없는 날은 null (히트맵에서 빈 칸으로 표시)
    val annualDays: List<DailyAchievement?> = emptyList(),
    // 음료별 수분 환산율(DrinkType.hydrationRate) 적용한 정보성 지표 — 잔 수 기반 목표/달성과는 무관
    val todayHydrationVolumeMl: Int = 0
)

@HiltViewModel
class SmartStatsViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<SmartStatsUiState> = combine(
        waterRepository.getHistory(),
        waterRepository.getAnnualHistory(),
        waterRepository.todayRecord,
        settingsRepository.userSettings,
        minuteTicker
    ) { history, annualHistory, today, settings, _ ->
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val todayDate = LocalDate.now(clock)

        val monthRecords = (29 downTo 0).map { offset ->
            val key = todayDate.minusDays(offset.toLong()).format(formatter)
            val record: DayRecord? = if (key == today.dateKey) today else history[key]
            record ?: DayRecord(dateKey = key, goal = settings.dailyGoal)
        }
        val monthStats = monthRecords.mapIndexed { index, record ->
            DayStat(
                dateKey = record.dateKey,
                label = "",
                count = record.totalCount,
                // 오늘은 항상 최신 목표(위젯 경로와 동일), 과거는 기록 당시 저장된 목표를 그대로 사용 —
                // 목표를 바꿔도 이미 지난 날의 달성 여부가 소급 재판정되지 않도록 함
                goal = if (record.dateKey == today.dateKey) settings.dailyGoal else record.goal,
                isToday = index == monthRecords.lastIndex
            )
        }

        val insight = StatsInsightService.calculateTimeOfDayInsight(
            days = monthRecords,
            activeStartHour = settings.notificationStart,
            activeEndHour = settings.notificationEnd
        )

        // 오늘 것은 archiveToAnnualHistory가 다음 기록 시점에나 반영되므로, 실시간 todayRecord로 대체
        val todayAchievement = DailyAchievement(
            dateKey = today.dateKey,
            totalCount = today.totalCount,
            goal = settings.dailyGoal
        )
        val annualDays = (364 downTo 0).map { offset ->
            val key = todayDate.minusDays(offset.toLong()).format(formatter)
            if (key == today.dateKey) todayAchievement else annualHistory[key]
        }

        val todayHydrationVolumeMl = StatsInsightService.calculateHydrationVolumeMl(today.entries)

        SmartStatsUiState(
            monthStats = monthStats,
            insight = insight,
            annualDays = annualDays,
            todayHydrationVolumeMl = todayHydrationVolumeMl
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartStatsUiState())
}
