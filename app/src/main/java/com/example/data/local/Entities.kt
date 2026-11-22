package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["whatsapp"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val uid: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val whatsapp: String,
    val role: String, // "admin" | "merchant" | "customer"
    val status: String, // "active" | "pending_approval" | "closed"
    val balance: Double = 0.0,
    val loyaltyCount: Int = 0,
    val commissionType: String = "percentage",
    val commissionValue: Double = 0.0,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val description: String,
    val packagesJson: String,
    val isAvailable: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val originalPrice: Double,
    val serviceCategory: String = "FREE_FIRE",
    val expiresAt: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "discount_codes",
    indices = [Index(value = ["code"], unique = true)]
)
data class DiscountCodeEntity(
    @PrimaryKey val id: String,
    val code: String,
    val discountType: String, // "percentage" | "fixed"
    val discountValue: Double,
    val includedCategoriesCsv: String = "ALL", // "ALL" or "FREE_FIRE,PUBG"
    val expiresAt: Long,
    val maxUses: Int = 50,
    val usedCount: Int = 0,
    val isPublic: Boolean = true,
    val targetCustomerId: String? = null,
    val targetCustomerName: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contests")
data class ContestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val prizeType: String, // "balance" | "free_topup" | "discount_code"
    val prizeLabel: String,
    val prizeAmount: Double = 0.0,
    val rulesText: String,
    val startsAt: Long,
    val endsAt: Long,
    val status: String = "active", // "active" | "ended" | "winner_selected"
    val winnerCustomerId: String? = null,
    val winnerCustomerName: String? = null,
    val isPrizeApproved: Boolean = false,
    val isWinnerNotified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contest_entries",
    indices = [Index(value = ["contestId", "customerId"], unique = true)]
)
data class ContestEntryEntity(
    @PrimaryKey val id: String,
    val contestId: String,
    val customerId: String,
    val customerName: String,
    val customerWhatsapp: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val customerWhatsapp: String,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val serviceId: String,
    val serviceName: String,
    val serviceCategory: String,
    val packageId: String,
    val packageLabel: String,
    val playerAccountInfo: String,
    val note: String = "",
    val isOfferOrder: Boolean = false,
    val discountCodeUsed: String? = null,
    val originalPrice: Double,
    val discountApplied: Double = 0.0,
    val finalPrice: Double,
    val supplierCost: Double,
    val merchantCommission: Double = 0.0,
    val netProfit: Double = 0.0,
    val status: String = "pending", // "pending" | "accepted" | "rejected" | "completed"
    val rejectionReason: String? = null,
    val isCustomerNotified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "topups")
data class TopUpEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val customerWhatsapp: String,
    val amount: Double,
    val paymentMethod: String,
    val transactionRef: String,
    val proofImageUri: String? = null,
    val status: String = "pending",
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    val isCustomerNotified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val actionType: String,
    val targetId: String,
    val details: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: String = "global",
    val agencyPhone: String = "0670533324",
    val whatsappNumber: String = "212670533324",
    val loyaltyThreshold: Int = 4,
    val loyaltyDiscountPercent: Double = 25.0,
    val adminEmail: String = "m95169829@gmail.com",
    val agencyBalance: Double = 0.0
)

@Entity(tableName = "auth_rate_limits")
data class AuthRateLimitEntity(
    @PrimaryKey val email: String,
    val failedAttempts: Int = 0,
    val lockoutUntil: Long = 0L
)
