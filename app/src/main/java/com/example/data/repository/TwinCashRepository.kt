package com.example.data.repository

import com.example.data.local.AuditLogEntity
import com.example.data.local.AuthRateLimitEntity
import com.example.data.local.ContestEntity
import com.example.data.local.ContestEntryEntity
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.OfferEntity
import com.example.data.local.OrderEntity
import com.example.data.local.PasswordHasher
import com.example.data.local.SeedData
import com.example.data.local.ServiceEntity
import com.example.data.local.SessionManager
import com.example.data.local.SettingsEntity
import com.example.data.local.TopUpEntity
import com.example.data.local.TwinCashDatabase
import com.example.data.local.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.CommissionType
import com.example.data.model.ContestPrizeType
import com.example.data.model.ContestStatus
import com.example.data.model.DiscountType
import com.example.data.model.PackageJsonCodec
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePackage
import com.example.data.model.UserRole
import com.example.domain.PricingEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TwinCashRepository(
    private val db: TwinCashDatabase,
    private val sessionManager: SessionManager
) {
    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 5 * 60_000L
    }

    val currentUidFlow: Flow<String?> = sessionManager.currentUidFlow
    val allUsersFlow: Flow<List<UserEntity>> = db.userDao().observeAllUsers()
    val allServicesFlow: Flow<List<ServiceEntity>> = db.serviceDao().observeAllServices()
    val allOffersFlow: Flow<List<OfferEntity>> = db.offerDao().observeAllOffers()
    val allDiscountCodesFlow: Flow<List<DiscountCodeEntity>> = db.discountCodeDao().observeAllCodes()
    val allContestsFlow: Flow<List<ContestEntity>> = db.contestDao().observeAllContests()
    val allContestEntriesFlow: Flow<List<ContestEntryEntity>> = db.contestEntryDao().observeAllEntries()
    val allOrdersFlow: Flow<List<OrderEntity>> = db.orderDao().observeAllOrders()
    val allTopUpsFlow: Flow<List<TopUpEntity>> = db.topUpDao().observeAllTopUps()
    val allAuditLogsFlow: Flow<List<AuditLogEntity>> = db.auditLogDao().observeAllLogs()
    val settingsFlow: Flow<SettingsEntity?> = db.settingsDao().observeSettings()

    fun observeUser(uid: String): Flow<UserEntity?> = db.userDao().observeUserById(uid)
    fun observeCustomerOrders(uid: String): Flow<List<OrderEntity>> = db.orderDao().observeOrdersByCustomer(uid)
    fun observeCustomerTopUps(uid: String): Flow<List<TopUpEntity>> = db.topUpDao().observeTopUpsByCustomer(uid)
    fun observeCustomerCodes(uid: String): Flow<List<DiscountCodeEntity>> = db.discountCodeDao().observeCodesForCustomer(uid)
    fun observeCustomerContestEntries(uid: String): Flow<List<ContestEntryEntity>> = db.contestEntryDao().observeEntriesForCustomer(uid)
    fun observeMerchantOrders(uid: String): Flow<List<OrderEntity>> = db.orderDao().observeOrdersForMerchant(uid)

    suspend fun ensureSeeded() {
        SeedData.seedIfEmpty(db)
    }

    suspend fun login(emailRaw: String, password: String): Result<UserEntity> {
        val email = emailRaw.trim().lowercase()
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال البريد الإلكتروني وكلمة المرور"))
        }

        val now = System.currentTimeMillis()
        val rateLimit = db.authRateLimitDao().getRateLimit(email)
        if (rateLimit != null && rateLimit.lockoutUntil > now) {
            val remainingSec = ((rateLimit.lockoutUntil - now) / 1000L).coerceAtLeast(1L)
            val mins = remainingSec / 60
            val secs = remainingSec % 60
            return Result.failure(
                IllegalStateException("تم حظر الحساب مؤقتاً بعد 5 محاولات فاشلة. حاول بعد $mins د و $secs ث")
            )
        }

        val user = db.userDao().getUserByEmail(email)
        val validPassword = user != null && PasswordHasher.verifyPassword(password, user.passwordHash)
        if (!validPassword || user == null) {
            val currentFailures = if (rateLimit != null && rateLimit.lockoutUntil <= now && rateLimit.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                1
            } else {
                (rateLimit?.failedAttempts ?: 0) + 1
            }
            val lockoutUntil = if (currentFailures >= MAX_FAILED_ATTEMPTS) now + LOCKOUT_DURATION_MS else 0L
            db.authRateLimitDao().upsertRateLimit(
                AuthRateLimitEntity(email = email, failedAttempts = currentFailures, lockoutUntil = lockoutUntil)
            )
            val remaining = (MAX_FAILED_ATTEMPTS - currentFailures).coerceAtLeast(0)
            return if (remaining == 0) {
                Result.failure(IllegalStateException("تم حظرك مؤقتاً لمدة 5 دقائق بسبب تكرار المحاولات الفاشلة"))
            } else {
                Result.failure(IllegalArgumentException("بيانات الدخول غير صحيحة. المحاولات المتبقية: $remaining"))
            }
        }

        if (user.status == AccountStatus.PENDING_APPROVAL.key) {
            return Result.failure(IllegalStateException("حساب التاجر قيد المراجعة والقبول من طرف الأدمن"))
        }
        if (user.status == AccountStatus.CLOSED.key) {
            return Result.failure(IllegalStateException("تم إغلاق هذا الحساب أو حظره من طرف إدارة TwinCash"))
        }

        db.authRateLimitDao().clearRateLimit(email)
        sessionManager.saveSession(user.uid)
        return Result.success(user)
    }

    suspend fun registerCustomer(
        fullName: String,
        emailRaw: String,
        whatsappRaw: String,
        password: String
    ): Result<UserEntity> {
        val name = fullName.trim()
        val email = emailRaw.trim().lowercase()
        val whatsapp = normalizePhone(whatsappRaw)

        if (name.length < 3) return Result.failure(IllegalArgumentException("الاسم الكامل قصير جداً"))
        if (!email.contains("@") || !email.contains(".")) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صحيح"))
        }
        if (whatsapp.length < 9) {
            return Result.failure(IllegalArgumentException("رقم الواتساب إجباري ويجب أن يكون صحيحاً"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("كلمة المرور يجب أن تتكون من 6 أحرف على الأقل"))
        }
        val isOwnerAdmin = email == SeedData.ADMIN_EMAIL.lowercase()
        if (db.userDao().getUserByEmail(email) != null) {
            return Result.failure(
                IllegalStateException(
                    if (isOwnerAdmin) "حساب الأدمن مسجل مسبقاً، يرجى تسجيل الدخول"
                    else "هذا البريد الإلكتروني مسجل مسبقاً"
                )
            )
        }
        if (db.userDao().getUserByWhatsapp(whatsapp) != null) {
            return Result.failure(IllegalStateException("رقم الواتساب مسجل بالفعل لحساب آخر"))
        }

        val newUser = UserEntity(
            uid = if (isOwnerAdmin) "admin_owner" else "cust_${UUID.randomUUID().toString().take(8)}",
            fullName = name,
            email = email,
            passwordHash = PasswordHasher.hashPassword(password),
            whatsapp = whatsapp,
            role = if (isOwnerAdmin) UserRole.ADMIN.key else UserRole.CUSTOMER.key,
            status = AccountStatus.ACTIVE.key,
            balance = 0.0,
            loyaltyCount = 0
        )
        db.userDao().insertUser(newUser)
        sessionManager.saveSession(newUser.uid)
        return Result.success(newUser)
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun updateCustomerProfile(
        uid: String,
        newName: String,
        newWhatsappRaw: String,
        avatarUri: String?
    ): Result<Unit> {
        val user = db.userDao().getUserById(uid) ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        val name = newName.trim()
        val whatsapp = normalizePhone(newWhatsappRaw)
        if (name.length < 3) return Result.failure(IllegalArgumentException("الاسم الكامل غير صالح"))
        if (whatsapp.length < 9) return Result.failure(IllegalArgumentException("رقم الواتساب غير صالح"))

        val existingPhone = db.userDao().getUserByWhatsapp(whatsapp)
        if (existingPhone != null && existingPhone.uid != uid) {
            return Result.failure(IllegalStateException("رقم الواتساب مستخدم بالفعل من طرف حساب آخر"))
        }

        db.userDao().updateUser(
            user.copy(
                fullName = name,
                whatsapp = whatsapp,
                avatarUri = avatarUri ?: user.avatarUri
            )
        )
        return Result.success(Unit)
    }

    suspend fun validateDiscountCode(
        customerId: String,
        serviceCategory: String,
        rawCode: String,
        basePrice: Double
    ): Result<Pair<DiscountCodeEntity, Double>> {
        val clean = rawCode.trim().uppercase()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("يرجى إدخال كود الخصم"))

        val entity = db.discountCodeDao().getByCode(clean)
            ?: return Result.failure(IllegalArgumentException("كود الخصم غير موجود"))
        val now = System.currentTimeMillis()
        if (!entity.isActive) return Result.failure(IllegalStateException("هذا الكود معطل حالياً"))
        if (entity.expiresAt < now) return Result.failure(IllegalStateException("انتهت صلاحية كود الخصم"))
        if (entity.usedCount >= entity.maxUses) {
            return Result.failure(IllegalStateException("تم استنفاد الحد الأقصى لاستعمال هذا الكود"))
        }
        if (!entity.isPublic && entity.targetCustomerId != customerId) {
            return Result.failure(SecurityException("هذا الكود مخصص لزبون آخر ولا يمكنك استخدامه"))
        }
        if (entity.includedCategoriesCsv != "ALL") {
            val allowed = entity.includedCategoriesCsv.split(",").map { it.trim() }
            if (!allowed.contains(serviceCategory)) {
                return Result.failure(IllegalArgumentException("هذا الكود لا يشمل هذه الخدمة"))
            }
        }

        // [FIX #1.2] استخدام محرك التسعير الموحّد لحساب خصم الكود
        val amount = PricingEngine.calculateDiscount(
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            basePrice = basePrice
        )
        return Result.success(entity to amount)
    }

    suspend fun placeServiceOrder(
        customerId: String,
        serviceId: String,
        packageId: String,
        playerAccountInfo: String,
        note: String,
        discountCodeRaw: String? = null
    ): Result<OrderEntity> {
        val customer = db.userDao().getUserById(customerId)
            ?: return Result.failure(IllegalStateException("الحساب غير موجود"))
        if (customer.status != AccountStatus.ACTIVE.key) {
            return Result.failure(IllegalStateException("حسابك غير نشط حالياً"))
        }
        val service = db.serviceDao().getServiceById(serviceId)
            ?: return Result.failure(IllegalStateException("الخدمة غير موجودة"))
        if (!service.isAvailable) {
            return Result.failure(IllegalStateException("هذه الخدمة غير متاحة حالياً"))
        }
        val packages = PackageJsonCodec.fromJson(service.packagesJson)
        val pkg = packages.firstOrNull { it.id == packageId }
            ?: return Result.failure(IllegalStateException("الباقة المختارة غير متوفرة"))
        if (playerAccountInfo.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بيانات الحساب أو المعرف"))
        }

        val settings = db.settingsDao().getSettings() ?: SettingsEntity()
        // [FIX #1.2] استخدام محرك التسعير الموحّد لحساب خصم الولاء والسعر النهائي
        val loyaltyDiscount = PricingEngine.calculateLoyaltyDiscount(
            pkgPrice = pkg.sellPrice,
            threshold = settings.loyaltyThreshold,
            percent = settings.loyaltyDiscountPercent,
            loyaltyCount = customer.loyaltyCount
        )
        val loyaltyEligible = loyaltyDiscount > 0.0 || customer.loyaltyCount >= settings.loyaltyThreshold

        var validatedCode: DiscountCodeEntity? = null
        var codeDiscount = 0.0
        if (!discountCodeRaw.isNullOrBlank()) {
            val codeCheck = validateDiscountCode(
                customerId = customerId,
                serviceCategory = service.category,
                rawCode = discountCodeRaw,
                basePrice = (pkg.sellPrice - loyaltyDiscount).coerceAtLeast(0.0)
            )
            if (codeCheck.isFailure) {
                return Result.failure(codeCheck.exceptionOrNull() ?: IllegalArgumentException("كود غير صالح"))
            }
            val pair = codeCheck.getOrThrow()
            validatedCode = pair.first
            codeDiscount = pair.second
        }

        val finalPrice = PricingEngine.calculateFinalPrice(
            pkgPrice = pkg.sellPrice,
            loyaltyDiscount = loyaltyDiscount,
            codeDiscount = codeDiscount
        )
        val totalDiscount = (pkg.sellPrice - finalPrice).coerceAtLeast(0.0).round2()

        if (customer.balance < finalPrice) {
            return Result.failure(
                IllegalStateException("رصيدك غير كافٍ (${customer.balance.round2()} د.م). المبلغ المطلوب: $finalPrice د.م")
            )
        }

        val updatedLoyalty = if (loyaltyEligible) 0 else customer.loyaltyCount
        db.userDao().updateUser(
            customer.copy(
                balance = (customer.balance - finalPrice).round2(),
                loyaltyCount = updatedLoyalty
            )
        )

        if (validatedCode != null) {
            db.discountCodeDao().upsertCode(
                validatedCode.copy(usedCount = validatedCode.usedCount + 1)
            )
        }

        val order = OrderEntity(
            id = "ORD-${(1050..9999).random()}",
            customerId = customer.uid,
            customerName = customer.fullName,
            customerWhatsapp = customer.whatsapp,
            serviceId = service.id,
            serviceName = service.name,
            serviceCategory = service.category,
            packageId = pkg.id,
            packageLabel = pkg.label,
            playerAccountInfo = playerAccountInfo.trim(),
            note = note.trim(),
            isOfferOrder = false,
            discountCodeUsed = validatedCode?.code,
            originalPrice = pkg.sellPrice,
            discountApplied = totalDiscount,
            finalPrice = finalPrice,
            supplierCost = pkg.supplierCost,
            status = "pending",
            isCustomerNotified = true
        )
        db.orderDao().upsertOrder(order)
        return Result.success(order)
    }

    suspend fun placeOfferOrder(
        customerId: String,
        offerId: String,
        playerAccountInfo: String,
        note: String
    ): Result<OrderEntity> {
        val customer = db.userDao().getUserById(customerId)
            ?: return Result.failure(IllegalStateException("الحساب غير موجود"))
        if (customer.status != AccountStatus.ACTIVE.key) {
            return Result.failure(IllegalStateException("حسابك غير نشط حالياً"))
        }
        val offer = db.offerDao().getOfferById(offerId)
            ?: return Result.failure(IllegalStateException("العرض غير موجود"))
        if (!offer.isActive || offer.expiresAt < System.currentTimeMillis()) {
            return Result.failure(IllegalStateException("هذا العرض منتهي الصلاحية أو غير نشط"))
        }
        if (playerAccountInfo.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال معرف اللاعب (Player ID)"))
        }
        if (customer.balance < offer.price) {
            return Result.failure(
                IllegalStateException("رصيدك غير كافٍ (${customer.balance.round2()} د.م). سعر العرض: ${offer.price} د.م")
            )
        }

        db.userDao().updateUser(
            customer.copy(balance = (customer.balance - offer.price).round2())
        )

        val estimatedSupplierCost = (offer.price * 0.72).round2()
        val order = OrderEntity(
            id = "ORD-${(1050..9999).random()}",
            customerId = customer.uid,
            customerName = customer.fullName,
            customerWhatsapp = customer.whatsapp,
            serviceId = offer.id,
            serviceName = offer.title,
            serviceCategory = ServiceCategory.FREE_FIRE.key,
            packageId = offer.id,
            packageLabel = "عرض Free Fire خاص",
            playerAccountInfo = playerAccountInfo.trim(),
            note = note.trim(),
            isOfferOrder = true,
            originalPrice = offer.originalPrice,
            discountApplied = (offer.originalPrice - offer.price).coerceAtLeast(0.0).round2(),
            finalPrice = offer.price,
            supplierCost = estimatedSupplierCost,
            status = "pending",
            isCustomerNotified = true
        )
        db.orderDao().upsertOrder(order)
        return Result.success(order)
    }

    suspend fun acceptAndCompleteOrder(actorId: String, orderId: String): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        if (actor.role != UserRole.ADMIN.key && (actor.role != UserRole.MERCHANT.key || actor.status != AccountStatus.ACTIVE.key)) {
            return Result.failure(IllegalStateException("غير مصرح لك بتنفيذ الطلبات"))
        }
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(IllegalStateException("الطلب غير موجود"))
        if (order.status != "pending" && order.status != "accepted") {
            return Result.failure(IllegalStateException("تمت معالجة هذا الطلب مسبقاً"))
        }

        // [FIX #1.2] [FIX #1.3] توحيد حساب العمولة وصافي الربح عبر PricingEngine اعتماداً على grossMargin
        val commission = if (actor.role == UserRole.MERCHANT.key) {
            PricingEngine.calculateCommission(
                finalPrice = order.finalPrice,
                supplierCost = order.supplierCost,
                commissionType = actor.commissionType,
                commissionValue = actor.commissionValue
            )
        } else {
            0.0
        }
        val netProfit = PricingEngine.calculateNetProfit(
            finalPrice = order.finalPrice,
            supplierCost = order.supplierCost,
            commission = commission
        )

        db.orderDao().upsertOrder(
            order.copy(
                merchantId = actor.uid,
                merchantName = actor.fullName,
                merchantCommission = commission,
                netProfit = netProfit,
                status = "completed",
                isCustomerNotified = false,
                updatedAt = System.currentTimeMillis()
            )
        )

        val settings = db.settingsDao().getSettings() ?: SettingsEntity()
        db.settingsDao().upsertSettings(
            settings.copy(agencyBalance = (settings.agencyBalance + order.finalPrice).round2())
        )

        // Increment customer loyalty counter ONLY for accepted/completed regular non-discounted orders
        if (!order.isOfferOrder && order.discountApplied == 0.0) {
            val customer = db.userDao().getUserById(order.customerId)
            if (customer != null) {
                val nextLoyalty = (customer.loyaltyCount + 1).coerceAtMost(settings.loyaltyThreshold)
                db.userDao().updateUser(customer.copy(loyaltyCount = nextLoyalty))
            }
        }

        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = actor.uid,
                actorName = actor.fullName,
                actorRole = actor.role,
                actionType = "قبول وإتمام طلب",
                targetId = order.id,
                details = "إتمام طلب ${order.serviceName} (${order.packageLabel}) للزبون ${order.customerName}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun rejectOrder(actorId: String, orderId: String, reason: String): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        if (actor.role != UserRole.ADMIN.key && (actor.role != UserRole.MERCHANT.key || actor.status != AccountStatus.ACTIVE.key)) {
            return Result.failure(IllegalStateException("غير مصرح لك برفض الطلبات"))
        }
        val cleanReason = reason.trim().ifBlank { "تعذر تنفيذ الطلب، تم استرجاع المبلغ لرصيدك" }
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(IllegalStateException("الطلب غير موجود"))
        if (order.status != "pending" && order.status != "accepted") {
            return Result.failure(IllegalStateException("تمت معالجة هذا الطلب مسبقاً"))
        }

        val settings = db.settingsDao().getSettings() ?: SettingsEntity()
        val customer = db.userDao().getUserById(order.customerId)
        if (customer != null) {
            val restoredLoyalty = if (!order.isOfferOrder && order.discountApplied > 0.0 && order.discountCodeUsed == null) {
                settings.loyaltyThreshold
            } else {
                customer.loyaltyCount
            }
            db.userDao().updateUser(
                customer.copy(
                    balance = (customer.balance + order.finalPrice).round2(),
                    loyaltyCount = restoredLoyalty
                )
            )
        }

        // [FIX #1.6] إنقاص عداد استخدام كود الخصم بمقدار 1 عند رفض الطلب بشرط ألا يقل عن 0
        if (!order.discountCodeUsed.isNullOrBlank()) {
            val codeEntity = db.discountCodeDao().getByCode(order.discountCodeUsed)
            if (codeEntity != null && codeEntity.usedCount > 0) {
                db.discountCodeDao().upsertCode(
                    codeEntity.copy(usedCount = (codeEntity.usedCount - 1).coerceAtLeast(0))
                )
            }
        }

        db.orderDao().upsertOrder(
            order.copy(
                merchantId = actor.uid,
                merchantName = actor.fullName,
                status = "rejected",
                rejectionReason = cleanReason,
                isCustomerNotified = false,
                updatedAt = System.currentTimeMillis()
            )
        )

        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = actor.uid,
                actorName = actor.fullName,
                actorRole = actor.role,
                actionType = "رفض طلب واسترجاع رصيد",
                targetId = order.id,
                details = "رفض طلب ${order.id} للزبون ${order.customerName} السبب: $cleanReason"
            )
        )
        return Result.success(Unit)
    }

    suspend fun submitTopUp(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        proofImageUri: String?
    ): Result<TopUpEntity> {
        val customer = db.userDao().getUserById(customerId)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        if (amount <= 0.0) return Result.failure(IllegalArgumentException("يرجى إدخال مبلغ صحيح أكبر من 0"))
        if (transactionRef.trim().isEmpty() && proofImageUri == null) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رقم العملية أو إرفاق صورة الإثبات"))
        }

        val topUp = TopUpEntity(
            id = "TOP-${(1000..9999).random()}",
            customerId = customer.uid,
            customerName = customer.fullName,
            customerWhatsapp = customer.whatsapp,
            amount = amount.round2(),
            paymentMethod = paymentMethod,
            transactionRef = transactionRef.trim().ifBlank { "مرفق صورة" },
            proofImageUri = proofImageUri,
            status = "pending",
            isCustomerNotified = true
        )
        db.topUpDao().upsertTopUp(topUp)
        return Result.success(topUp)
    }

    suspend fun approveTopUp(adminId: String, topUpId: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val topUp = db.topUpDao().getTopUpById(topUpId)
            ?: return Result.failure(IllegalStateException("طلب الشحن غير موجود"))
        if (topUp.status != "pending") return Result.failure(IllegalStateException("تمت معالجة الطلب مسبقاً"))

        val customer = db.userDao().getUserById(topUp.customerId)
            ?: return Result.failure(IllegalStateException("الزبون غير موجود"))

        db.userDao().updateUser(customer.copy(balance = (customer.balance + topUp.amount).round2()))
        db.topUpDao().upsertTopUp(
            topUp.copy(
                status = "accepted",
                reviewedBy = admin.fullName,
                isCustomerNotified = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        val settings = db.settingsDao().getSettings() ?: SettingsEntity()
        db.settingsDao().upsertSettings(
            settings.copy(agencyBalance = (settings.agencyBalance + topUp.amount).round2())
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "قبول تعبئة رصيد",
                targetId = topUp.id,
                details = "إضافة ${topUp.amount} د.م لرصيد ${customer.fullName}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun rejectTopUp(adminId: String, topUpId: String, reason: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val topUp = db.topUpDao().getTopUpById(topUpId)
            ?: return Result.failure(IllegalStateException("طلب الشحن غير موجود"))
        if (topUp.status != "pending") return Result.failure(IllegalStateException("تمت معالجة الطلب مسبقاً"))

        val cleanReason = reason.trim().ifBlank { "تعذر التحقق من وصل التحويل" }
        db.topUpDao().upsertTopUp(
            topUp.copy(
                status = "rejected",
                rejectionReason = cleanReason,
                reviewedBy = admin.fullName,
                isCustomerNotified = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "رفض تعبئة رصيد",
                targetId = topUp.id,
                details = "رفض تعبئة ${topUp.amount} د.م للزبون ${topUp.customerName}: $cleanReason"
            )
        )
        return Result.success(Unit)
    }

    suspend fun markNotificationsRead(customerId: String) {
        db.orderDao().markCustomerOrdersNotified(customerId)
        db.topUpDao().markCustomerTopUpsNotified(customerId)
        db.contestDao().markWinnerNotified(customerId)
    }

    // --- Contests (Customer Entry + Admin Draw/Approval) ---

    suspend fun joinContest(customerId: String, contestId: String): Result<Unit> {
        val customer = db.userDao().getUserById(customerId)
            ?: return Result.failure(IllegalStateException("الحساب غير موجود"))
        if (customer.role != UserRole.CUSTOMER.key || customer.status != AccountStatus.ACTIVE.key) {
            return Result.failure(IllegalStateException("المشاركة متاحة للزبائن النشطين فقط"))
        }
        val contest = db.contestDao().getContestById(contestId)
            ?: return Result.failure(IllegalStateException("المسابقة غير موجودة"))
        val now = System.currentTimeMillis()
        if (contest.status != ContestStatus.ACTIVE.key || now < contest.startsAt || now > contest.endsAt) {
            return Result.failure(IllegalStateException("هذه المسابقة غير متاحة للمشاركة حالياً"))
        }
        if (db.contestEntryDao().getEntry(contestId, customerId) != null) {
            return Result.failure(IllegalStateException("لقد شاركت مسبقاً في هذه المسابقة"))
        }

        val entry = ContestEntryEntity(
            id = "${contestId}_${customerId}",
            contestId = contestId,
            customerId = customer.uid,
            customerName = customer.fullName,
            customerWhatsapp = customer.whatsapp
        )
        db.contestEntryDao().insertEntry(entry)
        return Result.success(Unit)
    }

    suspend fun saveContest(
        adminId: String,
        id: String?,
        title: String,
        description: String,
        prizeType: ContestPrizeType,
        prizeLabel: String,
        prizeAmount: Double,
        rulesText: String,
        daysDuration: Int
    ): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        if (title.isBlank() || prizeLabel.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال عنوان المسابقة والجائزة"))
        }
        val now = System.currentTimeMillis()
        val existing = id?.let { db.contestDao().getContestById(it) }
        val contest = ContestEntity(
            id = id ?: "cnt_${UUID.randomUUID().toString().take(8)}",
            title = title.trim(),
            description = description.trim(),
            prizeType = prizeType.key,
            prizeLabel = prizeLabel.trim(),
            prizeAmount = prizeAmount.coerceAtLeast(0.0).round2(),
            rulesText = rulesText.trim().ifBlank { "مشاركة واحدة لكل زبون." },
            startsAt = existing?.startsAt ?: now,
            endsAt = now + daysDuration.coerceAtLeast(1) * 24L * 60L * 60L * 1000L,
            status = existing?.status ?: ContestStatus.ACTIVE.key,
            winnerCustomerId = existing?.winnerCustomerId,
            winnerCustomerName = existing?.winnerCustomerName,
            isPrizeApproved = existing?.isPrizeApproved ?: false,
            isWinnerNotified = existing?.isWinnerNotified ?: false
        )
        db.contestDao().upsertContest(contest)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = if (id == null) "إنشاء مسابقة" else "تعديل مسابقة",
                targetId = contest.id,
                details = "حفظ مسابقة: ${contest.title}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun selectContestWinner(
        adminId: String,
        contestId: String,
        specificCustomerId: String?
    ): Result<String> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val contest = db.contestDao().getContestById(contestId)
            ?: return Result.failure(IllegalStateException("المسابقة غير موجودة"))
        val entries = db.contestEntryDao().getEntriesForContest(contestId)
        if (entries.isEmpty()) {
            return Result.failure(IllegalStateException("لا يوجد مشاركون في هذه المسابقة بعد"))
        }
        val winnerEntry = if (specificCustomerId != null) {
            entries.firstOrNull { it.customerId == specificCustomerId }
                ?: return Result.failure(IllegalStateException("المشارك المختار غير موجود"))
        } else {
            entries.random()
        }

        db.contestDao().upsertContest(
            contest.copy(
                status = ContestStatus.WINNER_SELECTED.key,
                winnerCustomerId = winnerEntry.customerId,
                winnerCustomerName = winnerEntry.customerName,
                isPrizeApproved = false,
                isWinnerNotified = false
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "اختيار فائز بمسابقة",
                targetId = contest.id,
                details = "اختيار الفائز ${winnerEntry.customerName} في مسابقة ${contest.title} (بانتظار اعتماد الجائزة)"
            )
        )
        return Result.success(winnerEntry.customerName)
    }

    suspend fun approveContestPrize(adminId: String, contestId: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val contest = db.contestDao().getContestById(contestId)
            ?: return Result.failure(IllegalStateException("المسابقة غير موجودة"))
        val winnerId = contest.winnerCustomerId
            ?: return Result.failure(IllegalStateException("لم يتم اختيار فائز بعد"))
        if (contest.isPrizeApproved) {
            return Result.failure(IllegalStateException("تم اعتماد الجائزة ومنحها مسبقاً"))
        }

        val winner = db.userDao().getUserById(winnerId)
            ?: return Result.failure(IllegalStateException("حساب الفائز غير موجود"))

        if (contest.prizeType == ContestPrizeType.BALANCE.key && contest.prizeAmount > 0.0) {
            db.userDao().updateUser(
                winner.copy(balance = (winner.balance + contest.prizeAmount).round2())
            )
        } else if (contest.prizeType == ContestPrizeType.DISCOUNT_CODE.key) {
            val winCode = "WIN${(100..999).random()}"
            db.discountCodeDao().upsertCode(
                DiscountCodeEntity(
                    id = "dc_${UUID.randomUUID().toString().take(8)}",
                    code = winCode,
                    discountType = DiscountType.FIXED.key,
                    discountValue = contest.prizeAmount.coerceAtLeast(25.0),
                    includedCategoriesCsv = "ALL",
                    expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60_000L,
                    maxUses = 1,
                    usedCount = 0,
                    isPublic = false,
                    targetCustomerId = winner.uid,
                    targetCustomerName = winner.fullName,
                    isActive = true
                )
            )
        }

        db.contestDao().upsertContest(
            contest.copy(
                isPrizeApproved = true,
                isWinnerNotified = false
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "اعتماد جائزة مسابقة",
                targetId = contest.id,
                details = "اعتماد ومنح جائزة (${contest.prizeLabel}) للفائز ${winner.fullName}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteContest(adminId: String, contestId: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        db.contestDao().deleteContest(contestId)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "حذف مسابقة",
                targetId = contestId,
                details = "حذف المسابقة $contestId"
            )
        )
        return Result.success(Unit)
    }

    // --- Admin Discount Codes Management ---

    suspend fun saveDiscountCode(
        adminId: String,
        id: String?,
        codeRaw: String,
        discountType: DiscountType,
        discountValue: Double,
        includedCategoriesCsv: String,
        daysValid: Int,
        maxUses: Int,
        isPublic: Boolean,
        targetCustomerId: String?,
        isActive: Boolean
    ): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val cleanCode = codeRaw.trim().uppercase().replace(" ", "")
        if (cleanCode.length < 3 || discountValue <= 0.0 || maxUses <= 0) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كود صحيح وقيمة خصم وعدد استعمالات موجب"))
        }
        val existingByCode = db.discountCodeDao().getByCode(cleanCode)
        if (existingByCode != null && existingByCode.id != id) {
            return Result.failure(IllegalStateException("هذا الكود موجود مسبقاً"))
        }
        val targetUser = if (!isPublic && !targetCustomerId.isNullOrBlank()) {
            db.userDao().getUserById(targetCustomerId)
        } else null

        val existing = id?.let { db.discountCodeDao().getById(it) }
        val entity = DiscountCodeEntity(
            id = id ?: "dc_${UUID.randomUUID().toString().take(8)}",
            code = cleanCode,
            discountType = discountType.key,
            discountValue = discountValue.round2(),
            includedCategoriesCsv = includedCategoriesCsv.ifBlank { "ALL" },
            expiresAt = System.currentTimeMillis() + daysValid.coerceAtLeast(1) * 24L * 60 * 60_000L,
            maxUses = maxUses,
            usedCount = existing?.usedCount ?: 0,
            isPublic = isPublic,
            targetCustomerId = targetUser?.uid,
            targetCustomerName = targetUser?.fullName,
            isActive = isActive
        )
        db.discountCodeDao().upsertCode(entity)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = if (id == null) "إنشاء كود خصم" else "تعديل كود خصم",
                targetId = entity.id,
                details = "حفظ كود ${entity.code} (${if (isPublic) "عام" else "لـ ${targetUser?.fullName}"})"
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleDiscountCode(adminId: String, codeId: String, active: Boolean): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val existing = db.discountCodeDao().getById(codeId)
            ?: return Result.failure(IllegalStateException("الكود غير موجود"))
        db.discountCodeDao().upsertCode(existing.copy(isActive = active))
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "تغيير حالة كود خصم",
                targetId = codeId,
                details = "${if (active) "تفعيل" else "تعطيل"} الكود ${existing.code}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteDiscountCode(adminId: String, codeId: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        db.discountCodeDao().deleteCode(codeId)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "حذف كود خصم",
                targetId = codeId,
                details = "حذف كود الخصم $codeId"
            )
        )
        return Result.success(Unit)
    }

    // --- Admin Services, Offers, Merchants, Customers, Settings ---

    suspend fun saveService(
        adminId: String,
        id: String?,
        category: ServiceCategory,
        name: String,
        description: String,
        packages: List<ServicePackage>,
        isAvailable: Boolean,
        displayOrder: Int
    ): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        if (name.isBlank() || packages.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الخدمة وباقة واحدة على الأقل"))
        }
        val entity = ServiceEntity(
            id = id ?: "srv_${UUID.randomUUID().toString().take(8)}",
            category = category.key,
            name = name.trim(),
            description = description.trim(),
            packagesJson = PackageJsonCodec.toJson(packages),
            isAvailable = isAvailable,
            displayOrder = displayOrder
        )
        db.serviceDao().upsertService(entity)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = if (id == null) "إضافة خدمة" else "تعديل خدمة",
                targetId = entity.id,
                details = "تحديث خدمة ${entity.name} (الترتيب: $displayOrder)"
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleServiceAvailability(adminId: String, serviceId: String, isAvailable: Boolean): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val srv = db.serviceDao().getServiceById(serviceId)
            ?: return Result.failure(IllegalStateException("الخدمة غير موجودة"))
        db.serviceDao().upsertService(srv.copy(isAvailable = isAvailable))
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = if (isAvailable) "إظهار خدمة" else "إخفاء خدمة مؤقتاً",
                targetId = srv.id,
                details = "${if (isAvailable) "تفعيل" else "إخفاء"} خدمة ${srv.name}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteService(adminId: String, serviceId: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        db.serviceDao().deleteService(serviceId)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "حذف خدمة",
                targetId = serviceId,
                details = "حذف الخدمة $serviceId"
            )
        )
        return Result.success(Unit)
    }

    suspend fun saveOffer(
        adminId: String,
        id: String?,
        title: String,
        description: String,
        price: Double,
        originalPrice: Double,
        daysValid: Int,
        isActive: Boolean
    ): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        if (title.isBlank() || price <= 0.0) {
            return Result.failure(IllegalArgumentException("يرجى إدخال عنوان العرض وسعر صحيح"))
        }
        val expiresAt = System.currentTimeMillis() + (daysValid.coerceAtLeast(1) * 24L * 60L * 60L * 1000L)
        val offer = OfferEntity(
            id = id ?: "off_${UUID.randomUUID().toString().take(8)}",
            title = title.trim(),
            description = description.trim(),
            price = price.round2(),
            originalPrice = originalPrice.coerceAtLeast(price).round2(),
            serviceCategory = ServiceCategory.FREE_FIRE.key,
            expiresAt = expiresAt,
            isActive = isActive
        )
        db.offerDao().upsertOffer(offer)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = if (id == null) "إضافة عرض Free Fire" else "تعديل عرض Free Fire",
                targetId = offer.id,
                details = "تحديث عرض ${offer.title} بسعر ${offer.price} د.م"
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteOffer(adminId: String, offerId: String): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        db.offerDao().deleteOffer(offerId)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "حذف عرض",
                targetId = offerId,
                details = "حذف عرض Free Fire ($offerId)"
            )
        )
        return Result.success(Unit)
    }

    suspend fun createMerchant(
        adminId: String,
        fullName: String,
        emailRaw: String,
        whatsappRaw: String,
        password: String,
        commissionType: CommissionType,
        commissionValue: Double
    ): Result<UserEntity> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val email = emailRaw.trim().lowercase()
        val whatsapp = normalizePhone(whatsappRaw)
        if (fullName.trim().length < 3 || !email.contains("@") || whatsapp.length < 9 || password.length < 6) {
            return Result.failure(IllegalArgumentException("يرجى التحقق من جميع بيانات التاجر"))
        }
        if (db.userDao().getUserByEmail(email) != null) {
            return Result.failure(IllegalStateException("البريد الإلكتروني مسجل مسبقاً"))
        }
        if (db.userDao().getUserByWhatsapp(whatsapp) != null) {
            return Result.failure(IllegalStateException("رقم الواتساب مستخدم لحساب آخر"))
        }

        val merchant = UserEntity(
            uid = "merch_${UUID.randomUUID().toString().take(8)}",
            fullName = fullName.trim(),
            email = email,
            passwordHash = PasswordHasher.hashPassword(password),
            whatsapp = whatsapp,
            role = UserRole.MERCHANT.key,
            status = AccountStatus.PENDING_APPROVAL.key,
            balance = 0.0,
            commissionType = commissionType.key,
            commissionValue = commissionValue.coerceAtLeast(0.0)
        )
        db.userDao().insertUser(merchant)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "إنشاء حساب تاجر",
                targetId = merchant.uid,
                details = "إنشاء حساب التاجر ${merchant.fullName} بحالة قيد القبول"
            )
        )
        return Result.success(merchant)
    }

    suspend fun updateUserStatus(adminId: String, targetUid: String, status: AccountStatus): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val target = db.userDao().getUserById(targetUid)
            ?: return Result.failure(IllegalStateException("المستخدم غير موجود"))
        if (target.role == UserRole.ADMIN.key) {
            return Result.failure(IllegalStateException("لا يمكن إغلاق حساب الأدمن الرئيسي"))
        }
        db.userDao().updateUser(target.copy(status = status.key))
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "تغيير حالة حساب",
                targetId = target.uid,
                details = "تغيير حالة ${target.fullName} إلى ${status.arabicLabel}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateMerchantCommission(
        adminId: String,
        merchantId: String,
        commissionType: CommissionType,
        commissionValue: Double
    ): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val merchant = db.userDao().getUserById(merchantId)
            ?: return Result.failure(IllegalStateException("التاجر غير موجود"))
        db.userDao().updateUser(
            merchant.copy(
                commissionType = commissionType.key,
                commissionValue = commissionValue.coerceAtLeast(0.0)
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "تعديل عمولة تاجر",
                targetId = merchant.uid,
                details = "تحديث عمولة ${merchant.fullName}: $commissionValue (${commissionType.arabicLabel})"
            )
        )
        return Result.success(Unit)
    }

    suspend fun adjustCustomerBalance(adminId: String, customerId: String, newBalance: Double): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val customer = db.userDao().getUserById(customerId)
            ?: return Result.failure(IllegalStateException("الزبون غير موجود"))
        db.userDao().updateUser(customer.copy(balance = newBalance.coerceAtLeast(0.0).round2()))
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "تعديل رصيد زبون",
                targetId = customer.uid,
                details = "تعديل رصيد ${customer.fullName} من ${customer.balance} إلى ${newBalance.round2()} د.م"
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateSettings(
        adminId: String,
        agencyPhone: String,
        whatsappNumber: String,
        loyaltyThreshold: Int,
        loyaltyDiscountPercent: Double
    ): Result<Unit> {
        val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
        val current = db.settingsDao().getSettings() ?: SettingsEntity()
        db.settingsDao().upsertSettings(
            current.copy(
                agencyPhone = agencyPhone.trim(),
                whatsappNumber = whatsappNumber.trim().replace("+", "").replace(" ", ""),
                loyaltyThreshold = loyaltyThreshold.coerceIn(1, 20),
                loyaltyDiscountPercent = loyaltyDiscountPercent.coerceIn(1.0, 100.0)
            )
        )
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = "LOG-${UUID.randomUUID().toString().take(8)}",
                actorId = admin.uid,
                actorName = admin.fullName,
                actorRole = "admin",
                actionType = "تحديث الإعدادات",
                targetId = "global",
                details = "تحديث رقم الوكالة ($agencyPhone) ونسبة الولاء ($loyaltyDiscountPercent%)"
            )
        )
        return Result.success(Unit)
    }

    private suspend fun requireAdmin(adminId: String): Result<UserEntity> {
        val user = db.userDao().getUserById(adminId)
        return if (user != null && user.role == UserRole.ADMIN.key && user.status == AccountStatus.ACTIVE.key) {
            Result.success(user)
        } else {
            Result.failure(SecurityException("هذه العملية مخصصة للأدمن فقط"))
        }
    }

    private fun normalizePhone(raw: String): String =
        raw.trim().replace(" ", "").replace("-", "")

    private fun Double.round2(): Double = PricingEngine.round2(this)
}

// [REVIEWED 2026-11] إصلاحات شاملة
