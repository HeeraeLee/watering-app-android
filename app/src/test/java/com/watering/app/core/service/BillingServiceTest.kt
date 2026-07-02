package com.watering.app.core.service

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingServiceTest {

    @Test
    fun parseIsoPeriodDays_일단위기간() {
        assertEquals(7, parseIsoPeriodDays("P7D"))
    }

    @Test
    fun parseIsoPeriodDays_주단위기간은7일로환산() {
        assertEquals(7, parseIsoPeriodDays("P1W"))
        assertEquals(14, parseIsoPeriodDays("P2W"))
    }

    @Test
    fun parseIsoPeriodDays_월단위기간은30일로환산() {
        assertEquals(30, parseIsoPeriodDays("P1M"))
    }

    @Test
    fun parseIsoPeriodDays_년단위기간은365일로환산() {
        assertEquals(365, parseIsoPeriodDays("P1Y"))
    }

    @Test
    fun parseIsoPeriodDays_복합기간은합산된다() {
        assertEquals(365 + 60 + 7 + 3, parseIsoPeriodDays("P1Y2M1W3D"))
    }

    @Test
    fun parseIsoPeriodDays_형식이맞지않으면0반환() {
        assertEquals(0, parseIsoPeriodDays("invalid"))
    }

    private fun pricingPhase(priceAmountMicros: Long, billingPeriod: String) =
        mockk<ProductDetails.PricingPhase> {
            every { this@mockk.priceAmountMicros } returns priceAmountMicros
            every { this@mockk.billingPeriod } returns billingPeriod
        }

    private fun offer(
        offerToken: String,
        phases: List<ProductDetails.PricingPhase>
    ) = mockk<ProductDetails.SubscriptionOfferDetails> {
        every { this@mockk.offerToken } returns offerToken
        every { pricingPhases } returns mockk {
            every { pricingPhaseList } returns phases
        }
    }

    @Test
    fun trialOfferOrDefault_무료체험오퍼가있으면그오퍼를우선반환() {
        val paidOffer = offer("paid", listOf(pricingPhase(5_000_000L, "P1M")))
        val trialOffer = offer("trial", listOf(pricingPhase(0L, "P7D"), pricingPhase(5_000_000L, "P1M")))
        val product = mockk<ProductDetails> {
            every { subscriptionOfferDetails } returns listOf(paidOffer, trialOffer)
        }

        val result = product.trialOfferOrDefault()

        assertEquals("trial", result?.offerToken)
    }

    @Test
    fun trialOfferOrDefault_무료체험없으면첫오퍼를반환() {
        val firstOffer = offer("first", listOf(pricingPhase(5_000_000L, "P1M")))
        val secondOffer = offer("second", listOf(pricingPhase(6_000_000L, "P1Y")))
        val product = mockk<ProductDetails> {
            every { subscriptionOfferDetails } returns listOf(firstOffer, secondOffer)
        }

        val result = product.trialOfferOrDefault()

        assertEquals("first", result?.offerToken)
    }

    @Test
    fun trialOfferOrDefault_오퍼자체가없으면null() {
        val product = mockk<ProductDetails> {
            every { subscriptionOfferDetails } returns null
        }

        assertNull(product.trialOfferOrDefault())
    }

    @Test
    fun freeTrialDays_무료단계가있으면일수반환() {
        val trialOffer = offer("trial", listOf(pricingPhase(0L, "P7D"), pricingPhase(5_000_000L, "P1M")))

        assertEquals(7, trialOffer.freeTrialDays())
    }

    @Test
    fun freeTrialDays_무료단계없으면null() {
        val paidOffer = offer("paid", listOf(pricingPhase(5_000_000L, "P1M")))

        assertNull(paidOffer.freeTrialDays())
    }
}
