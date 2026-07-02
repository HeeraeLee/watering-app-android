package com.watering.app.features.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.watering.app.core.service.BillingService
import com.watering.app.core.service.freeTrialDays
import com.watering.app.core.service.trialOfferOrDefault

private data class PremiumFeature(val icon: String, val title: String, val description: String)

// 실제 구현된 프리미엄 혜택만 노출 — 미구현 기능을 광고하면 스토어 정책·환불 리스크가 있음
// (30일/연간 통계, 수분 섭취율 분석, CSV 내보내기, 미세먼지·폭염 알림은 구현 후 추가할 것)
private val PREMIUM_FEATURES = listOf(
    PremiumFeature("🛡️", "연속 기록 보호", "한 달에 하루 놓쳐도 연속 기록이 유지돼요")
)

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class PlanPricing(val freeTrialDays: Int?, val priceLine: String)

// 오퍼 조회를 한 번만 수행해 배지(무료체험 일수)와 가격 문구를 함께 계산한다
private fun ProductDetails.pricing(suffix: String): PlanPricing {
    if (productType != BillingClient.ProductType.SUBS) {
        return PlanPricing(
            freeTrialDays = null,
            priceLine = oneTimePurchaseOfferDetails?.formattedPrice.orEmpty() + suffix
        )
    }
    val offer = trialOfferOrDefault()
    val paidPhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull { it.priceAmountMicros > 0L }
        ?: offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val price = paidPhase?.formattedPrice.orEmpty()
    val trialDays = offer?.freeTrialDays()?.takeIf { it > 0 }
    val priceLine = if (trialDays != null) "${trialDays}일 무료체험 후 $price$suffix" else price + suffix
    return PlanPricing(trialDays, priceLine)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isPurchasing by viewModel.isPurchasing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    var selectedPlanId by remember { mutableStateOf(BillingService.YEARLY_ID) }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("오류") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("확인") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프리미엄") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        if (isPremium) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("이미 프리미엄을 이용 중이에요", style = MaterialTheme.typography.titleMedium)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection()
            FeatureSection()
            PlanSection(
                products = products,
                selectedPlanId = selectedPlanId,
                onSelect = { selectedPlanId = it }
            )
            PurchaseSection(
                isPurchasing = isPurchasing,
                purchaseEnabled = products.isNotEmpty() && activity != null,
                onPurchase = {
                    val product = products.firstOrNull { it.productId == selectedPlanId }
                    if (product != null && activity != null) viewModel.purchase(activity, product)
                },
                onRestore = viewModel::restore
            )
            LegalSection()
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 28.dp)
    ) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("워터링 프리미엄", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "건강한 수분 섭취 습관을\n더 깊이 있게 관리해보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FeatureSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        PREMIUM_FEATURES.forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(feature.icon, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp))
                Column {
                    Text(feature.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanSection(
    products: List<ProductDetails>,
    selectedPlanId: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val monthlyProduct = products.firstOrNull { it.productId == BillingService.MONTHLY_ID }
        val monthlyPricing = monthlyProduct?.pricing(" / 월")
        PlanCard(
            id = BillingService.MONTHLY_ID,
            title = "월간",
            badge = if (monthlyPricing?.freeTrialDays != null) "7일 무료체험" else null,
            priceLine = monthlyPricing?.priceLine,
            isSelected = selectedPlanId == BillingService.MONTHLY_ID,
            onSelect = onSelect
        )
        PlanCard(
            id = BillingService.YEARLY_ID,
            title = "연간",
            badge = "47% 할인",
            priceLine = products.firstOrNull { it.productId == BillingService.YEARLY_ID }?.pricing(" / 년")?.priceLine,
            isSelected = selectedPlanId == BillingService.YEARLY_ID,
            onSelect = onSelect
        )
        PlanCard(
            id = BillingService.LIFETIME_ID,
            title = "평생",
            badge = "최고 혜택",
            priceLine = products.firstOrNull { it.productId == BillingService.LIFETIME_ID }?.pricing(" (일회성)")?.priceLine,
            isSelected = selectedPlanId == BillingService.LIFETIME_ID,
            onSelect = onSelect
        )
    }
}

@Composable
private fun PlanCard(
    id: String,
    title: String,
    badge: String?,
    priceLine: String?,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(id) }
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Text(
                text = priceLine ?: "불러오는 중…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PurchaseSection(
    isPurchasing: Boolean,
    purchaseEnabled: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPurchase,
            enabled = purchaseEnabled && !isPurchasing,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("프리미엄 시작하기")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRestore) {
            Text("구매 복원", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegalSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "결제는 Google Play 계정을 통해 청구돼요.\n구독은 만료 24시간 전 자동 갱신되며,\nGoogle Play 앱 > 구독에서 언제든 해지할 수 있어요.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
