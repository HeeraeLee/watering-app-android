package com.watering.app.core.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.watering.app.core.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 구독 상태 검증은 서버 없이 Play Billing Library의 Purchase 조회만으로 수행한다 (iOS StoreKit 2 방식과 동일)
@Singleton
class BillingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : PurchasesUpdatedListener {

    companion object {
        const val MONTHLY_ID = "com.watering.app.premium.monthly"
        const val YEARLY_ID = "com.watering.app.premium.yearly"
        const val LIFETIME_ID = "com.watering.app.premium.lifetime"
        private val SUBSCRIPTION_IDS = listOf(MONTHLY_ID, YEARLY_ID)
        private const val TAG = "BillingService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadProducts()
                        queryPurchases()
                    }
                } else {
                    Log.e(TAG, "빌링 연결 실패: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "빌링 서비스 연결 끊김 — 다음 요청 시 재연결")
            }
        })
    }

    suspend fun loadProducts() {
        if (_products.value.isNotEmpty()) return

        val subProducts = SUBSCRIPTION_IDS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val inAppProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(LIFETIME_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val subResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(subProducts).build()
        )
        val inAppResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(inAppProducts).build()
        )

        _products.value = subResult.productDetailsList.orEmpty() + inAppResult.productDetailsList.orEmpty()
    }

    // 복원 버튼 및 앱 시작 시 호출 — Play Billing은 별도 restore API 없이 재조회로 충분
    suspend fun queryPurchases() {
        val subPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val inAppPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        val allPurchases = subPurchases.purchasesList + inAppPurchases.purchasesList
        val active = allPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        _isPremium.value = active
        settingsRepository.updatePremium(active)

        allPurchases.forEach { acknowledgeIfNeeded(it) }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        val paramsList = if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
                _errorMessage.value = "상품 정보를 불러오지 못했습니다."
                return
            }
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
        }

        _isPurchasing.value = true
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _isPurchasing.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    purchases.orEmpty().forEach { acknowledgeIfNeeded(it) }
                    queryPurchases()
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _errorMessage.value = result.debugMessage
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }
    }
}
