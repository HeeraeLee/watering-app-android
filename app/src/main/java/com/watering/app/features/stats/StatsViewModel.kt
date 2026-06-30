package com.watering.app.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        waterRepository.getHistory(),
        waterRepository.todayRecord,
        waterRepository.streakInfo,
        settingsRepository.userSettings
    ) { history, today, streak, settings ->
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
        val todayDate = LocalDate.now()

        val week = (6 downTo 0).map { offset ->
            val date = todayDate.minusDays(offset.toLong())
            val key = date.format(formatter)
            val record: DayRecord? = if (key == today.dateKey) today else history[key]
            DayStat(
                dateKey = key,
                label = dayLabels[date.dayOfWeek.value % 7],
                count = record?.totalCount ?: 0,
                goal = settings.dailyGoal,
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
