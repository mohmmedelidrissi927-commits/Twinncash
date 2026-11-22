package com.example

import com.example.domain.PricingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

// [FIX #1.2] اختبارات وحدة شاملة لمحرك التسعير الموحّد PricingEngine
class PricingEngineTest {

    @Test
    fun calculateCommission_percentage_usesGrossMarginAndClampsPercent() {
        // finalPrice=100, supplierCost=70 => grossMargin=30; 20% = 6.0
        assertEquals(6.0, PricingEngine.calculateCommission(100.0, 70.0, "percentage", 20.0), 0.001)
        // 0% => 0.0
        assertEquals(0.0, PricingEngine.calculateCommission(100.0, 70.0, "percentage", 0.0), 0.001)
        // سالب => 0.0
        assertEquals(0.0, PricingEngine.calculateCommission(100.0, 70.0, "percentage", -15.0), 0.001)
        // أكبر من 100% => يُقيد عند 100% من هامش الربح
        assertEquals(30.0, PricingEngine.calculateCommission(100.0, 70.0, "percentage", 150.0), 0.001)
        // هامش ربح سالب (سعر البيع أقل من التكلفة) => 0.0
        assertEquals(0.0, PricingEngine.calculateCommission(60.0, 70.0, "percentage", 20.0), 0.001)
    }

    @Test
    fun calculateCommission_fixed_capsAtGrossMarginAndRejectsNegative() {
        // ضمن هامش الربح (30) => 12.5
        assertEquals(12.5, PricingEngine.calculateCommission(100.0, 70.0, "fixed", 12.5), 0.001)
        // أكبر من هامش الربح (50 > 30) => يُقيد عند 30.0
        assertEquals(30.0, PricingEngine.calculateCommission(100.0, 70.0, "fixed", 50.0), 0.001)
        // صفر أو سالب => 0.0
        assertEquals(0.0, PricingEngine.calculateCommission(100.0, 70.0, "fixed", 0.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateCommission(100.0, 70.0, "fixed", -5.0), 0.001)
        // هامش ربح سالب => 0.0
        assertEquals(0.0, PricingEngine.calculateCommission(50.0, 70.0, "fixed", 10.0), 0.001)
    }

    @Test
    fun calculateNetProfit_computesAndRoundsCorrectly() {
        assertEquals(24.0, PricingEngine.calculateNetProfit(100.0, 70.0, 6.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateNetProfit(100.0, 70.0, 30.0), 0.001)
        // يعكس الخسارة بدقة إذا تجاوزت التكلفة السعر النهائي
        assertEquals(-10.0, PricingEngine.calculateNetProfit(60.0, 70.0, 0.0), 0.001)
        assertEquals(12.35, PricingEngine.calculateNetProfit(50.0, 30.214, 7.44), 0.001)
    }

    @Test
    fun calculateDiscount_handlesPercentageFixedAndBoundaryCases() {
        // نسبة مئوية
        assertEquals(15.0, PricingEngine.calculateDiscount("percentage", 15.0, 100.0), 0.001)
        assertEquals(100.0, PricingEngine.calculateDiscount("percentage", 120.0, 100.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateDiscount("percentage", -10.0, 100.0), 0.001)

        // مبلغ ثابت
        assertEquals(25.0, PricingEngine.calculateDiscount("fixed", 25.0, 100.0), 0.001)
        assertEquals(40.0, PricingEngine.calculateDiscount("fixed", 75.0, 40.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateDiscount("fixed", -5.0, 40.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateDiscount("fixed", 10.0, 0.0), 0.001)
    }

    @Test
    fun calculateLoyaltyDiscount_appliesOnlyWhenThresholdReached() {
        assertEquals(0.0, PricingEngine.calculateLoyaltyDiscount(100.0, 4, 25.0, 3), 0.001)
        assertEquals(25.0, PricingEngine.calculateLoyaltyDiscount(100.0, 4, 25.0, 4), 0.001)
        assertEquals(25.0, PricingEngine.calculateLoyaltyDiscount(100.0, 4, 25.0, 6), 0.001)
        assertEquals(0.0, PricingEngine.calculateLoyaltyDiscount(100.0, 0, 25.0, 4), 0.001)
        assertEquals(100.0, PricingEngine.calculateLoyaltyDiscount(100.0, 2, 150.0, 2), 0.001)
        assertEquals(0.0, PricingEngine.calculateLoyaltyDiscount(-20.0, 2, 25.0, 2), 0.001)
    }

    @Test
    fun calculateFinalPrice_neverDropsBelowZeroAndIgnoresNegativeDiscounts() {
        assertEquals(65.0, PricingEngine.calculateFinalPrice(100.0, 25.0, 10.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateFinalPrice(50.0, 30.0, 40.0), 0.001)
        assertEquals(100.0, PricingEngine.calculateFinalPrice(100.0, -10.0, -5.0), 0.001)
        assertEquals(0.0, PricingEngine.calculateFinalPrice(-10.0, 5.0, 5.0), 0.001)
    }
}

// [REVIEWED 2026-11] إصلاحات شاملة
