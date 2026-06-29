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

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    val presetAmounts = listOf(100, 150, 200, 250, 350, 500)

    init {
        viewModelScope.launch {
            val cupSize = settingsRepository.userSettings.first().cupSize
            _uiState.update { it.copy(selectedAmount = cupSize, customAmountText = cupSize.toString()) }
        }
    }

    fun selectDrinkType(type: DrinkType) {
        _uiState.update { it.copy(selectedDrinkType = type) }
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
