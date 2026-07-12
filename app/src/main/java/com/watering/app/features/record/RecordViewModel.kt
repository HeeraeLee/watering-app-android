package com.watering.app.features.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.DrinkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val selectedDrinkType: DrinkType = DrinkType.WATER,
    val selectedAmount: Int = 200,
    val isCustomAmount: Boolean = false,
    val customAmountText: String = "200"
)

// 음료 종류에 따라 관련 있는 규격만 보여줌(웹 목업 "4안" 채택, 2026-07-12) — 설정 화면
// 컵 크기(CupSizeSetting)에 쓴 것과 동일한 실제 브랜드 규격 값을 재사용
fun presetAmountsFor(drinkType: DrinkType): List<Int> = when (drinkType) {
    DrinkType.WATER -> listOf(190, 200, 500, 887)          // 종이컵/물컵/생수/스탠리 퀜처
    DrinkType.COFFEE, DrinkType.TEA -> listOf(355, 473, 591, 710) // 스벅 톨/그란데/벤티/메가커피
    DrinkType.JUICE, DrinkType.MILK -> listOf(200, 355, 473, 500)
    DrinkType.OTHER -> listOf(200, 355, 473, 500, 710)
}

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cupSize = settingsRepository.userSettings.first().cupSize
            _uiState.update { it.copy(selectedAmount = cupSize, customAmountText = cupSize.toString()) }
        }
    }

    // 음료 종류를 바꾸면 그 종류에 맞는 첫 번째 프리셋으로 양도 같이 갱신 — 안 맞는 이전 종류의
    // 프리셋 값이 그대로 남아있지 않도록
    fun selectDrinkType(type: DrinkType) {
        val defaultAmount = presetAmountsFor(type).first()
        _uiState.update {
            it.copy(
                selectedDrinkType = type,
                selectedAmount = defaultAmount,
                isCustomAmount = false,
                customAmountText = defaultAmount.toString()
            )
        }
    }

    fun selectPresetAmount(amount: Int) {
        _uiState.update {
            it.copy(selectedAmount = amount, isCustomAmount = false, customAmountText = amount.toString())
        }
    }

    fun enterCustomAmount(text: String) {
        val digits = text.filter { it.isDigit() }.take(4)
        _uiState.update {
            it.copy(customAmountText = digits, selectedAmount = digits.toIntOrNull() ?: 0)
        }
    }

    fun toggleCustomMode() {
        _uiState.update { it.copy(isCustomAmount = !it.isCustomAmount) }
    }
}
