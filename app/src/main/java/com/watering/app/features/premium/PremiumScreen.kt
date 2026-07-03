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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.watering.app.R
import com.watering.app.core.service.BillingService
import com.watering.app.core.service.freeTrialDays
import com.watering.app.core.service.trialOfferOrDefault

private data class PremiumFeature(val icon: String, @StringRes val titleRes: Int, @StringRes val descriptionRes: Int)

// 실제 구현된 프리미엄 혜택만 노출 — 미구현 기능을 광고하면 스토어 정책·환불 리스크가 있음
// (수분 섭취율 분석, CSV 내보내기, 미세먼지·폭염 알림은 구현 후 추가할 것)
private val PREMIUM_FEATURES = listOf(
    PremiumFeature("🛡️", R.string.feature_streak_protection_title, R.string.feature_streak_protection_description),
    PremiumFeature("🎨", R.string.settings_section_widget_theme, R.string.feature_widget_theme_description),
    PremiumFeature("📊", R.string.smart_stats_title, R.string.feature_smart_stats_description)
)

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class PlanPricing(val freeTrialDays: Int?, val price: String, val suffix: String)

// 오퍼 조회를 한 번만 수행해 배지(무료체험 일수)와 가격 문구를 함께 계산한다. 텍스트 조립은
// stringResource가 필요해 Composable 호출부(priceLineText)에서 수행한다.
private fun ProductDetails.pricing(suffix: String): PlanPricing {
    if (productType != BillingClient.ProductType.SUBS) {
        return PlanPricing(
            freeTrialDays = null,
            price = oneTimePurchaseOfferDetails?.formattedPrice.orEmpty(),
            suffix = suffix
        )
    }
    val offer = trialOfferOrDefault()
    val paidPhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull { it.priceAmountMicros > 0L }
        ?: offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val price = paidPhase?.formattedPrice.orEmpty()
    val trialDays = offer?.freeTrialDays()?.takeIf { it > 0 }
    return PlanPricing(trialDays, price, suffix)
}

@Composable
private fun PlanPricing.priceLineText(): String =
    if (freeTrialDays != null) {
        stringResource(R.string.premium_trial_price_line, freeTrialDays, price + suffix)
    } else {
        price + suffix
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
            title = { Text(stringResource(R.string.premium_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text(stringResource(R.string.premium_error_confirm)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium_top_bar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
                    Text(stringResource(R.string.premium_already_active), style = MaterialTheme.typography.titleMedium)
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
        Text(stringResource(R.string.premium_header_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.premium_header_subtitle),
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
                    Text(stringResource(feature.titleRes), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(feature.descriptionRes),
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
        val monthlyPricing = monthlyProduct?.pricing(stringResource(R.string.premium_suffix_monthly))
        PlanCard(
            id = BillingService.MONTHLY_ID,
            title = stringResource(R.string.premium_plan_monthly),
            badge = if (monthlyPricing?.freeTrialDays != null) stringResource(R.string.premium_badge_free_trial) else null,
            priceLine = monthlyPricing?.priceLineText(),
            isSelected = selectedPlanId == BillingService.MONTHLY_ID,
            onSelect = onSelect
        )
        PlanCard(
            id = BillingService.YEARLY_ID,
            title = stringResource(R.string.premium_plan_yearly),
            badge = stringResource(R.string.premium_badge_yearly_discount),
            priceLine = products.firstOrNull { it.productId == BillingService.YEARLY_ID }
                ?.pricing(stringResource(R.string.premium_suffix_yearly))?.priceLineText(),
            isSelected = selectedPlanId == BillingService.YEARLY_ID,
            onSelect = onSelect
        )
        PlanCard(
            id = BillingService.LIFETIME_ID,
            title = stringResource(R.string.premium_plan_lifetime),
            badge = stringResource(R.string.premium_badge_lifetime_best),
            priceLine = products.firstOrNull { it.productId == BillingService.LIFETIME_ID }
                ?.pricing(stringResource(R.string.premium_suffix_lifetime))?.priceLineText(),
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
                text = priceLine ?: stringResource(R.string.premium_loading),
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
                Text(stringResource(R.string.premium_start_button))
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.premium_restore_button), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            stringResource(R.string.premium_legal_text),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
