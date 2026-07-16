package com.watering.app.features.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DayStat(
    val dateKey: String,
    val label: String,      // "월", "화" 등 요일 한 글자
    val count: Int,
    val goal: Int,
    val isToday: Boolean
)

data class StatsUiState(
    val weekStats: List<DayStat> = emptyList(),
    val weeklyAvg: Double = 0.0,
    val goalDays: Int = 0,
    val weeklyTotal: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        waterRepository.getHistory(),
        waterRepository.todayRecord,
        waterRepository.streakInfo,
        settingsRepository.userSettings,
        minuteTicker
    ) { history, today, streak, settings, _ ->
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayLabels = context.resources.getStringArray(R.array.weekday_labels_short)
        val todayDate = LocalDate.now(clock)

        val week = (6 downTo 0).map { offset ->
            val date = todayDate.minusDays(offset.toLong())
            val key = date.format(formatter)
            val record: DayRecord? = if (key == today.dateKey) today else history[key]
            DayStat(
                dateKey = key,
                label = dayLabels[date.dayOfWeek.value % 7],
                count = record?.totalCount ?: 0,
                // 오늘은 항상 최신 목표(위젯 경로와 동일), 과거는 기록 당시 저장된 목표를 그대로 사용 —
                // 목표를 바꿔도 이미 지난 날의 달성 여부가 소급 재판정되지 않도록 함
                goal = if (key == today.dateKey) settings.dailyGoal else (record?.goal ?: settings.dailyGoal),
                isToday = offset == 0
            )
        }

        val counts = week.map { it.count }
        StatsUiState(
            weekStats = week,
            weeklyAvg = if (counts.isEmpty()) 0.0 else counts.average(),
            goalDays = week.count { it.count >= it.goal },
            weeklyTotal = counts.sum(),
            currentStreak = streak.currentStreak,
            longestStreak = streak.longestStreak
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
