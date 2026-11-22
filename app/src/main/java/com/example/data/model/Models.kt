package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class UserRole(val key: String, val arabicLabel: String) {
    ADMIN("admin", "الأدمن"),
    MERCHANT("merchant", "التاجر"),
    CUSTOMER("customer", "الزبون");

    companion object {
        fun fromKey(key: String): UserRole = entries.firstOrNull { it.key == key } ?: CUSTOMER
    }
}

enum class AccountStatus(val key: String, val arabicLabel: String) {
    ACTIVE("active", "نشط"),
    PENDING_APPROVAL("pending_approval", "قيد القبول"),
    CLOSED("closed", "مغلق / محظور");

    companion object {
        fun fromKey(key: String): AccountStatus = entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

enum class ServiceCategory(
    val key: String,
    val arabicTitle: String,
    val shortBadge: String,
    val inputHint: String
) {
    ROBUX("ROBUX", "شحن Robux", "ROBUX", "اسم المستخدم أو رابط الحساب في Roblox"),
    FREE_FIRE("FREE_FIRE", "شحن Free Fire", "FF", "معرف اللاعب (Player ID) في Free Fire"),
    PUBG("PUBG", "شحن PUBG UC", "PUBG", "معرف اللاعب (Player ID) في PUBG"),
    STEAM("STEAM", "بطاقات Steam", "STEAM", "البريد الإلكتروني أو رابط حساب Steam"),
    PLAYSTATION("PLAYSTATION", "بطاقات PlayStation", "PSN", "البريد أو المنطقة (USA / EU / UAE)"),
    XBOX("XBOX", "بطاقات Xbox", "XBOX", "البريد الإلكتروني لحساب Microsoft / Xbox");

    companion object {
        fun fromKey(key: String): ServiceCategory = entries.firstOrNull { it.key == key } ?: FREE_FIRE
    }
}

enum class OrderStatus(val key: String, val arabicLabel: String) {
    PENDING("pending", "قيد الانتظار"),
    ACCEPTED("accepted", "مقبول"),
    REJECTED("rejected", "مرفوض"),
    COMPLETED("completed", "مكتمل");

    companion object {
        fun fromKey(key: String): OrderStatus = entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class TopUpStatus(val key: String, val arabicLabel: String) {
    PENDING("pending", "قيد الانتظار"),
    ACCEPTED("accepted", "مقبول"),
    REJECTED("rejected", "مرفوض");

    companion object {
        fun fromKey(key: String): TopUpStatus = entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class CommissionType(val key: String, val arabicLabel: String) {
    PERCENTAGE("percentage", "نسبة مئوية (%)"),
    FIXED("fixed", "مبلغ ثابت (د.م)");

    companion object {
        fun fromKey(key: String): CommissionType = entries.firstOrNull { it.key == key } ?: PERCENTAGE
    }
}

enum class DiscountType(val key: String, val arabicLabel: String) {
    PERCENTAGE("percentage", "نسبة مئوية (%)"),
    FIXED("fixed", "مبلغ ثابت (د.م)");

    companion object {
        fun fromKey(key: String): DiscountType = entries.firstOrNull { it.key == key } ?: PERCENTAGE
    }
}

enum class ContestPrizeType(val key: String, val arabicLabel: String) {
    BALANCE("balance", "رصيد في المحفظة"),
    FREE_TOPUP("free_topup", "شحن مجاني"),
    DISCOUNT_CODE("discount_code", "كود خصم خاص");

    companion object {
        fun fromKey(key: String): ContestPrizeType = entries.firstOrNull { it.key == key } ?: BALANCE
    }
}

enum class ContestStatus(val key: String, val arabicLabel: String) {
    ACTIVE("active", "جارية"),
    ENDED("ended", "منتهية"),
    WINNER_SELECTED("winner_selected", "تم اختيار الفائز");

    companion object {
        fun fromKey(key: String): ContestStatus = entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

enum class ProfitPeriod(val arabicLabel: String, val daysBack: Int) {
    TODAY("اليوم", 1),
    WEEK("الأسبوع", 7),
    MONTH("الشهر", 30),
    ALL("الكل", 3650)
}

data class ServicePackage(
    val id: String,
    val label: String,
    val quantityLabel: String,
    val supplierCost: Double,
    val sellPrice: Double
)

object PackageJsonCodec {
    fun toJson(packages: List<ServicePackage>): String {
        val arr = JSONArray()
        packages.forEach { pkg ->
            val obj = JSONObject()
            obj.put("id", pkg.id)
            obj.put("label", pkg.label)
            obj.put("quantityLabel", pkg.quantityLabel)
            obj.put("supplierCost", pkg.supplierCost)
            obj.put("sellPrice", pkg.sellPrice)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun fromJson(json: String): List<ServicePackage> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ServicePackage(
                            id = obj.optString("id", "pkg_$i"),
                            label = obj.optString("label", ""),
                            quantityLabel = obj.optString("quantityLabel", ""),
                            supplierCost = obj.optDouble("supplierCost", 0.0),
                            sellPrice = obj.optDouble("sellPrice", 0.0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class ProfitSummary(
    val totalSales: Double = 0.0,
    val totalSupplierCost: Double = 0.0,
    val totalMerchantCommission: Double = 0.0,
    val netProfit: Double = 0.0,
    val todayProfit: Double = 0.0,
    val weekProfit: Double = 0.0,
    val monthProfit: Double = 0.0,
    val completedCount: Int = 0
)

data class ServiceProfitItem(
    val serviceName: String,
    val category: ServiceCategory,
    val ordersCount: Int,
    val totalSales: Double,
    val netProfit: Double
)

data class MerchantProfitItem(
    val merchantId: String,
    val merchantName: String,
    val ordersCount: Int,
    val totalSales: Double,
    val totalCommission: Double,
    val netProfitGenerated: Double
)

data class DailyProfitPoint(
    val dayLabel: String,
    val sales: Double,
    val profit: Double
)
