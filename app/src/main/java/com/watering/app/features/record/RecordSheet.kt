package com.watering.app.features.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.model.DrinkType
import com.watering.app.ui.theme.AppCardBackgroundColor
import com.watering.app.ui.theme.AquaCtaContentColor
import com.watering.app.ui.theme.DrinkBadge

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
        sheetState = sheetState,
        // 다크모드 기본 컨테이너 색이 순검정에 가까워 홈의 딥 틸 배경과 이질감이 있던 문제 수정
        // (Fable UI 리뷰 화면별 findings — 기록 시트). 앱 공용 카드 색으로 통일
        containerColor = AppCardBackgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 직접 입력 필드 포커스로 키패드가 열리면 시트 윈도우 높이가 키패드만큼 줄어드는데,
                // 내용이 그 높이보다 길면 스크롤 불가능한 Column은 마지막 자식(입력 필드/버튼)의
                // 높이 제약을 최소 높이 아래로 짓눌러 입력한 숫자가 클리핑돼 안 보이던 버그 수정 —
                // 스크롤 가능하게 해 자식들이 고유 높이를 유지하도록 함 (SM-A256N 등 고밀도 기기 재현)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = stringResource(R.string.record_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.record_drink_type_label), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                DrinkTypeSelector(
                    selected = uiState.selectedDrinkType,
                    onSelect = viewModel::selectDrinkType
                )
            }
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.record_amount_label), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountSelector(
                    presets = presetAmountsFor(uiState.selectedDrinkType),
                    selectedAmount = uiState.selectedAmount,
                    isCustom = uiState.isCustomAmount,
                    customText = uiState.customAmountText,
                    onSelectPreset = viewModel::selectPresetAmount,
                    onToggleCustom = viewModel::toggleCustomMode,
                    onCustomTextChange = viewModel::enterCustomAmount
                )
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    onRecord(uiState.selectedAmount, uiState.selectedDrinkType)
                    onDismiss()
                },
                enabled = uiState.selectedAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                // 파스텔 아쿠아 배경 위 흰 글자 저대비(약 1.8:1) 수정 — AquaCtaContentColor 주석 참고
                colors = ButtonDefaults.buttonColors(contentColor = AquaCtaContentColor)
            ) {
                Text(
                    text = stringResource(
                        R.string.record_button,
                        uiState.selectedDrinkType.emoji,
                        stringResource(uiState.selectedDrinkType.displayNameRes),
                        uiState.selectedAmount
                    ),
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
                label = { Text(stringResource(type.displayNameRes)) },
                leadingIcon = { DrinkBadge(type, size = 22.dp) }
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
            label = { Text(stringResource(R.string.record_custom_input)) }
        )
    }

    if (isCustom) {
        OutlinedTextField(
            value = customText,
            onValueChange = onCustomTextChange,
            label = { Text(stringResource(R.string.record_custom_input)) },
            suffix = { Text("ml") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        // 칩 자체는 ml 숫자만 유지하고, 선택된 사이즈가 어떤 컵/텀블러에 해당하는지만
        // 한 줄 설명으로 보여줌(웹 목업 C안 채택, 2026-07-12)
        amountLabelRes(selectedAmount)?.let { labelRes ->
            Text(
                text = stringResource(R.string.record_amount_size_hint, stringResource(labelRes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
