package com.watering.app.features.record

import app.cash.turbine.test
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.UserSettings
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(cupSize: Int = 200): RecordViewModel {
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns flowOf(UserSettings(cupSize = cupSize))
        }
        return RecordViewModel(settingsRepository)
    }

    @Test
    fun 초기상태는_기본값200ml물이다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(DrinkType.WATER, state.selectedDrinkType)
        assertEquals(200, state.selectedAmount)
    }

    @Test
    fun init에서_설정의컵크기를불러와초기선택량으로반영한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(cupSize = 350)

        val state = viewModel.uiState.value
        assertEquals(350, state.selectedAmount)
        assertEquals("350", state.customAmountText)
    }

    @Test
    fun selectDrinkType_음료종류를변경한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.selectDrinkType(DrinkType.COFFEE)

        assertEquals(DrinkType.COFFEE, viewModel.uiState.value.selectedDrinkType)
    }

    @Test
    fun selectPresetAmount_선택시커스텀모드가꺼진다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.toggleCustomMode()

        viewModel.selectPresetAmount(500)

        val state = viewModel.uiState.value
        assertEquals(500, state.selectedAmount)
        assertEquals("500", state.customAmountText)
        assertEquals(false, state.isCustomAmount)
    }

    @Test
    fun enterCustomAmount_숫자가아닌문자는제거된다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.enterCustomAmount("1a2b3c")

        assertEquals("123", viewModel.uiState.value.customAmountText)
        assertEquals(123, viewModel.uiState.value.selectedAmount)
    }

    @Test
    fun enterCustomAmount_4자리를초과하면잘린다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.enterCustomAmount("123456")

        assertEquals("1234", viewModel.uiState.value.customAmountText)
        assertEquals(1234, viewModel.uiState.value.selectedAmount)
    }

    @Test
    fun enterCustomAmount_빈문자열이면selectedAmount는0이된다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.enterCustomAmount("")

        assertEquals("", viewModel.uiState.value.customAmountText)
        assertEquals(0, viewModel.uiState.value.selectedAmount)
    }

    @Test
    fun toggleCustomMode_호출할때마다상태가반전된다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val initial = viewModel.uiState.value.isCustomAmount

        viewModel.toggleCustomMode()
        assertEquals(!initial, viewModel.uiState.value.isCustomAmount)

        viewModel.toggleCustomMode()
        assertEquals(initial, viewModel.uiState.value.isCustomAmount)
    }

    @Test
    fun presetAmounts_고정된6개옵션을제공한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        assertTrue(viewModel.presetAmounts.containsAll(listOf(100, 150, 200, 250, 350, 500)))
    }

    @Test
    fun uiState_turbine으로수집해도동일한값을받는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(cupSize = 250)

        viewModel.uiState.test {
            val item = awaitItem()
            assertEquals(250, item.selectedAmount)
        }
    }
}
