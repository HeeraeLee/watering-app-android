package com.watering.app.features.premium

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.watering.app.core.service.BillingService
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class PremiumViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(billingService: BillingService): PremiumViewModel =
        PremiumViewModel(billingService)

    private fun fakeBillingService(): BillingService = mockk(relaxed = true) {
        every { products } returns MutableStateFlow(emptyList())
        every { isPremium } returns MutableStateFlow(false)
        every { isPurchasing } returns MutableStateFlow(false)
        every { errorMessage } returns MutableStateFlow(null)
    }

    @Test
    fun init시_상품목록을로드한다() = runTest(mainDispatcherRule.testDispatcher) {
        val billingService = fakeBillingService()
        coEvery { billingService.loadProducts() } returns Unit

        createViewModel(billingService)

        coVerify { billingService.loadProducts() }
    }

    @Test
    fun products_isPremium_등의상태를billingService에서그대로노출한다() = runTest(mainDispatcherRule.testDispatcher) {
        val billingService = fakeBillingService()
        val viewModel = createViewModel(billingService)

        assertSame(billingService.products, viewModel.products)
        assertSame(billingService.isPremium, viewModel.isPremium)
        assertSame(billingService.isPurchasing, viewModel.isPurchasing)
        assertSame(billingService.errorMessage, viewModel.errorMessage)
    }

    @Test
    fun purchase_billingService에구매를위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val billingService = fakeBillingService()
        val viewModel = createViewModel(billingService)
        val activity = mockk<Activity>()
        val product = mockk<ProductDetails>()

        viewModel.purchase(activity, product)

        verify { billingService.launchPurchase(activity, product) }
    }

    @Test
    fun restore_billingService에구매내역재조회를위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val billingService = fakeBillingService()
        val viewModel = createViewModel(billingService)
        coEvery { billingService.queryPurchases() } returns Unit

        viewModel.restore()

        coVerify { billingService.queryPurchases() }
    }

    @Test
    fun clearError_billingService에위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val billingService = fakeBillingService()
        val viewModel = createViewModel(billingService)

        viewModel.clearError()

        verify { billingService.clearError() }
    }
}
