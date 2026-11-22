package com.example.domain

import kotlin.math.round

// [FIX #1.2] محرك تسعير وعمولات موحّد نقي بدون أي تبعيات Android أو Firebase
object PricingEngine {

    fun round2(value: Double): Double = round(value * 100.0) / 100.0

    /**
     * يحسب عمولة التاجر بناءً على هامش الربح الإجمالي:
     * grossMargin = (finalPrice - supplierCost).coerceAtLeast(0.0)
     */
    fun calculateCommission(
        finalPrice: Double,
        supplierCost: Double,
        commissionType: String,
        commissionValue: Double
    ): Double {
        val grossMargin = (finalPrice - supplierCost).coerceAtLeast(0.0)
        val safeValue = commissionValue.coerceAtLeast(0.0)
        return when (commissionType.trim().lowercase()) {
            "fixed" -> safeValue.coerceAtMost(grossMargin)
            else -> {
                val clampedPercent = safeValue.coerceAtMost(100.0)
                grossMargin * (clampedPercent / 100.0)
            }
        }.let(::round2)
    }

    /**
     * يحسب صافي ربح الوكالة بعد خصم تكلفة المورد وعمولة التاجر.
     */
    fun calculateNetProfit(
        finalPrice: Double,
        supplierCost: Double,
        commission: Double
    ): Double = round2(finalPrice - supplierCost - commission)

    /**
     * يحسب قيمة الخصم حسب نوعه (نسبة مئوية أو مبلغ ثابت) مع ضمان عدم تجاوز السعر الأساسي.
     */
    fun calculateDiscount(
        discountType: String,
        discountValue: Double,
        basePrice: Double
    ): Double {
        val safeBase = basePrice.coerceAtLeast(0.0)
        val safeValue = discountValue.coerceAtLeast(0.0)
        return when (discountType.trim().lowercase()) {
            "fixed" -> safeValue.coerceAtMost(safeBase)
            else -> {
                val clampedPercent = safeValue.coerceAtMost(100.0)
                safeBase * (clampedPercent / 100.0)
            }
        }.let(::round2)
    }

    /**
     * يحسب خصم الولاء عند بلوغ عتبة الطلبات المطلوبة.
     */
    fun calculateLoyaltyDiscount(
        pkgPrice: Double,
        threshold: Int,
        percent: Double,
        loyaltyCount: Int
    ): Double {
        val safePrice = pkgPrice.coerceAtLeast(0.0)
        if (safePrice <= 0.0 || threshold <= 0 || loyaltyCount < threshold) return 0.0
        val clampedPercent = percent.coerceIn(0.0, 100.0)
        return round2(safePrice * (clampedPercent / 100.0))
    }

    /**
     * يحسب السعر النهائي بعد تطبيق خصم الولاء وكود الخصم دون النزول تحت الصفر.
     */
    fun calculateFinalPrice(
        pkgPrice: Double,
        loyaltyDiscount: Double,
        codeDiscount: Double
    ): Double {
        val safePrice = pkgPrice.coerceAtLeast(0.0)
        val safeTotalDiscount = (loyaltyDiscount.coerceAtLeast(0.0) + codeDiscount.coerceAtLeast(0.0))
            .coerceAtMost(safePrice)
        return round2((safePrice - safeTotalDiscount).coerceAtLeast(0.0))
    }
}

// [REVIEWED 2026-11] إصلاحات شاملة
