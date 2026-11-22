package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.AuditLogEntity
import com.example.data.local.ContestEntity
import com.example.data.local.ContestEntryEntity
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.OfferEntity
import com.example.data.local.OrderEntity
import com.example.data.local.SeedData
import com.example.data.local.ServiceEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.TopUpEntity
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
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.remote.toArabicFirebaseMessage
import com.example.domain.PricingEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TwinCashFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يرجى تسجيل الدخول بحساب Google أولاً")
    }

    private fun isVerifiedAdminAuth(): Boolean {
        val u = auth.currentUser ?: return false
        return u.email?.trim()?.lowercase() == SeedData.ADMIN_EMAIL.lowercase() && u.isEmailVerified
    }

    private inline fun <T> Flow<T>.guardStream(
        uid: String,
        op: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    // --- Real-Time Streams ---

    fun observeUser(uid: String): Flow<UserEntity?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap -> if (snap.exists()) snap.toUserEntity() else null }
                .guardStream(uid, OperationType.GET, path)
        )
    }

    fun observeSettings(): Flow<SettingsEntity> = flow {
        val uid = requireUserId()
        val path = "settings/global"
        emitAll(
            db.collection("settings").document("global")
                .snapshots()
                .map { snap -> if (snap.exists()) snap.toSettingsEntity() else SettingsEntity() }
                .guardStream(uid, OperationType.GET, path)
        )
    }

    fun observeServices(includeSupplierCosts: Boolean): Flow<List<ServiceEntity>> = flow {
        val uid = requireUserId()
        val servicesStream = db.collection("services")
            .snapshots()
            .map { qs -> qs.documents }
            .guardStream(uid, OperationType.LIST, "services")

        if (includeSupplierCosts && isVerifiedAdminAuth()) {
            val costsStream = db.collection("service_costs")
                .snapshots()
                .map { qs ->
                    qs.documents.associate { doc ->
                        val rawMap = doc.get("costs") as? Map<*, *> ?: emptyMap<Any, Any>()
                        val parsed = rawMap.entries.mapNotNull { (k, v) ->
                            val key = k as? String ?: return@mapNotNull null
                            val num = (v as? Number)?.toDouble() ?: return@mapNotNull null
                            key to num
                        }.toMap()
                        doc.id to parsed
                    }
                }
                .guardStream(uid, OperationType.LIST, "service_costs")

            emitAll(
                combine(servicesStream, costsStream) { docs, costLookup ->
                    val mapped = docs.map { doc ->
                        doc.toServiceEntity(costLookup[doc.id].orEmpty())
                    }.sortedBy { it.displayOrder }
                    mapped.ifEmpty { SeedData.baseServices() }
                }
            )
        } else {
            emitAll(
                servicesStream.map { docs ->
                    val mapped = docs.map { doc ->
                        doc.toServiceEntity(emptyMap())
                    }.sortedBy { it.displayOrder }
                    mapped.ifEmpty { SeedData.baseServices() }
                }
            )
        }
    }

    fun observeOffers(): Flow<List<OfferEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("offers")
                .snapshots()
                .map { qs -> qs.documents.map { it.toOfferEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "offers")
        )
    }

    fun observeContests(): Flow<List<ContestEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("contests")
                .snapshots()
                .map { qs -> qs.documents.map { it.toContestEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "contests")
        )
    }

    fun observeCustomerOrders(uid: String): Flow<List<OrderEntity>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        emitAll(
            db.collection("orders")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { qs -> qs.documents.map { it.toOrderEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "orders")
        )
    }

    fun observeCustomerTopUps(uid: String): Flow<List<TopUpEntity>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        emitAll(
            db.collection("topups")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { qs -> qs.documents.map { it.toTopUpEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "topups")
        )
    }

    fun observeCustomerCodes(uid: String): Flow<List<DiscountCodeEntity>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        val publicFlow = db.collection("discountCodes")
            .whereEqualTo("publicCode", true)
            .snapshots()
            .map { qs -> qs.documents.map { it.toDiscountCodeEntity() } }
            .guardStream(uid, OperationType.LIST, "discountCodes(public)")

        val privateFlow = db.collection("discountCodes")
            .whereEqualTo("targetCustomerId", uid)
            .snapshots()
            .map { qs -> qs.documents.map { it.toDiscountCodeEntity() } }
            .guardStream(uid, OperationType.LIST, "discountCodes(private)")

        emitAll(
            combine(publicFlow, privateFlow) { pub, priv ->
                (pub + priv).distinctBy { it.id }.sortedByDescending { it.createdAt }
            }
        )
    }

    fun observeCustomerContestEntries(uid: String): Flow<List<ContestEntryEntity>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        emitAll(
            db.collection("contestEntries")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { qs -> qs.documents.map { it.toContestEntryEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "contestEntries")
        )
    }

    fun observeMerchantOrders(uid: String): Flow<List<OrderEntity>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        val pendingFlow = db.collection("orders")
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { qs -> qs.documents.map { it.toOrderEntity() } }
            .guardStream(uid, OperationType.LIST, "orders(pending)")

        val myFlow = db.collection("orders")
            .whereEqualTo("merchantId", uid)
            .snapshots()
            .map { qs -> qs.documents.map { it.toOrderEntity() } }
            .guardStream(uid, OperationType.LIST, "orders(merchant)")

        emitAll(
            combine(pendingFlow, myFlow) { pending, mine ->
                (pending + mine).distinctBy { it.id }.sortedByDescending { it.createdAt }
            }
        )
    }

    // --- Admin Streams ---

    fun observeAllUsers(): Flow<List<UserEntity>> = flow {
        val uid = requireUserId()
        val usersFlow = db.collection("users")
            .snapshots()
            .map { qs -> qs.documents.map { it.toUserEntity() } }
            .guardStream(uid, OperationType.LIST, "users")

        val invitesFlow = db.collection("merchant_invites")
            .snapshots()
            .map { qs ->
                qs.documents.mapNotNull { doc ->
                    val claimed = doc.getString("claimedUid")
                    if (!claimed.isNullOrBlank()) null else doc.toUnclaimedMerchantUserEntity()
                }
            }
            .guardStream(uid, OperationType.LIST, "merchant_invites")

        emitAll(
            combine(usersFlow, invitesFlow) { users, invites ->
                val userEmails = users.map { it.email.lowercase() }.toSet()
                val pendingInvites = invites.filter { it.email.lowercase() !in userEmails }
                (users + pendingInvites).sortedByDescending { it.createdAt }
            }
        )
    }

    fun observeAllOrdersForAdmin(): Flow<List<OrderEntity>> = flow {
        val uid = requireUserId()
        val ordersFlow = db.collection("orders")
            .snapshots()
            .map { qs -> qs.documents.map { it.toOrderEntity() } }
            .guardStream(uid, OperationType.LIST, "orders")

        val costsFlow = db.collection("service_costs")
            .snapshots()
            .map { qs ->
                qs.documents.associate { doc ->
                    val rawMap = doc.get("costs") as? Map<*, *> ?: emptyMap<Any, Any>()
                    val parsed = rawMap.entries.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val num = (v as? Number)?.toDouble() ?: return@mapNotNull null
                        key to num
                    }.toMap()
                    doc.id to parsed
                }
            }
            .guardStream(uid, OperationType.LIST, "service_costs")

        emitAll(
            combine(ordersFlow, costsFlow) { orders, costsLookup ->
                orders.map { order ->
                    // [FIX #1.4] قراءة supplierCost المخزن في الطلب أولاً مع دعم الطلبات القديمة
                    val resolvedSupplierCost = when {
                        order.supplierCost > 0.0 -> order.supplierCost
                        order.isOfferOrder -> PricingEngine.round2(order.finalPrice * 0.72)
                        else -> costsLookup[order.serviceId]?.get(order.packageId) ?: 0.0
                    }
                    val resolvedNetProfit = if (order.status == "completed") {
                        PricingEngine.calculateNetProfit(
                            finalPrice = order.finalPrice,
                            supplierCost = resolvedSupplierCost,
                            commission = order.merchantCommission
                        )
                    } else {
                        0.0
                    }
                    order.copy(
                        supplierCost = resolvedSupplierCost,
                        netProfit = resolvedNetProfit
                    )
                }.sortedByDescending { it.createdAt }
            }
        )
    }

    fun observeAllTopUps(): Flow<List<TopUpEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("topups")
                .snapshots()
                .map { qs -> qs.documents.map { it.toTopUpEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "topups")
        )
    }

    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("auditLogs")
                .snapshots()
                .map { qs -> qs.documents.map { it.toAuditLogEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "auditLogs")
        )
    }

    fun observeAllDiscountCodes(): Flow<List<DiscountCodeEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("discountCodes")
                .snapshots()
                .map { qs -> qs.documents.map { it.toDiscountCodeEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "discountCodes")
        )
    }

    fun observeAllContestEntries(): Flow<List<ContestEntryEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("contestEntries")
                .snapshots()
                .map { qs -> qs.documents.map { it.toContestEntryEntity() }.sortedByDescending { it.createdAt } }
                .guardStream(uid, OperationType.LIST, "contestEntries")
        )
    }

    // --- Auth Profile Registration & Bootstrap ---

    suspend fun ensureBaseCatalogSeededForAdmin() {
        if (!isVerifiedAdminAuth()) return
        try {
            val settingsRef = db.collection("settings").document("global")
            val settingsSnap = settingsRef.get().await()
            if (!settingsSnap.exists()) {
                settingsRef.set(
                    mapOf(
                        "agencyPhone" to "0670533324",
                        "whatsappNumber" to "212670533324",
                        "loyaltyThreshold" to 4,
                        "loyaltyDiscountPercent" to 25.0,
                        "adminEmail" to SeedData.ADMIN_EMAIL,
                        "agencyBalance" to 0.0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val existingServices = db.collection("services").limit(1).get().await()
            if (existingServices.isEmpty) {
                val batch = db.batch()
                SeedData.baseServices().forEach { srv ->
                    batch.set(
                        db.collection("services").document(srv.id),
                        mapOf(
                            "category" to srv.category,
                            "name" to srv.name,
                            "description" to srv.description,
                            "packages" to emptyList<Map<String, Any>>(),
                            "available" to srv.isAvailable,
                            "displayOrder" to srv.displayOrder,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        db.collection("service_costs").document(srv.id),
                        mapOf(
                            "costs" to emptyMap<String, Double>(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/services")
        }
    }

    suspend fun checkMerchantInviteStatusIfNoUserDoc(): String? {
        val currentUser = auth.currentUser ?: return null
        val email = currentUser.email?.trim()?.lowercase() ?: return null
        if (email == SeedData.ADMIN_EMAIL.lowercase()) return null
        return try {
            val inviteSnap = db.collection("merchant_invites").document(email).get().await()
            if (!inviteSnap.exists()) return null
            val status = inviteSnap.getString("status") ?: AccountStatus.PENDING_APPROVAL.key
            when (status) {
                AccountStatus.PENDING_APPROVAL.key -> "حساب التاجر قيد المراجعة والقبول من طرف الأدمن"
                AccountStatus.CLOSED.key -> "تم إغلاق هذا الحساب أو حظره من طرف إدارة TwinCash"
                AccountStatus.ACTIVE.key -> {
                    val whatsapp = inviteSnap.getString("whatsapp").orEmpty()
                    val fullName = inviteSnap.getString("fullName").orEmpty()
                    val commissionType = inviteSnap.getString("commissionType") ?: CommissionType.PERCENTAGE.key
                    val commissionValue = (inviteSnap.get("commissionValue") as? Number)?.toDouble() ?: 0.0
                    val batch = db.batch()
                    batch.set(
                        db.collection("whatsapp_index").document(whatsapp),
                        mapOf("uid" to currentUser.uid, "createdAt" to FieldValue.serverTimestamp())
                    )
                    batch.set(
                        db.collection("users").document(currentUser.uid),
                        mapOf(
                            "uid" to currentUser.uid,
                            "fullName" to fullName,
                            "email" to email,
                            "whatsapp" to whatsapp,
                            "role" to UserRole.MERCHANT.key,
                            "status" to AccountStatus.ACTIVE.key,
                            "balance" to 0.0,
                            "loyaltyCount" to 0,
                            "commissionType" to commissionType,
                            "commissionValue" to commissionValue,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(
                        db.collection("merchant_invites").document(email),
                        mapOf("claimedUid" to currentUser.uid, "updatedAt" to FieldValue.serverTimestamp())
                    )
                    batch.commit().await()
                    null
                }
                else -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "merchant_invites/$email")
            null
        }
    }

    suspend fun registerGoogleUserProfile(
        fullNameRaw: String,
        whatsappRaw: String
    ): Result<UserEntity> {
        var docPath = "users/unauthenticated"
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("يرجى تسجيل الدخول بحساب Google أولاً"))
            val uid = firebaseUser.uid
            docPath = "users/$uid"

            val verifiedEmail = firebaseUser.email?.trim()?.lowercase().orEmpty()
            if (verifiedEmail.isBlank()) {
                return Result.failure(IllegalArgumentException("تعذر قراءة البريد الإلكتروني من حساب Google"))
            }

            val name = fullNameRaw.trim().ifBlank { firebaseUser.displayName?.trim().orEmpty() }
            val whatsapp = normalizePhone(whatsappRaw)
            if (name.length < 3) {
                return Result.failure(IllegalArgumentException("الاسم الكامل قصير جداً (3 أحرف على الأقل)"))
            }
            if (whatsapp.length < 8) {
                return Result.failure(IllegalArgumentException("رقم الواتساب إجباري ويجب أن يكون صحيحاً"))
            }

            val existingSnap = db.collection("users").document(uid).get().await()
            if (existingSnap.exists()) {
                return Result.success(existingSnap.toUserEntity())
            }

            val isOwnerAdmin = verifiedEmail == SeedData.ADMIN_EMAIL.lowercase() && firebaseUser.isEmailVerified
            val assignedRole = if (isOwnerAdmin) UserRole.ADMIN.key else UserRole.CUSTOMER.key

            val batch = db.batch()
            batch.set(
                db.collection("whatsapp_index").document(whatsapp),
                mapOf(
                    "uid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("users").document(uid),
                mapOf(
                    "uid" to uid,
                    "fullName" to name,
                    "email" to verifiedEmail,
                    "whatsapp" to whatsapp,
                    "role" to assignedRole,
                    "status" to AccountStatus.ACTIVE.key,
                    "balance" to 0.0,
                    "loyaltyCount" to 0,
                    "commissionType" to CommissionType.PERCENTAGE.key,
                    "commissionValue" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            if (isOwnerAdmin) {
                ensureBaseCatalogSeededForAdmin()
            }

            val saved = db.collection("users").document(uid).get().await()
            Result.success(
                if (saved.exists()) saved.toUserEntity()
                else UserEntity(
                    uid = uid,
                    fullName = name,
                    email = verifiedEmail,
                    passwordHash = "",
                    whatsapp = whatsapp,
                    role = assignedRole,
                    status = AccountStatus.ACTIVE.key,
                    balance = 0.0,
                    loyaltyCount = 0
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "رقم الواتساب مستخدم مسبقاً لحساب آخر أو بيانات التسجيل غير صالحة"
            } else {
                toArabicFirebaseMessage(e)
            }
            Result.failure(Exception(msg, e))
        }
    }

    // --- Profile & Customer Actions ---

    suspend fun updateCustomerProfile(
        uid: String,
        newName: String,
        newWhatsappRaw: String,
        avatarUri: String?
    ): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid || isVerifiedAdminAuth()) { "غير مصرح بتعديل حساب مستخدم آخر" }

            val name = newName.trim()
            val whatsapp = normalizePhone(newWhatsappRaw)
            if (name.length < 3) return Result.failure(IllegalArgumentException("الاسم الكامل غير صالح"))
            if (whatsapp.length < 8) return Result.failure(IllegalArgumentException("رقم الواتساب غير صالح"))

            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("المستخدم غير موجود"))
            val oldWhatsapp = snap.getString("whatsapp").orEmpty()

            val batch = db.batch()
            if (oldWhatsapp != whatsapp) {
                if (oldWhatsapp.isNotBlank()) {
                    batch.delete(db.collection("whatsapp_index").document(oldWhatsapp))
                }
                batch.set(
                    db.collection("whatsapp_index").document(whatsapp),
                    mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp())
                )
            }
            val updates = mutableMapOf<String, Any?>(
                "fullName" to name,
                "whatsapp" to whatsapp,
                "avatarUri" to (avatarUri ?: snap.getString("avatarUri")),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            batch.update(userRef, updates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "رقم الواتساب مستخدم بالفعل لحساب آخر"
            } else {
                toArabicFirebaseMessage(e)
            }
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun validateDiscountCode(
        customerId: String,
        serviceCategory: String,
        rawCode: String,
        basePrice: Double
    ): Result<Pair<DiscountCodeEntity, Double>> {
        return try {
            requireUserId()
            val clean = rawCode.trim().uppercase()
            if (clean.isEmpty()) return Result.failure(IllegalArgumentException("يرجى إدخال كود الخصم"))

            val pubMatches = db.collection("discountCodes")
                .whereEqualTo("publicCode", true)
                .whereEqualTo("code", clean)
                .get()
                .await()
                .documents
                .map { it.toDiscountCodeEntity() }

            val privMatches = db.collection("discountCodes")
                .whereEqualTo("targetCustomerId", customerId)
                .whereEqualTo("code", clean)
                .get()
                .await()
                .documents
                .map { it.toDiscountCodeEntity() }

            val entity = (privMatches + pubMatches).firstOrNull()
                ?: return Result.failure(IllegalArgumentException("كود الخصم غير موجود أو غير متاح لك"))

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
            Result.success(entity to amount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "discountCodes")
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    // [FIX #1.4] قراءة تكلفة المورد الفعلية من service_costs/{serviceId}.costs[packageId] مع حماية صلاحيات الزبون
    private suspend fun resolveServiceSupplierCost(serviceId: String, packageId: String): Double {
        return try {
            val snap = db.collection("service_costs").document(serviceId).get().await()
            if (!snap.exists()) return 0.0
            val rawMap = snap.get("costs") as? Map<*, *> ?: return 0.0
            (rawMap[packageId] as? Number)?.toDouble()?.round2() ?: 0.0
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // في حال كان المتصل زبوناً غير مسموح له بقراءة service_costs (قاعدة الأدمن فقط)
            0.0
        }
    }

    // [FIX #1.2] [FIX #1.4] [FIX #1.5] استخدام معاملة ذرية runTransaction للتحقق من الرصيد وتخزين supplierCost
    suspend fun placeServiceOrder(
        customerId: String,
        serviceId: String,
        packageId: String,
        playerAccountInfo: String,
        note: String,
        discountCodeRaw: String? = null
    ): Result<OrderEntity> {
        var docPath = "orders"
        return try {
            val activeUid = requireUserId()
            check(activeUid == customerId) { "جلسة المستخدم تغيرت" }
            if (playerAccountInfo.trim().isEmpty()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال بيانات الحساب أو المعرف"))
            }

            // قراءة تكلفة المورد قبل المعاملة كي لا تتعطل معاملة الزبون بسبب قيود قراءة service_costs
            val supplierCost = resolveServiceSupplierCost(serviceId, packageId)

            var candidateCodeRef: DocumentReference? = null
            if (!discountCodeRaw.isNullOrBlank()) {
                val cleanCode = discountCodeRaw.trim().uppercase()
                val pubDocs = db.collection("discountCodes")
                    .whereEqualTo("publicCode", true)
                    .whereEqualTo("code", cleanCode)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                val privDocs = db.collection("discountCodes")
                    .whereEqualTo("targetCustomerId", customerId)
                    .whereEqualTo("code", cleanCode)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                candidateCodeRef = (privDocs + pubDocs).firstOrNull()?.reference
                    ?: return Result.failure(IllegalArgumentException("كود الخصم غير موجود أو غير متاح لك"))
            }

            val orderId = "ORD-${(1050..9999).random()}-${UUID.randomUUID().toString().take(4).uppercase()}"
            docPath = "orders/$orderId"

            val customerRef = db.collection("users").document(customerId)
            val serviceRef = db.collection("services").document(serviceId)
            val settingsRef = db.collection("settings").document("global")
            val orderRef = db.collection("orders").document(orderId)

            val createdOrder = db.runTransaction { transaction ->
                // قراءة كل المستندات قبل أي كتابة داخل المعاملة الذرية
                val customerSnap = transaction.get(customerRef)
                if (!customerSnap.exists()) throw IllegalStateException("الحساب غير موجود")
                val customer = customerSnap.toUserEntity()
                if (customer.status != AccountStatus.ACTIVE.key) {
                    throw IllegalStateException("حسابك غير نشط حالياً")
                }

                val serviceSnap = transaction.get(serviceRef)
                if (!serviceSnap.exists()) throw IllegalStateException("الخدمة غير موجودة")
                val service = serviceSnap.toServiceEntity(emptyMap())
                if (!service.isAvailable) {
                    throw IllegalStateException("هذه الخدمة غير متاحة حالياً")
                }

                val packages = PackageJsonCodec.fromJson(service.packagesJson)
                val pkg = packages.firstOrNull { it.id == packageId }
                    ?: throw IllegalStateException("الباقة المختارة غير متوفرة")

                val settingsSnap = transaction.get(settingsRef)
                val settings = if (settingsSnap.exists()) settingsSnap.toSettingsEntity() else SettingsEntity()

                val loyaltyDiscount = PricingEngine.calculateLoyaltyDiscount(
                    pkgPrice = pkg.sellPrice,
                    threshold = settings.loyaltyThreshold,
                    percent = settings.loyaltyDiscountPercent,
                    loyaltyCount = customer.loyaltyCount
                )
                val loyaltyEligible = customer.loyaltyCount >= settings.loyaltyThreshold

                var validatedCode: DiscountCodeEntity? = null
                var codeDiscount = 0.0
                if (candidateCodeRef != null) {
                    val codeSnap = transaction.get(candidateCodeRef)
                    if (!codeSnap.exists()) throw IllegalArgumentException("كود الخصم غير موجود")
                    val codeEntity = codeSnap.toDiscountCodeEntity()
                    val now = System.currentTimeMillis()
                    if (!codeEntity.isActive) throw IllegalStateException("هذا الكود معطل حالياً")
                    if (codeEntity.expiresAt < now) throw IllegalStateException("انتهت صلاحية كود الخصم")
                    if (codeEntity.usedCount >= codeEntity.maxUses) {
                        throw IllegalStateException("تم استنفاد الحد الأقصى لاستعمال هذا الكود")
                    }
                    if (!codeEntity.isPublic && codeEntity.targetCustomerId != customerId) {
                        throw SecurityException("هذا الكود مخصص لزبون آخر ولا يمكنك استخدامه")
                    }
                    if (codeEntity.includedCategoriesCsv != "ALL") {
                        val allowed = codeEntity.includedCategoriesCsv.split(",").map { it.trim() }
                        if (!allowed.contains(service.category)) {
                            throw IllegalArgumentException("هذا الكود لا يشمل هذه الخدمة")
                        }
                    }
                    val codeBase = (pkg.sellPrice - loyaltyDiscount).coerceAtLeast(0.0)
                    codeDiscount = PricingEngine.calculateDiscount(
                        discountType = codeEntity.discountType,
                        discountValue = codeEntity.discountValue,
                        basePrice = codeBase
                    )
                    validatedCode = codeEntity
                }

                val finalPrice = PricingEngine.calculateFinalPrice(
                    pkgPrice = pkg.sellPrice,
                    loyaltyDiscount = loyaltyDiscount,
                    codeDiscount = codeDiscount
                )
                val totalDiscount = (pkg.sellPrice - finalPrice).coerceAtLeast(0.0).round2()

                if (customer.balance < finalPrice) {
                    throw IllegalStateException(
                        "رصيدك غير كافٍ (${customer.balance.round2()} د.م). المبلغ المطلوب: $finalPrice د.م"
                    )
                }

                val updatedLoyalty = if (loyaltyEligible) 0 else customer.loyaltyCount
                val updatedBalance = (customer.balance - finalPrice).round2()

                transaction.update(
                    customerRef,
                    mapOf(
                        "balance" to updatedBalance,
                        "loyaltyCount" to updatedLoyalty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (candidateCodeRef != null && validatedCode != null) {
                    transaction.update(
                        candidateCodeRef,
                        mapOf(
                            "usedCount" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val effectiveCost = if (supplierCost > 0.0) supplierCost else pkg.supplierCost.round2()
                val orderPayload = mapOf<String, Any?>(
                    "customerId" to customer.uid,
                    "customerName" to customer.fullName,
                    "customerWhatsapp" to customer.whatsapp,
                    "serviceId" to service.id,
                    "serviceName" to service.name,
                    "serviceCategory" to service.category,
                    "packageId" to pkg.id,
                    "packageLabel" to pkg.label,
                    "playerAccountInfo" to playerAccountInfo.trim(),
                    "note" to note.trim(),
                    "offerOrder" to false,
                    "discountCodeUsed" to validatedCode?.code,
                    "originalPrice" to pkg.sellPrice,
                    "discountApplied" to totalDiscount,
                    "finalPrice" to finalPrice,
                    "supplierCost" to effectiveCost,
                    "merchantCommission" to 0.0,
                    "netProfit" to 0.0,
                    "status" to "pending",
                    "customerNotified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                transaction.set(orderRef, orderPayload)

                OrderEntity(
                    id = orderId,
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
                    supplierCost = effectiveCost,
                    status = "pending",
                    isCustomerNotified = true
                )
            }.await()

            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            val msg = if (e is IllegalArgumentException || e is IllegalStateException || e is SecurityException) {
                e.message ?: toArabicFirebaseMessage(e)
            } else {
                toArabicFirebaseMessage(e)
            }
            Result.failure(Exception(msg, e))
        }
    }

    // [FIX #1.4] [FIX #1.5] استخدام معاملة ذرية runTransaction في طلبات العروض وحساب supplierCost = (offer.price * 0.72)
    suspend fun placeOfferOrder(
        customerId: String,
        offerId: String,
        playerAccountInfo: String,
        note: String
    ): Result<OrderEntity> {
        var docPath = "orders"
        return try {
            val activeUid = requireUserId()
            check(activeUid == customerId) { "جلسة المستخدم تغيرت" }
            if (playerAccountInfo.trim().isEmpty()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال معرف اللاعب (Player ID)"))
            }

            val orderId = "ORD-${(1050..9999).random()}-${UUID.randomUUID().toString().take(4).uppercase()}"
            docPath = "orders/$orderId"

            val customerRef = db.collection("users").document(customerId)
            val offerRef = db.collection("offers").document(offerId)
            val orderRef = db.collection("orders").document(orderId)

            val createdOrder = db.runTransaction { transaction ->
                val customerSnap = transaction.get(customerRef)
                if (!customerSnap.exists()) throw IllegalStateException("الحساب غير موجود")
                val customer = customerSnap.toUserEntity()
                if (customer.status != AccountStatus.ACTIVE.key) {
                    throw IllegalStateException("حسابك غير نشط حالياً")
                }

                val offerSnap = transaction.get(offerRef)
                if (!offerSnap.exists()) throw IllegalStateException("العرض غير موجود")
                val offer = offerSnap.toOfferEntity()
                if (!offer.isActive || offer.expiresAt < System.currentTimeMillis()) {
                    throw IllegalStateException("هذا العرض منتهي الصلاحية أو غير نشط")
                }
                if (customer.balance < offer.price) {
                    throw IllegalStateException(
                        "رصيدك غير كافٍ (${customer.balance.round2()} د.م). سعر العرض: ${offer.price} د.م"
                    )
                }

                val estimatedSupplierCost = (offer.price * 0.72).round2()
                val discountApplied = (offer.originalPrice - offer.price).coerceAtLeast(0.0).round2()

                transaction.update(
                    customerRef,
                    mapOf(
                        "balance" to (customer.balance - offer.price).round2(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    orderRef,
                    mapOf(
                        "customerId" to customer.uid,
                        "customerName" to customer.fullName,
                        "customerWhatsapp" to customer.whatsapp,
                        "serviceId" to offer.id,
                        "serviceName" to offer.title,
                        "serviceCategory" to ServiceCategory.FREE_FIRE.key,
                        "packageId" to offer.id,
                        "packageLabel" to "عرض Free Fire خاص",
                        "playerAccountInfo" to playerAccountInfo.trim(),
                        "note" to note.trim(),
                        "offerOrder" to true,
                        "originalPrice" to offer.originalPrice,
                        "discountApplied" to discountApplied,
                        "finalPrice" to offer.price,
                        "supplierCost" to estimatedSupplierCost,
                        "merchantCommission" to 0.0,
                        "netProfit" to 0.0,
                        "status" to "pending",
                        "customerNotified" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                OrderEntity(
                    id = orderId,
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
                    discountApplied = discountApplied,
                    finalPrice = offer.price,
                    supplierCost = estimatedSupplierCost,
                    status = "pending",
                    isCustomerNotified = true
                )
            }.await()

            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            val msg = if (e is IllegalArgumentException || e is IllegalStateException) {
                e.message ?: toArabicFirebaseMessage(e)
            } else {
                toArabicFirebaseMessage(e)
            }
            Result.failure(Exception(msg, e))
        }
    }

    // [FIX #1.2] [FIX #1.3] [FIX #1.4] حساب العمولة على grossMargin عبر PricingEngine باستخدام supplierCost المخزّن
    suspend fun acceptAndCompleteOrder(actorId: String, orderId: String): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == actorId)

            val orderRef = db.collection("orders").document(orderId)
            val preOrderSnap = orderRef.get().await()
            if (!preOrderSnap.exists()) return Result.failure(IllegalStateException("الطلب غير موجود"))
            val preOrder = preOrderSnap.toOrderEntity()
            val fallbackAdminCost = if (preOrder.supplierCost <= 0.0 && !preOrder.isOfferOrder && isVerifiedAdminAuth()) {
                resolveServiceSupplierCost(preOrder.serviceId, preOrder.packageId)
            } else 0.0

            val actorRef = db.collection("users").document(actorId)
            val settingsRef = db.collection("settings").document("global")

            db.runTransaction { transaction ->
                val actorSnap = transaction.get(actorRef)
                if (!actorSnap.exists()) throw IllegalStateException("المستخدم غير موجود")
                val actor = actorSnap.toUserEntity()
                if (actor.role != UserRole.ADMIN.key && (actor.role != UserRole.MERCHANT.key || actor.status != AccountStatus.ACTIVE.key)) {
                    throw IllegalStateException("غير مصرح لك بتنفيذ الطلبات")
                }

                val orderSnap = transaction.get(orderRef)
                if (!orderSnap.exists()) throw IllegalStateException("الطلب غير موجود")
                val order = orderSnap.toOrderEntity()
                if (order.status != "pending" && order.status != "accepted") {
                    throw IllegalStateException("تمت معالجة هذا الطلب مسبقاً")
                }

                val settingsSnap = transaction.get(settingsRef)
                val settings = if (settingsSnap.exists()) settingsSnap.toSettingsEntity() else SettingsEntity()

                val needsLoyaltyIncrement = !order.isOfferOrder && order.discountApplied == 0.0
                val customerRef = db.collection("users").document(order.customerId)
                val customerSnap = if (needsLoyaltyIncrement) transaction.get(customerRef) else null

                val resolvedSupplierCost = when {
                    order.supplierCost > 0.0 -> order.supplierCost
                    order.isOfferOrder -> (order.finalPrice * 0.72).round2()
                    else -> fallbackAdminCost
                }
                val commission = if (actor.role == UserRole.MERCHANT.key) {
                    PricingEngine.calculateCommission(
                        finalPrice = order.finalPrice,
                        supplierCost = resolvedSupplierCost,
                        commissionType = actor.commissionType,
                        commissionValue = actor.commissionValue
                    )
                } else 0.0
                val netProfit = PricingEngine.calculateNetProfit(
                    finalPrice = order.finalPrice,
                    supplierCost = resolvedSupplierCost,
                    commission = commission
                )

                transaction.update(
                    orderRef,
                    mapOf(
                        "merchantId" to actor.uid,
                        "merchantName" to actor.fullName,
                        "merchantCommission" to commission,
                        "netProfit" to netProfit,
                        "status" to "completed",
                        "customerNotified" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (actor.role == UserRole.ADMIN.key && settingsSnap.exists()) {
                    transaction.update(
                        settingsRef,
                        mapOf(
                            "agencyBalance" to (settings.agencyBalance + order.finalPrice).round2(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (needsLoyaltyIncrement && customerSnap != null && customerSnap.exists()) {
                    val customer = customerSnap.toUserEntity()
                    val nextLoyalty = (customer.loyaltyCount + 1).coerceAtMost(settings.loyaltyThreshold)
                    transaction.update(
                        customerRef,
                        mapOf(
                            "loyaltyCount" to nextLoyalty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val logId = "LOG-${UUID.randomUUID().toString().take(8)}"
                transaction.set(
                    db.collection("auditLogs").document(logId),
                    mapOf(
                        "actorId" to actor.uid,
                        "actorName" to actor.fullName,
                        "actorRole" to actor.role,
                        "actionType" to "قبول وإتمام طلب",
                        "targetId" to order.id,
                        "details" to "إتمام طلب ${order.serviceName} (${order.packageLabel}) للزبون ${order.customerName}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            val msg = if (e is IllegalStateException) e.message ?: toArabicFirebaseMessage(e) else toArabicFirebaseMessage(e)
            Result.failure(Exception(msg, e))
        }
    }

    // [FIX #1.5] [FIX #1.6] استرجاع الرصيد في معاملة ذرية وإنقاص عداد كود الخصم usedCount بمقدار 1 عند رفض الطلب
    suspend fun rejectOrder(actorId: String, orderId: String, reason: String): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == actorId)

            val cleanReason = reason.trim().ifBlank { "تعذر تنفيذ الطلب، تم استرجاع المبلغ لرصيدك" }
            val orderRef = db.collection("orders").document(orderId)
            val preOrderSnap = orderRef.get().await()
            if (!preOrderSnap.exists()) return Result.failure(IllegalStateException("الطلب غير موجود"))
            val preOrder = preOrderSnap.toOrderEntity()

            val discountCodeRef: DocumentReference? = if (!preOrder.discountCodeUsed.isNullOrBlank()) {
                db.collection("discountCodes")
                    .whereEqualTo("code", preOrder.discountCodeUsed)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.reference
            } else null

            val actorRef = db.collection("users").document(actorId)
            val settingsRef = db.collection("settings").document("global")

            db.runTransaction { transaction ->
                val actorSnap = transaction.get(actorRef)
                if (!actorSnap.exists()) throw IllegalStateException("المستخدم غير موجود")
                val actor = actorSnap.toUserEntity()
                if (actor.role != UserRole.ADMIN.key && (actor.role != UserRole.MERCHANT.key || actor.status != AccountStatus.ACTIVE.key)) {
                    throw IllegalStateException("غير مصرح لك برفض الطلبات")
                }

                val orderSnap = transaction.get(orderRef)
                if (!orderSnap.exists()) throw IllegalStateException("الطلب غير موجود")
                val order = orderSnap.toOrderEntity()
                if (order.status != "pending" && order.status != "accepted") {
                    throw IllegalStateException("تمت معالجة هذا الطلب مسبقاً")
                }

                val settingsSnap = transaction.get(settingsRef)
                val settings = if (settingsSnap.exists()) settingsSnap.toSettingsEntity() else SettingsEntity()

                val customerRef = db.collection("users").document(order.customerId)
                val customerSnap = transaction.get(customerRef)
                val codeSnap = discountCodeRef?.let { transaction.get(it) }

                if (customerSnap.exists()) {
                    val customer = customerSnap.toUserEntity()
                    val restoredLoyalty = if (!order.isOfferOrder && order.discountApplied > 0.0 && order.discountCodeUsed == null) {
                        settings.loyaltyThreshold
                    } else customer.loyaltyCount
                    transaction.update(
                        customerRef,
                        mapOf(
                            "balance" to (customer.balance + order.finalPrice).round2(),
                            "loyaltyCount" to restoredLoyalty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (discountCodeRef != null && codeSnap != null && codeSnap.exists()) {
                    val currentUsed = (codeSnap.get("usedCount") as? Number)?.toInt() ?: 0
                    if (currentUsed > 0) {
                        transaction.update(
                            discountCodeRef,
                            mapOf(
                                "usedCount" to FieldValue.increment(-1),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                transaction.update(
                    orderRef,
                    mapOf(
                        "merchantId" to actor.uid,
                        "merchantName" to actor.fullName,
                        "status" to "rejected",
                        "rejectionReason" to cleanReason,
                        "customerNotified" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val logId = "LOG-${UUID.randomUUID().toString().take(8)}"
                transaction.set(
                    db.collection("auditLogs").document(logId),
                    mapOf(
                        "actorId" to actor.uid,
                        "actorName" to actor.fullName,
                        "actorRole" to actor.role,
                        "actionType" to "رفض طلب واسترجاع رصيد",
                        "targetId" to order.id,
                        "details" to "رفض طلب ${order.id} للزبون ${order.customerName} السبب: $cleanReason",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            val msg = if (e is IllegalStateException) e.message ?: toArabicFirebaseMessage(e) else toArabicFirebaseMessage(e)
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun submitTopUp(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        proofImageUri: String?
    ): Result<TopUpEntity> {
        var docPath = "topups"
        return try {
            val activeUid = requireUserId()
            check(activeUid == customerId)
            if (amount <= 0.0) return Result.failure(IllegalArgumentException("يرجى إدخال مبلغ صحيح أكبر من 0"))
            if (transactionRef.trim().isEmpty() && proofImageUri == null) {
                return Result.failure(IllegalArgumentException("يرجى إدخال رقم العملية أو إرفاق صورة الإثبات"))
            }

            val customerSnap = db.collection("users").document(customerId).get().await()
            if (!customerSnap.exists()) return Result.failure(IllegalStateException("المستخدم غير موجود"))
            val customer = customerSnap.toUserEntity()

            val topUpId = "TOP-${(1000..9999).random()}-${UUID.randomUUID().toString().take(4).uppercase()}"
            docPath = "topups/$topUpId"

            val payload = mapOf<String, Any?>(
                "customerId" to customer.uid,
                "customerName" to customer.fullName,
                "customerWhatsapp" to customer.whatsapp,
                "amount" to amount.round2(),
                "paymentMethod" to paymentMethod.trim(),
                "transactionRef" to transactionRef.trim().ifBlank { "مرفق صورة" },
                "proofImageUri" to proofImageUri,
                "status" to "pending",
                "customerNotified" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("topups").document(topUpId).set(payload).await()
            Result.success(
                TopUpEntity(
                    id = topUpId,
                    customerId = customer.uid,
                    customerName = customer.fullName,
                    customerWhatsapp = customer.whatsapp,
                    amount = amount.round2(),
                    paymentMethod = paymentMethod.trim(),
                    transactionRef = transactionRef.trim().ifBlank { "مرفق صورة" },
                    proofImageUri = proofImageUri,
                    status = "pending",
                    isCustomerNotified = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    // [FIX #1.5] استخدام معاملة ذرية runTransaction عند قبول تعبئة الرصيد لمنع سباق التعديل
    suspend fun approveTopUp(adminId: String, topUpId: String): Result<Unit> {
        val docPath = "topups/$topUpId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val topUpRef = db.collection("topups").document(topUpId)
            val preTopUpSnap = topUpRef.get().await()
            if (!preTopUpSnap.exists()) return Result.failure(IllegalStateException("طلب الشحن غير موجود"))
            val preTopUp = preTopUpSnap.toTopUpEntity()
            if (preTopUp.status != "pending") return Result.failure(IllegalStateException("تمت معالجة الطلب مسبقاً"))

            val customerRef = db.collection("users").document(preTopUp.customerId)
            val settingsRef = db.collection("settings").document("global")

            db.runTransaction { transaction ->
                val topUpSnap = transaction.get(topUpRef)
                if (!topUpSnap.exists()) throw IllegalStateException("طلب الشحن غير موجود")
                val topUp = topUpSnap.toTopUpEntity()
                if (topUp.status != "pending") throw IllegalStateException("تمت معالجة الطلب مسبقاً")

                val customerSnap = transaction.get(customerRef)
                if (!customerSnap.exists()) throw IllegalStateException("الزبون غير موجود")
                val customer = customerSnap.toUserEntity()

                val settingsSnap = transaction.get(settingsRef)
                val settings = if (settingsSnap.exists()) settingsSnap.toSettingsEntity() else SettingsEntity()

                transaction.update(
                    customerRef,
                    mapOf(
                        "balance" to (customer.balance + topUp.amount).round2(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    topUpRef,
                    mapOf(
                        "status" to "accepted",
                        "reviewedBy" to admin.fullName,
                        "customerNotified" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    settingsRef,
                    mapOf(
                        "agencyPhone" to settings.agencyPhone,
                        "whatsappNumber" to settings.whatsappNumber,
                        "loyaltyThreshold" to settings.loyaltyThreshold,
                        "loyaltyDiscountPercent" to settings.loyaltyDiscountPercent,
                        "adminEmail" to SeedData.ADMIN_EMAIL,
                        "agencyBalance" to (settings.agencyBalance + topUp.amount).round2(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                    mapOf(
                        "actorId" to admin.uid,
                        "actorName" to admin.fullName,
                        "actorRole" to "admin",
                        "actionType" to "قبول تعبئة رصيد",
                        "targetId" to topUp.id,
                        "details" to "إضافة ${topUp.amount} د.م لرصيد ${customer.fullName}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            val msg = if (e is IllegalStateException) e.message ?: toArabicFirebaseMessage(e) else toArabicFirebaseMessage(e)
            Result.failure(Exception(msg, e))
        }
    }

    suspend fun rejectTopUp(adminId: String, topUpId: String, reason: String): Result<Unit> {
        val docPath = "topups/$topUpId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val topUpRef = db.collection("topups").document(topUpId)
            val topUpSnap = topUpRef.get().await()
            if (!topUpSnap.exists()) return Result.failure(IllegalStateException("طلب الشحن غير موجود"))
            val topUp = topUpSnap.toTopUpEntity()
            if (topUp.status != "pending") return Result.failure(IllegalStateException("تمت معالجة الطلب مسبقاً"))

            val cleanReason = reason.trim().ifBlank { "تعذر التحقق من وصل التحويل" }
            val batch = db.batch()
            batch.update(
                topUpRef,
                mapOf(
                    "status" to "rejected",
                    "rejectionReason" to cleanReason,
                    "reviewedBy" to admin.fullName,
                    "customerNotified" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "رفض تعبئة رصيد",
                    "targetId" to topUp.id,
                    "details" to "رفض تعبئة ${topUp.amount} د.م للزبون ${topUp.customerName}: $cleanReason",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun markNotificationsRead(customerId: String) {
        try {
            val activeUid = requireUserId()
            if (activeUid != customerId) return

            val unreadOrders = db.collection("orders")
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("customerNotified", false)
                .get()
                .await()
            val unreadTopUps = db.collection("topups")
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("customerNotified", false)
                .get()
                .await()
            val unreadContests = db.collection("contests")
                .whereEqualTo("winnerCustomerId", customerId)
                .whereEqualTo("winnerNotified", false)
                .get()
                .await()

            if (unreadOrders.isEmpty && unreadTopUps.isEmpty && unreadContests.isEmpty) return

            val batch = db.batch()
            unreadOrders.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf("customerNotified" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            unreadTopUps.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf("customerNotified" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            unreadContests.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf("winnerNotified" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$customerId")
        }
    }

    // --- Contests ---

    suspend fun joinContest(customerId: String, contestId: String): Result<Unit> {
        val entryId = "${contestId}_${customerId}"
        val docPath = "contestEntries/$entryId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == customerId)

            val customerSnap = db.collection("users").document(customerId).get().await()
            if (!customerSnap.exists()) return Result.failure(IllegalStateException("الحساب غير موجود"))
            val customer = customerSnap.toUserEntity()
            if (customer.role != UserRole.CUSTOMER.key || customer.status != AccountStatus.ACTIVE.key) {
                return Result.failure(IllegalStateException("المشاركة متاحة للزبائن النشطين فقط"))
            }

            val contestSnap = db.collection("contests").document(contestId).get().await()
            if (!contestSnap.exists()) return Result.failure(IllegalStateException("المسابقة غير موجودة"))
            val contest = contestSnap.toContestEntity()
            val now = System.currentTimeMillis()
            if (contest.status != ContestStatus.ACTIVE.key || now < contest.startsAt || now > contest.endsAt) {
                return Result.failure(IllegalStateException("هذه المسابقة غير متاحة للمشاركة حالياً"))
            }

            val existing = db.collection("contestEntries").document(entryId).get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("لقد شاركت مسبقاً في هذه المسابقة"))
            }

            db.collection("contestEntries").document(entryId).set(
                mapOf(
                    "contestId" to contestId,
                    "customerId" to customer.uid,
                    "customerName" to customer.fullName,
                    "customerWhatsapp" to customer.whatsapp,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
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
        val docId = id ?: "cnt_${UUID.randomUUID().toString().take(8)}"
        val docPath = "contests/$docId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            if (title.isBlank() || prizeLabel.isBlank()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال عنوان المسابقة والجائزة"))
            }

            val now = System.currentTimeMillis()
            val existingSnap = if (id != null) db.collection("contests").document(id).get().await() else null
            val existing = if (existingSnap?.exists() == true) existingSnap.toContestEntity() else null

            val batch = db.batch()
            val contestRef = db.collection("contests").document(docId)
            if (existing == null) {
                val payload = mapOf<String, Any?>(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "prizeType" to prizeType.key,
                    "prizeLabel" to prizeLabel.trim(),
                    "prizeAmount" to prizeAmount.coerceAtLeast(0.0).round2(),
                    "rulesText" to rulesText.trim().ifBlank { "مشاركة واحدة لكل زبون." },
                    "startsAt" to now,
                    "endsAt" to now + daysDuration.coerceAtLeast(1) * 24L * 60L * 60L * 1000L,
                    "status" to ContestStatus.ACTIVE.key,
                    "prizeApproved" to false,
                    "winnerNotified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.set(contestRef, payload)
            } else {
                val updates = mapOf<String, Any?>(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "prizeType" to prizeType.key,
                    "prizeLabel" to prizeLabel.trim(),
                    "prizeAmount" to prizeAmount.coerceAtLeast(0.0).round2(),
                    "rulesText" to rulesText.trim().ifBlank { "مشاركة واحدة لكل زبون." },
                    "endsAt" to now + daysDuration.coerceAtLeast(1) * 24L * 60L * 60L * 1000L,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.update(contestRef, updates)
            }
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to if (id == null) "إنشاء مسابقة" else "تعديل مسابقة",
                    "targetId" to docId,
                    "details" to "حفظ مسابقة: ${title.trim()}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun selectContestWinner(
        adminId: String,
        contestId: String,
        specificCustomerId: String?
    ): Result<String> {
        val docPath = "contests/$contestId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val contestRef = db.collection("contests").document(contestId)
            val contestSnap = contestRef.get().await()
            if (!contestSnap.exists()) return Result.failure(IllegalStateException("المسابقة غير موجودة"))
            val contest = contestSnap.toContestEntity()

            val entries = db.collection("contestEntries")
                .whereEqualTo("contestId", contestId)
                .get()
                .await()
                .documents
                .map { it.toContestEntryEntity() }

            if (entries.isEmpty()) {
                return Result.failure(IllegalStateException("لا يوجد مشاركون في هذه المسابقة بعد"))
            }
            val winnerEntry = if (specificCustomerId != null) {
                entries.firstOrNull { it.customerId == specificCustomerId }
                    ?: return Result.failure(IllegalStateException("المشارك المختار غير موجود"))
            } else {
                entries.random()
            }

            val batch = db.batch()
            batch.update(
                contestRef,
                mapOf(
                    "status" to ContestStatus.WINNER_SELECTED.key,
                    "winnerCustomerId" to winnerEntry.customerId,
                    "winnerCustomerName" to winnerEntry.customerName,
                    "prizeApproved" to false,
                    "winnerNotified" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "اختيار فائز بمسابقة",
                    "targetId" to contestId,
                    "details" to "اختيار الفائز ${winnerEntry.customerName} في مسابقة ${contest.title} (بانتظار اعتماد الجائزة)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(winnerEntry.customerName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun approveContestPrize(adminId: String, contestId: String): Result<Unit> {
        val docPath = "contests/$contestId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val contestRef = db.collection("contests").document(contestId)
            val contestSnap = contestRef.get().await()
            if (!contestSnap.exists()) return Result.failure(IllegalStateException("المسابقة غير موجودة"))
            val contest = contestSnap.toContestEntity()
            val winnerId = contest.winnerCustomerId
                ?: return Result.failure(IllegalStateException("لم يتم اختيار فائز بعد"))
            if (contest.isPrizeApproved) {
                return Result.failure(IllegalStateException("تم اعتماد الجائزة ومنحها مسبقاً"))
            }

            val winnerRef = db.collection("users").document(winnerId)
            val winnerSnap = winnerRef.get().await()
            if (!winnerSnap.exists()) return Result.failure(IllegalStateException("حساب الفائز غير موجود"))
            val winner = winnerSnap.toUserEntity()

            val batch = db.batch()
            if (contest.prizeType == ContestPrizeType.BALANCE.key && contest.prizeAmount > 0.0) {
                batch.update(
                    winnerRef,
                    mapOf(
                        "balance" to (winner.balance + contest.prizeAmount).round2(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (contest.prizeType == ContestPrizeType.DISCOUNT_CODE.key) {
                val winCode = "WIN${(100..999).random()}"
                val dcId = "dc_${UUID.randomUUID().toString().take(8)}"
                batch.set(
                    db.collection("discountCodes").document(dcId),
                    mapOf(
                        "code" to winCode,
                        "discountType" to DiscountType.FIXED.key,
                        "discountValue" to contest.prizeAmount.coerceAtLeast(25.0),
                        "includedCategories" to listOf("ALL"),
                        "expiresAt" to (System.currentTimeMillis() + 30L * 24 * 60 * 60_000L),
                        "maxUses" to 1,
                        "usedCount" to 0,
                        "publicCode" to false,
                        "targetCustomerId" to winner.uid,
                        "targetCustomerName" to winner.fullName,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.update(
                contestRef,
                mapOf(
                    "prizeApproved" to true,
                    "winnerNotified" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "اعتماد جائزة مسابقة",
                    "targetId" to contestId,
                    "details" to "اعتماد ومنح جائزة (${contest.prizeLabel}) للفائز ${winner.fullName}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun deleteContest(adminId: String, contestId: String): Result<Unit> {
        val docPath = "contests/$contestId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val batch = db.batch()
            batch.delete(db.collection("contests").document(contestId))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "حذف مسابقة",
                    "targetId" to contestId,
                    "details" to "حذف المسابقة $contestId",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    // --- Admin Discount Codes ---

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
        val docId = id ?: "dc_${UUID.randomUUID().toString().take(8)}"
        val docPath = "discountCodes/$docId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val cleanCode = codeRaw.trim().uppercase().replace(" ", "")
            if (cleanCode.length < 3 || discountValue <= 0.0 || maxUses <= 0) {
                return Result.failure(IllegalArgumentException("يرجى إدخال كود صحيح وقيمة خصم وعدد استعمالات موجب"))
            }

            val duplicateQuery = db.collection("discountCodes").whereEqualTo("code", cleanCode).get().await()
            if (duplicateQuery.documents.any { it.id != id }) {
                return Result.failure(IllegalStateException("هذا الكود موجود مسبقاً"))
            }

            val targetUser = if (!isPublic && !targetCustomerId.isNullOrBlank()) {
                val snap = db.collection("users").document(targetCustomerId).get().await()
                if (snap.exists()) snap.toUserEntity() else null
            } else null

            val categoriesList = includedCategoriesCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("ALL") }

            val existingSnap = if (id != null) db.collection("discountCodes").document(id).get().await() else null
            val ref = db.collection("discountCodes").document(docId)
            val batch = db.batch()

            if (existingSnap?.exists() == true) {
                val updates = mutableMapOf<String, Any?>(
                    "code" to cleanCode,
                    "discountType" to discountType.key,
                    "discountValue" to discountValue.round2(),
                    "includedCategories" to categoriesList,
                    "expiresAt" to (System.currentTimeMillis() + daysValid.coerceAtLeast(1) * 24L * 60 * 60_000L),
                    "maxUses" to maxUses,
                    "publicCode" to isPublic,
                    "targetCustomerId" to targetUser?.uid,
                    "targetCustomerName" to targetUser?.fullName,
                    "active" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.update(ref, updates)
            } else {
                val payload = mapOf<String, Any?>(
                    "code" to cleanCode,
                    "discountType" to discountType.key,
                    "discountValue" to discountValue.round2(),
                    "includedCategories" to categoriesList,
                    "expiresAt" to (System.currentTimeMillis() + daysValid.coerceAtLeast(1) * 24L * 60 * 60_000L),
                    "maxUses" to maxUses,
                    "usedCount" to 0,
                    "publicCode" to isPublic,
                    "targetCustomerId" to targetUser?.uid,
                    "targetCustomerName" to targetUser?.fullName,
                    "active" to isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.set(ref, payload)
            }

            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to if (id == null) "إنشاء كود خصم" else "تعديل كود خصم",
                    "targetId" to docId,
                    "details" to "حفظ كود $cleanCode (${if (isPublic) "عام" else "لـ ${targetUser?.fullName}"})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun toggleDiscountCode(adminId: String, codeId: String, active: Boolean): Result<Unit> {
        val docPath = "discountCodes/$codeId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val ref = db.collection("discountCodes").document(codeId)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("الكود غير موجود"))
            val codeName = snap.getString("code").orEmpty()

            val batch = db.batch()
            batch.update(ref, mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "تغيير حالة كود خصم",
                    "targetId" to codeId,
                    "details" to "${if (active) "تفعيل" else "تعطيل"} الكود $codeName",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun deleteDiscountCode(adminId: String, codeId: String): Result<Unit> {
        val docPath = "discountCodes/$codeId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val batch = db.batch()
            batch.delete(db.collection("discountCodes").document(codeId))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "حذف كود خصم",
                    "targetId" to codeId,
                    "details" to "حذف كود الخصم $codeId",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
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
        val docId = id ?: "srv_${UUID.randomUUID().toString().take(8)}"
        val docPath = "services/$docId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            if (name.isBlank() || packages.isEmpty()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال اسم الخدمة وباقة واحدة على الأقل"))
            }

            val publicPackages = packages.map { pkg ->
                mapOf(
                    "id" to pkg.id,
                    "label" to pkg.label,
                    "quantityLabel" to pkg.quantityLabel,
                    "sellPrice" to pkg.sellPrice.round2()
                )
            }
            val costsMap = packages.associate { pkg -> pkg.id to pkg.supplierCost.round2() }

            val serviceRef = db.collection("services").document(docId)
            val existing = serviceRef.get().await()
            val batch = db.batch()
            if (existing.exists()) {
                batch.update(
                    serviceRef,
                    mapOf(
                        "category" to category.key,
                        "name" to name.trim(),
                        "description" to description.trim(),
                        "packages" to publicPackages,
                        "available" to isAvailable,
                        "displayOrder" to displayOrder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    serviceRef,
                    mapOf(
                        "category" to category.key,
                        "name" to name.trim(),
                        "description" to description.trim(),
                        "packages" to publicPackages,
                        "available" to isAvailable,
                        "displayOrder" to displayOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                db.collection("service_costs").document(docId),
                mapOf(
                    "costs" to costsMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to if (id == null) "إضافة خدمة" else "تعديل خدمة",
                    "targetId" to docId,
                    "details" to "تحديث خدمة ${name.trim()} (الترتيب: $displayOrder)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun toggleServiceAvailability(adminId: String, serviceId: String, isAvailable: Boolean): Result<Unit> {
        val docPath = "services/$serviceId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val ref = db.collection("services").document(serviceId)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("الخدمة غير موجودة"))
            val name = snap.getString("name").orEmpty()

            val batch = db.batch()
            batch.update(ref, mapOf("available" to isAvailable, "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to if (isAvailable) "إظهار خدمة" else "إخفاء خدمة مؤقتاً",
                    "targetId" to serviceId,
                    "details" to "${if (isAvailable) "تفعيل" else "إخفاء"} خدمة $name",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun deleteService(adminId: String, serviceId: String): Result<Unit> {
        val docPath = "services/$serviceId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val batch = db.batch()
            batch.delete(db.collection("services").document(serviceId))
            batch.delete(db.collection("service_costs").document(serviceId))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "حذف خدمة",
                    "targetId" to serviceId,
                    "details" to "حذف الخدمة $serviceId",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
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
        val docId = id ?: "off_${UUID.randomUUID().toString().take(8)}"
        val docPath = "offers/$docId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            if (title.isBlank() || price <= 0.0) {
                return Result.failure(IllegalArgumentException("يرجى إدخال عنوان العرض وسعر صحيح"))
            }
            val expiresAt = System.currentTimeMillis() + (daysValid.coerceAtLeast(1) * 24L * 60L * 60L * 1000L)
            val ref = db.collection("offers").document(docId)
            val existing = ref.get().await()
            val batch = db.batch()
            if (existing.exists()) {
                batch.update(
                    ref,
                    mapOf(
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "price" to price.round2(),
                        "originalPrice" to originalPrice.coerceAtLeast(price).round2(),
                        "serviceCategory" to ServiceCategory.FREE_FIRE.key,
                        "expiresAt" to expiresAt,
                        "active" to isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    ref,
                    mapOf(
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "price" to price.round2(),
                        "originalPrice" to originalPrice.coerceAtLeast(price).round2(),
                        "serviceCategory" to ServiceCategory.FREE_FIRE.key,
                        "expiresAt" to expiresAt,
                        "active" to isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to if (id == null) "إضافة عرض Free Fire" else "تعديل عرض Free Fire",
                    "targetId" to docId,
                    "details" to "تحديث عرض ${title.trim()} بسعر ${price.round2()} د.م",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun deleteOffer(adminId: String, offerId: String): Result<Unit> {
        val docPath = "offers/$offerId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val batch = db.batch()
            batch.delete(db.collection("offers").document(offerId))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "حذف عرض",
                    "targetId" to offerId,
                    "details" to "حذف عرض Free Fire ($offerId)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun createMerchant(
        adminId: String,
        fullName: String,
        emailRaw: String,
        whatsappRaw: String,
        commissionType: CommissionType,
        commissionValue: Double
    ): Result<UserEntity> {
        val email = emailRaw.trim().lowercase()
        val docPath = "merchant_invites/$email"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val whatsapp = normalizePhone(whatsappRaw)
            if (fullName.trim().length < 3 || !email.contains("@") || whatsapp.length < 8) {
                return Result.failure(IllegalArgumentException("يرجى التحقق من اسم التاجر وبريد Google ورقم الواتساب"))
            }
            if (email == SeedData.ADMIN_EMAIL.lowercase()) {
                return Result.failure(IllegalArgumentException("لا يمكن استخدام بريد الأدمن كحساب تاجر"))
            }

            val existingInvite = db.collection("merchant_invites").document(email).get().await()
            if (existingInvite.exists()) {
                return Result.failure(IllegalStateException("البريد الإلكتروني مسجل مسبقاً كتاجر"))
            }
            val existingUsers = db.collection("users").whereEqualTo("email", email).get().await()
            if (!existingUsers.isEmpty) {
                return Result.failure(IllegalStateException("هذا البريد مسجل بالفعل لحساب آخر"))
            }

            val batch = db.batch()
            batch.set(
                db.collection("merchant_invites").document(email),
                mapOf(
                    "email" to email,
                    "fullName" to fullName.trim(),
                    "whatsapp" to whatsapp,
                    "role" to UserRole.MERCHANT.key,
                    "status" to AccountStatus.PENDING_APPROVAL.key,
                    "commissionType" to commissionType.key,
                    "commissionValue" to commissionValue.coerceAtLeast(0.0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "إنشاء دعوة تاجر",
                    "targetId" to email,
                    "details" to "إنشاء التاجر ${fullName.trim()} ($email) بحالة قيد القبول",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(
                UserEntity(
                    uid = "invite_$email",
                    fullName = fullName.trim(),
                    email = email,
                    passwordHash = "",
                    whatsapp = whatsapp,
                    role = UserRole.MERCHANT.key,
                    status = AccountStatus.PENDING_APPROVAL.key,
                    balance = 0.0,
                    commissionType = commissionType.key,
                    commissionValue = commissionValue.coerceAtLeast(0.0)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun updateUserStatus(adminId: String, targetUid: String, status: AccountStatus): Result<Unit> {
        val docPath = "users/$targetUid"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val batch = db.batch()
            val targetName: String

            if (targetUid.startsWith("invite_")) {
                val email = targetUid.removePrefix("invite_")
                val inviteRef = db.collection("merchant_invites").document(email)
                val inviteSnap = inviteRef.get().await()
                if (!inviteSnap.exists()) return Result.failure(IllegalStateException("دعوة التاجر غير موجودة"))
                targetName = inviteSnap.getString("fullName") ?: email
                batch.update(
                    inviteRef,
                    mapOf("status" to status.key, "updatedAt" to FieldValue.serverTimestamp())
                )
            } else {
                val userRef = db.collection("users").document(targetUid)
                val userSnap = userRef.get().await()
                if (!userSnap.exists()) return Result.failure(IllegalStateException("المستخدم غير موجود"))
                val target = userSnap.toUserEntity()
                if (target.role == UserRole.ADMIN.key) {
                    return Result.failure(IllegalStateException("لا يمكن إغلاق حساب الأدمن الرئيسي"))
                }
                targetName = target.fullName
                batch.update(userRef, mapOf("status" to status.key, "updatedAt" to FieldValue.serverTimestamp()))
                if (target.role == UserRole.MERCHANT.key) {
                    val inviteRef = db.collection("merchant_invites").document(target.email.lowercase())
                    val inviteSnap = inviteRef.get().await()
                    if (inviteSnap.exists()) {
                        batch.update(inviteRef, mapOf("status" to status.key, "updatedAt" to FieldValue.serverTimestamp()))
                    }
                }
            }

            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "تغيير حالة حساب",
                    "targetId" to targetUid,
                    "details" to "تغيير حالة $targetName إلى ${status.arabicLabel}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun updateMerchantCommission(
        adminId: String,
        merchantId: String,
        commissionType: CommissionType,
        commissionValue: Double
    ): Result<Unit> {
        val docPath = "users/$merchantId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val safeValue = commissionValue.coerceAtLeast(0.0)
            val batch = db.batch()
            val merchantName: String

            if (merchantId.startsWith("invite_")) {
                val email = merchantId.removePrefix("invite_")
                val inviteRef = db.collection("merchant_invites").document(email)
                val snap = inviteRef.get().await()
                if (!snap.exists()) return Result.failure(IllegalStateException("التاجر غير موجود"))
                merchantName = snap.getString("fullName") ?: email
                batch.update(
                    inviteRef,
                    mapOf(
                        "commissionType" to commissionType.key,
                        "commissionValue" to safeValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val userRef = db.collection("users").document(merchantId)
                val snap = userRef.get().await()
                if (!snap.exists()) return Result.failure(IllegalStateException("التاجر غير موجود"))
                val target = snap.toUserEntity()
                merchantName = target.fullName
                batch.update(
                    userRef,
                    mapOf(
                        "commissionType" to commissionType.key,
                        "commissionValue" to safeValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val inviteRef = db.collection("merchant_invites").document(target.email.lowercase())
                val inviteSnap = inviteRef.get().await()
                if (inviteSnap.exists()) {
                    batch.update(
                        inviteRef,
                        mapOf(
                            "commissionType" to commissionType.key,
                            "commissionValue" to safeValue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "تعديل عمولة تاجر",
                    "targetId" to merchantId,
                    "details" to "تحديث عمولة $merchantName: $safeValue (${commissionType.arabicLabel})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun adjustCustomerBalance(adminId: String, customerId: String, newBalance: Double): Result<Unit> {
        val docPath = "users/$customerId"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val ref = db.collection("users").document(customerId)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("الزبون غير موجود"))
            val customer = snap.toUserEntity()
            val updated = newBalance.coerceAtLeast(0.0).round2()

            val batch = db.batch()
            batch.update(ref, mapOf("balance" to updated, "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "تعديل رصيد زبون",
                    "targetId" to customer.uid,
                    "details" to "تعديل رصيد ${customer.fullName} من ${customer.balance} إلى $updated د.م",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    suspend fun updateSettings(
        adminId: String,
        agencyPhone: String,
        whatsappNumber: String,
        loyaltyThreshold: Int,
        loyaltyDiscountPercent: Double
    ): Result<Unit> {
        val docPath = "settings/global"
        return try {
            val admin = requireAdmin(adminId).getOrElse { return Result.failure(it) }
            val ref = db.collection("settings").document("global")
            val snap = ref.get().await()
            val current = if (snap.exists()) snap.toSettingsEntity() else SettingsEntity()

            val batch = db.batch()
            batch.set(
                ref,
                mapOf(
                    "agencyPhone" to agencyPhone.trim(),
                    "whatsappNumber" to whatsappNumber.trim().replace("+", "").replace(" ", ""),
                    "loyaltyThreshold" to loyaltyThreshold.coerceIn(1, 20),
                    "loyaltyDiscountPercent" to loyaltyDiscountPercent.coerceIn(1.0, 100.0),
                    "adminEmail" to SeedData.ADMIN_EMAIL,
                    "agencyBalance" to current.agencyBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("auditLogs").document("LOG-${UUID.randomUUID().toString().take(8)}"),
                mapOf(
                    "actorId" to admin.uid,
                    "actorName" to admin.fullName,
                    "actorRole" to "admin",
                    "actionType" to "تحديث الإعدادات",
                    "targetId" to "global",
                    "details" to "تحديث رقم الوكالة ($agencyPhone) ونسبة الولاء ($loyaltyDiscountPercent%)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(Exception(toArabicFirebaseMessage(e), e))
        }
    }

    private suspend fun requireAdmin(adminId: String): Result<UserEntity> {
        val activeUid = auth.currentUser?.uid
        if (activeUid.isNullOrBlank() || activeUid != adminId || !isVerifiedAdminAuth()) {
            return Result.failure(SecurityException("هذه العملية مخصصة للأدمن الرئيسي فقط"))
        }
        val snap = db.collection("users").document(adminId).get().await()
        if (!snap.exists()) return Result.failure(SecurityException("حساب الأدمن غير مكتمل"))
        val user = snap.toUserEntity()
        return if (user.role == UserRole.ADMIN.key && user.status == AccountStatus.ACTIVE.key) {
            Result.success(user)
        } else {
            Result.failure(SecurityException("هذه العملية مخصصة للأدمن فقط"))
        }
    }

    // --- Snapshot Deserialization Helpers (Safe Number & Timestamp Coercion) ---

    private fun DocumentSnapshot.tsMillis(field: String): Long =
        getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()

    private fun DocumentSnapshot.toUserEntity(): UserEntity = UserEntity(
        uid = getString("uid") ?: id,
        fullName = getString("fullName").orEmpty(),
        email = getString("email").orEmpty(),
        passwordHash = "",
        whatsapp = getString("whatsapp").orEmpty(),
        role = getString("role") ?: UserRole.CUSTOMER.key,
        status = getString("status") ?: AccountStatus.ACTIVE.key,
        balance = (get("balance") as? Number)?.toDouble() ?: 0.0,
        loyaltyCount = (get("loyaltyCount") as? Number)?.toInt() ?: 0,
        commissionType = getString("commissionType") ?: CommissionType.PERCENTAGE.key,
        commissionValue = (get("commissionValue") as? Number)?.toDouble() ?: 0.0,
        avatarUri = getString("avatarUri"),
        createdAt = tsMillis("createdAt")
    )

    private fun DocumentSnapshot.toUnclaimedMerchantUserEntity(): UserEntity = UserEntity(
        uid = "invite_${id}",
        fullName = getString("fullName").orEmpty(),
        email = getString("email") ?: id,
        passwordHash = "",
        whatsapp = getString("whatsapp").orEmpty(),
        role = UserRole.MERCHANT.key,
        status = getString("status") ?: AccountStatus.PENDING_APPROVAL.key,
        balance = 0.0,
        loyaltyCount = 0,
        commissionType = getString("commissionType") ?: CommissionType.PERCENTAGE.key,
        commissionValue = (get("commissionValue") as? Number)?.toDouble() ?: 0.0,
        avatarUri = null,
        createdAt = tsMillis("createdAt")
    )

    private fun DocumentSnapshot.toServiceEntity(costsMap: Map<String, Double>): ServiceEntity {
        val rawList = get("packages") as? List<*> ?: emptyList<Any>()
        val packages = rawList.mapIndexedNotNull { idx, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val pkgId = m["id"] as? String ?: "pkg_$idx"
            ServicePackage(
                id = pkgId,
                label = m["label"] as? String ?: "",
                quantityLabel = m["quantityLabel"] as? String ?: "",
                supplierCost = costsMap[pkgId] ?: 0.0,
                sellPrice = (m["sellPrice"] as? Number)?.toDouble() ?: 0.0
            )
        }
        return ServiceEntity(
            id = id,
            category = getString("category") ?: ServiceCategory.FREE_FIRE.key,
            name = getString("name").orEmpty(),
            description = getString("description").orEmpty(),
            packagesJson = PackageJsonCodec.toJson(packages),
            isAvailable = getBoolean("available") ?: true,
            displayOrder = (get("displayOrder") as? Number)?.toInt() ?: 0,
            createdAt = tsMillis("createdAt")
        )
    }

    private fun DocumentSnapshot.toOfferEntity(): OfferEntity = OfferEntity(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        price = (get("price") as? Number)?.toDouble() ?: 0.0,
        originalPrice = (get("originalPrice") as? Number)?.toDouble() ?: 0.0,
        serviceCategory = getString("serviceCategory") ?: ServiceCategory.FREE_FIRE.key,
        expiresAt = (get("expiresAt") as? Number)?.toLong() ?: 0L,
        isActive = getBoolean("active") ?: true,
        createdAt = tsMillis("createdAt")
    )

    private fun DocumentSnapshot.toDiscountCodeEntity(): DiscountCodeEntity {
        val cats = (get("includedCategories") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.joinToString(",")
            ?.ifBlank { "ALL" }
            ?: "ALL"
        return DiscountCodeEntity(
            id = id,
            code = getString("code").orEmpty(),
            discountType = getString("discountType") ?: DiscountType.PERCENTAGE.key,
            discountValue = (get("discountValue") as? Number)?.toDouble() ?: 0.0,
            includedCategoriesCsv = cats,
            expiresAt = (get("expiresAt") as? Number)?.toLong() ?: 0L,
            maxUses = (get("maxUses") as? Number)?.toInt() ?: 1,
            usedCount = (get("usedCount") as? Number)?.toInt() ?: 0,
            isPublic = getBoolean("publicCode") ?: true,
            targetCustomerId = getString("targetCustomerId"),
            targetCustomerName = getString("targetCustomerName"),
            isActive = getBoolean("active") ?: true,
            createdAt = tsMillis("createdAt")
        )
    }

    private fun DocumentSnapshot.toContestEntity(): ContestEntity = ContestEntity(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        prizeType = getString("prizeType") ?: ContestPrizeType.BALANCE.key,
        prizeLabel = getString("prizeLabel").orEmpty(),
        prizeAmount = (get("prizeAmount") as? Number)?.toDouble() ?: 0.0,
        rulesText = getString("rulesText").orEmpty(),
        startsAt = (get("startsAt") as? Number)?.toLong() ?: 0L,
        endsAt = (get("endsAt") as? Number)?.toLong() ?: 0L,
        status = getString("status") ?: ContestStatus.ACTIVE.key,
        winnerCustomerId = getString("winnerCustomerId"),
        winnerCustomerName = getString("winnerCustomerName"),
        isPrizeApproved = getBoolean("prizeApproved") ?: false,
        isWinnerNotified = getBoolean("winnerNotified") ?: false,
        createdAt = tsMillis("createdAt")
    )

    private fun DocumentSnapshot.toContestEntryEntity(): ContestEntryEntity = ContestEntryEntity(
        id = id,
        contestId = getString("contestId").orEmpty(),
        customerId = getString("customerId").orEmpty(),
        customerName = getString("customerName").orEmpty(),
        customerWhatsapp = getString("customerWhatsapp").orEmpty(),
        createdAt = tsMillis("createdAt")
    )

    private fun DocumentSnapshot.toOrderEntity(): OrderEntity = OrderEntity(
        id = id,
        customerId = getString("customerId").orEmpty(),
        customerName = getString("customerName").orEmpty(),
        customerWhatsapp = getString("customerWhatsapp").orEmpty(),
        merchantId = getString("merchantId"),
        merchantName = getString("merchantName"),
        serviceId = getString("serviceId").orEmpty(),
        serviceName = getString("serviceName").orEmpty(),
        serviceCategory = getString("serviceCategory").orEmpty(),
        packageId = getString("packageId").orEmpty(),
        packageLabel = getString("packageLabel").orEmpty(),
        playerAccountInfo = getString("playerAccountInfo").orEmpty(),
        note = getString("note").orEmpty(),
        isOfferOrder = getBoolean("offerOrder") ?: false,
        discountCodeUsed = getString("discountCodeUsed"),
        originalPrice = (get("originalPrice") as? Number)?.toDouble() ?: 0.0,
        discountApplied = (get("discountApplied") as? Number)?.toDouble() ?: 0.0,
        finalPrice = (get("finalPrice") as? Number)?.toDouble() ?: 0.0,
        // [FIX #1.4] قراءة تكلفة المورد وصافي الربح المخزنين في الطلب بدلاً من 0.0
        supplierCost = (get("supplierCost") as? Number)?.toDouble() ?: 0.0,
        merchantCommission = (get("merchantCommission") as? Number)?.toDouble() ?: 0.0,
        netProfit = (get("netProfit") as? Number)?.toDouble() ?: 0.0,
        status = getString("status") ?: "pending",
        rejectionReason = getString("rejectionReason"),
        isCustomerNotified = getBoolean("customerNotified") ?: true,
        createdAt = tsMillis("createdAt"),
        updatedAt = tsMillis("updatedAt")
    )

    private fun DocumentSnapshot.toTopUpEntity(): TopUpEntity = TopUpEntity(
        id = id,
        customerId = getString("customerId").orEmpty(),
        customerName = getString("customerName").orEmpty(),
        customerWhatsapp = getString("customerWhatsapp").orEmpty(),
        amount = (get("amount") as? Number)?.toDouble() ?: 0.0,
        paymentMethod = getString("paymentMethod").orEmpty(),
        transactionRef = getString("transactionRef").orEmpty(),
        proofImageUri = getString("proofImageUri"),
        status = getString("status") ?: "pending",
        rejectionReason = getString("rejectionReason"),
        reviewedBy = getString("reviewedBy"),
        isCustomerNotified = getBoolean("customerNotified") ?: true,
        createdAt = tsMillis("createdAt"),
        updatedAt = tsMillis("updatedAt")
    )

    private fun DocumentSnapshot.toAuditLogEntity(): AuditLogEntity = AuditLogEntity(
        id = id,
        actorId = getString("actorId").orEmpty(),
        actorName = getString("actorName").orEmpty(),
        actorRole = getString("actorRole").orEmpty(),
        actionType = getString("actionType").orEmpty(),
        targetId = getString("targetId").orEmpty(),
        details = getString("details").orEmpty(),
        createdAt = tsMillis("createdAt")
    )

    private fun DocumentSnapshot.toSettingsEntity(): SettingsEntity = SettingsEntity(
        id = "global",
        agencyPhone = getString("agencyPhone") ?: "0670533324",
        whatsappNumber = getString("whatsappNumber") ?: "212670533324",
        loyaltyThreshold = (get("loyaltyThreshold") as? Number)?.toInt() ?: 4,
        loyaltyDiscountPercent = (get("loyaltyDiscountPercent") as? Number)?.toDouble() ?: 25.0,
        adminEmail = getString("adminEmail") ?: SeedData.ADMIN_EMAIL,
        agencyBalance = (get("agencyBalance") as? Number)?.toDouble() ?: 0.0
    )

    private fun normalizePhone(raw: String): String =
        raw.trim().replace(" ", "").replace("-", "")

    private fun Double.round2(): Double = PricingEngine.round2(this)
}

// [REVIEWED 2026-11] إصلاحات شاملة

