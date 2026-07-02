package com.watering.app.features.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.watering.app.core.service.BillingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingService: BillingService
) : ViewModel() {

    val products: StateFlow<List<ProductDetails>> = billingService.products
    val isPremium: StateFlow<Boolean> = billingService.isPremium
    val isPurchasing: StateFlow<Boolean> = billingService.isPurchasing
    val errorMessage: StateFlow<String?> = billingService.errorMessage

    init {
        viewModelScope.launch { billingService.loadProducts() }
    }

    fun purchase(activity: Activity, productDetails: ProductDetails) {
        billingService.launchPurchase(activity, productDetails)
    }

    fun restore() {
        viewModelScope.launch { billingService.queryPurchases() }
    }

    fun clearError() {
        billingService.clearError()
    }
}
