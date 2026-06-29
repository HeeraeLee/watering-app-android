package com.watering.app.features.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.core.model.DrinkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(
    onDismiss: () -> Unit,
    onRecord: (amount: Int, drinkType: DrinkType) -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "음료 기록",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("음료 종류", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                DrinkTypeSelector(
                    selected = uiState.selectedDrinkType,
                    onSelect = viewModel::selectDrinkType
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("양", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountSelector(
                    presets = viewModel.presetAmounts,
                    selectedAmount = uiState.selectedAmount,
                    isCustom = uiState.isCustomAmount,
                    customText = uiState.customAmountText,
                    onSelectPreset = viewModel::selectPresetAmount,
                    onToggleCustom = viewModel::toggleCustomMode,
                    onCustomTextChange = viewModel::enterCustomAmount
                )
            }

            Button(
                onClick = {
                    onRecord(uiState.selectedAmount, uiState.selectedDrinkType)
                    onDismiss()
                },
                enabled = uiState.selectedAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "${uiState.selectedDrinkType.emoji} ${uiState.selectedDrinkType.displayName} ${uiState.selectedAmount}ml 기록하기",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrinkTypeSelector(
    selected: DrinkType,
    onSelect: (DrinkType) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DrinkType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text("${type.emoji} ${type.displayName}") }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountSelector(
    presets: List<Int>,
    selectedAmount: Int,
    isCustom: Boolean,
    customText: String,
    onSelectPreset: (Int) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomTextChange: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { amount ->
            FilterChip(
                selected = !isCustom && selectedAmount == amount,
                onClick = { onSelectPreset(amount) },
                label = { Text("${amount}ml") }
            )
        }
        FilterChip(
            selected = isCustom,
            onClick = onToggleCustom,
            label = { Text("직접 입력") }
        )
    }

    if (isCustom) {
        OutlinedTextField(
            value = customText,
            onValueChange = onCustomTextChange,
            label = { Text("직접 입력") },
            suffix = { Text("ml") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
