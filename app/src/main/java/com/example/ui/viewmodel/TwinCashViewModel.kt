package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.model.DailyProfitPoint
import com.example.data.model.DiscountType
import com.example.data.model.MerchantProfitItem
import com.example.data.model.ProfitPeriod
import com.example.data.model.ProfitSummary
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePackage
import com.example.data.model.ServiceProfitItem
import com.example.data.model.UserRole
import com.example.data.remote.toArabicFirebaseMessage
import com.example.data.repository.TwinCashFirestoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class NeedsRegistration(
        val verifiedEmail: String,
        val suggestedName: String,
        val isAdminEmail: Boolean
    ) : SessionUiState
    data class Blocked(val reason: String) : SessionUiState
    data class Ready(val user: UserEntity) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

data class ToastEvent(val message: String, val isError: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class TwinCashViewModel(
    private val repository: TwinCashFirestoreRepository,
    private val currentUserId: String,
    private val verifiedEmail: String,
    private val displayName: String,
    private val isEmailVerified: Boolean
) : ViewModel() {

    private companion object {
        const val TAG = "TwinCashVM"
        const val STOP_TIMEOUT_MS = 5000L
    }

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()

    private val _selectedProfitPeriod = MutableStateFlow(ProfitPeriod.MONTH)
    val selectedProfitPeriod: StateFlow<ProfitPeriod> = _selectedProfitPeriod.asStateFlow()

    private fun <T> Flow<T>.unwrapFirestoreFlow(defaultValue: T, label: String): StateFlow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing $label", firestoreError ?: error)
            emit(defaultValue)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), defaultValue)

    val sessionState: StateFlow<SessionUiState> = flow {
        val inviteBlock = repository.checkMerchantInviteStatusIfNoUserDoc()
        if (inviteBlock != null) {
            emit(SessionUiState.Blocked(inviteBlock))
            return@flow
        }
        repository.observeUser(currentUserId)
            .map { user ->
                when {
                    user == null -> SessionUiState.NeedsRegistration(
                        verifiedEmail = verifiedEmail,
                        suggestedName = displayName,
                        isAdminEmail = verifiedEmail.equals(SeedData.ADMIN_EMAIL, ignoreCase = true) && isEmailVerified
                    )
                    user.status == AccountStatus.PENDING_APPROVAL.key ->
                        SessionUiState.Blocked("حساب التاجر قيد المراجعة والقبول من طرف الأدمن")
                    user.status != AccountStatus.ACTIVE.key ->
                        SessionUiState.Blocked("تم إغلاق هذا الحساب أو حظره من طرف إدارة TwinCash")
                    else -> {
                        if (user.role == UserRole.ADMIN.key) {
                            viewModelScope.launch { repository.ensureBaseCatalogSeededForAdmin() }
                        }
                        SessionUiState.Ready(user)
                    }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing session user", root)
                emit(SessionUiState.Error(toArabicFirebaseMessage(root)))
            }
            .collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionUiState.Loading)

    private val readyUser: Flow<UserEntity?> = sessionState.map { (it as? SessionUiState.Ready)?.user }

    val settings: StateFlow<SettingsEntity> = readyUser
        .flatMapLatest { user ->
            if (user != null) repository.observeSettings() else flowOf(SettingsEntity())
        }
        .unwrapFirestoreFlow(SettingsEntity(), "settings")

    val services: StateFlow<List<ServiceEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.observeServices(includeSupplierCosts = user.role == UserRole.ADMIN.key)
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "services")

    val freeFireOffers: StateFlow<List<OfferEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.observeOffers().map { list ->
                    list.filter { it.serviceCategory == ServiceCategory.FREE_FIRE.key }
                }
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "offers")

    val contests: StateFlow<List<ContestEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null) repository.observeContests() else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "contests")

    val customerOrders: StateFlow<List<OrderEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.CUSTOMER.key) {
                repository.observeCustomerOrders(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "customerOrders")

    val customerTopUps: StateFlow<List<TopUpEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.CUSTOMER.key) {
                repository.observeCustomerTopUps(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "customerTopUps")

    val customerDiscountCodes: StateFlow<List<DiscountCodeEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.CUSTOMER.key) {
                repository.observeCustomerCodes(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "customerDiscountCodes")

    val customerContestEntries: StateFlow<List<ContestEntryEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.CUSTOMER.key) {
                repository.observeCustomerContestEntries(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "customerContestEntries")

    val merchantOrders: StateFlow<List<OrderEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.MERCHANT.key) {
                repository.observeMerchantOrders(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .unwrapFirestoreFlow(emptyList(), "merchantOrders")

    // Admin-only streams
    val adminUsers: StateFlow<List<UserEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.ADMIN.key) repository.observeAllUsers()
            else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "adminUsers")

    val adminOrders: StateFlow<List<OrderEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.ADMIN.key) repository.observeAllOrdersForAdmin()
            else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "adminOrders")

    val adminTopUps: StateFlow<List<TopUpEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.ADMIN.key) repository.observeAllTopUps()
            else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "adminTopUps")

    val adminAuditLogs: StateFlow<List<AuditLogEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.ADMIN.key) repository.observeAllAuditLogs()
            else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "adminAuditLogs")

    val adminDiscountCodes: StateFlow<List<DiscountCodeEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.ADMIN.key) repository.observeAllDiscountCodes()
            else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "adminDiscountCodes")

    val adminContestEntries: StateFlow<List<ContestEntryEntity>> = readyUser
        .flatMapLatest { user ->
            if (user != null && user.role == UserRole.ADMIN.key) repository.observeAllContestEntries()
            else flowOf(emptyList())
        }
        .unwrapFirestoreFlow(emptyList(), "adminContestEntries")

    fun notify(message: String, isError: Boolean = false) {
        _toastEvents.tryEmit(ToastEvent(message, isError))
    }

    fun completeGoogleRegistration(fullName: String, whatsapp: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.registerGoogleUserProfile(fullName, whatsapp)
            _isBusy.value = false
            res.onSuccess { user ->
                val msg = if (user.role == UserRole.ADMIN.key) {
                    "تم تفعيل حساب الإدارة الرئيسي (admin) بنجاح"
                } else {
                    "تم إنشاء حسابك بنجاح في TwinCash"
                }
                notify(msg)
            }.onFailure { err ->
                notify(err.message ?: "تعذر إنشاء الحساب", isError = true)
            }
        }
    }

    fun updateProfile(fullName: String, whatsapp: String, avatarUri: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateCustomerProfile(currentUserId, fullName, whatsapp, avatarUri)
            _isBusy.value = false
            res.onSuccess { notify("تم تحديث بيانات الحساب بنجاح") }
                .onFailure { err -> notify(err.message ?: "فشل تحديث البيانات", isError = true) }
        }
    }

    fun validateDiscountCode(
        serviceCategory: String,
        code: String,
        basePrice: Double,
        onResult: (Result<Pair<DiscountCodeEntity, Double>>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.validateDiscountCode(currentUserId, serviceCategory, code, basePrice)
            onResult(res)
        }
    }

    fun placeServiceOrder(
        serviceId: String,
        packageId: String,
        playerAccountInfo: String,
        note: String,
        discountCode: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.placeServiceOrder(
                currentUserId, serviceId, packageId, playerAccountInfo, note, discountCode
            )
            _isBusy.value = false
            res.onSuccess { order ->
                val discountNote = if (order.discountApplied > 0.0) " (خصم ${order.discountApplied} د.م)" else ""
                notify("تم إرسال الطلب ${order.id} وهو الآن قيد الانتظار$discountNote")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر إرسال الطلب", isError = true)
            }
        }
    }

    fun placeOfferOrder(
        offerId: String,
        playerAccountInfo: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.placeOfferOrder(currentUserId, offerId, playerAccountInfo, note)
            _isBusy.value = false
            res.onSuccess { order ->
                notify("تم إرسال طلب العرض ${order.id} وهو قيد الانتظار")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر طلب العرض", isError = true)
            }
        }
    }

    fun submitTopUp(
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        proofImageUri: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitTopUp(currentUserId, amount, paymentMethod, transactionRef, proofImageUri)
            _isBusy.value = false
            res.onSuccess { topUp ->
                notify("تم إرسال طلب التعبئة ${topUp.id} بمبلغ ${topUp.amount} د.م للمراجعة")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر إرسال طلب التعبئة", isError = true)
            }
        }
    }

    fun joinContest(contestId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinContest(currentUserId, contestId)
            _isBusy.value = false
            res.onSuccess { notify("تم تسجيل مشاركتك في المسابقة بنجاح!") }
                .onFailure { err -> notify(err.message ?: "تعذر المشاركة", isError = true) }
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead(currentUserId)
        }
    }

    // --- Merchant / Admin Order Actions ---

    fun acceptAndCompleteOrder(orderId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.acceptAndCompleteOrder(currentUserId, orderId)
            _isBusy.value = false
            res.onSuccess { notify("تم قبول وإتمام الطلب $orderId بنجاح") }
                .onFailure { err -> notify(err.message ?: "فشل تنفيذ الطلب", isError = true) }
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rejectOrder(currentUserId, orderId, reason)
            _isBusy.value = false
            res.onSuccess { notify("تم رفض الطلب $orderId وإرجاع الرصيد للزبون تلقائياً") }
                .onFailure { err -> notify(err.message ?: "فشل رفض الطلب", isError = true) }
        }
    }

    // --- Admin Actions ---

    fun approveTopUp(topUpId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.approveTopUp(currentUserId, topUpId)
            _isBusy.value = false
            res.onSuccess { notify("تم قبول التعبئة وإضافة الرصيد لحساب الزبون") }
                .onFailure { err -> notify(err.message ?: "تعذر قبول التعبئة", isError = true) }
        }
    }

    fun rejectTopUp(topUpId: String, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rejectTopUp(currentUserId, topUpId, reason)
            _isBusy.value = false
            res.onSuccess { notify("تم رفض طلب التعبئة $topUpId") }
                .onFailure { err -> notify(err.message ?: "تعذر رفض التعبئة", isError = true) }
        }
    }

    fun saveService(
        id: String?,
        category: ServiceCategory,
        name: String,
        description: String,
        packages: List<ServicePackage>,
        isAvailable: Boolean,
        displayOrder: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveService(
                currentUserId, id, category, name, description, packages, isAvailable, displayOrder
            )
            _isBusy.value = false
            res.onSuccess {
                notify("تم حفظ الخدمة بنجاح")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر حفظ الخدمة", isError = true)
            }
        }
    }

    fun toggleServiceAvailability(serviceId: String, isAvailable: Boolean) {
        viewModelScope.launch {
            repository.toggleServiceAvailability(currentUserId, serviceId, isAvailable)
                .onSuccess { notify(if (isAvailable) "تم إظهار الخدمة للزبائن" else "تم إخفاء الخدمة مؤقتاً") }
                .onFailure { notify(it.message ?: "فشل التحديث", isError = true) }
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(currentUserId, serviceId)
                .onSuccess { notify("تم حذف الخدمة") }
                .onFailure { notify(it.message ?: "فشل الحذف", isError = true) }
        }
    }

    fun saveOffer(
        id: String?,
        title: String,
        description: String,
        price: Double,
        originalPrice: Double,
        daysValid: Int,
        isActive: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveOffer(
                currentUserId, id, title, description, price, originalPrice, daysValid, isActive
            )
            _isBusy.value = false
            res.onSuccess {
                notify("تم حفظ عرض Free Fire بنجاح")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر حفظ العرض", isError = true)
            }
        }
    }

    fun deleteOffer(offerId: String) {
        viewModelScope.launch {
            repository.deleteOffer(currentUserId, offerId)
                .onSuccess { notify("تم حذف العرض") }
                .onFailure { notify(it.message ?: "فشل الحذف", isError = true) }
        }
    }

    fun saveDiscountCode(
        id: String?,
        code: String,
        discountType: DiscountType,
        discountValue: Double,
        includedCategoriesCsv: String,
        daysValid: Int,
        maxUses: Int,
        isPublic: Boolean,
        targetCustomerId: String?,
        isActive: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveDiscountCode(
                currentUserId, id, code, discountType, discountValue,
                includedCategoriesCsv, daysValid, maxUses, isPublic, targetCustomerId, isActive
            )
            _isBusy.value = false
            res.onSuccess {
                notify("تم حفظ كود الخصم بنجاح")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر حفظ الكود", isError = true)
            }
        }
    }

    fun toggleDiscountCode(codeId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleDiscountCode(currentUserId, codeId, active)
                .onSuccess { notify(if (active) "تم تفعيل الكود" else "تم تعطيل الكود") }
                .onFailure { notify(it.message ?: "فشل التحديث", isError = true) }
        }
    }

    fun deleteDiscountCode(codeId: String) {
        viewModelScope.launch {
            repository.deleteDiscountCode(currentUserId, codeId)
                .onSuccess { notify("تم حذف كود الخصم") }
                .onFailure { notify(it.message ?: "فشل الحذف", isError = true) }
        }
    }

    fun saveContest(
        id: String?,
        title: String,
        description: String,
        prizeType: ContestPrizeType,
        prizeLabel: String,
        prizeAmount: Double,
        rulesText: String,
        daysDuration: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveContest(
                currentUserId, id, title, description, prizeType, prizeLabel, prizeAmount, rulesText, daysDuration
            )
            _isBusy.value = false
            res.onSuccess {
                notify("تم حفظ المسابقة بنجاح")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر حفظ المسابقة", isError = true)
            }
        }
    }

    fun selectContestWinner(contestId: String, specificCustomerId: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.selectContestWinner(currentUserId, contestId, specificCustomerId)
            _isBusy.value = false
            res.onSuccess { name -> notify("تم اختيار الفائز: $name (اضغط اعتماد لمنح الجائزة)") }
                .onFailure { err -> notify(err.message ?: "تعذر اختيار الفائز", isError = true) }
        }
    }

    fun approveContestPrize(contestId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.approveContestPrize(currentUserId, contestId)
            _isBusy.value = false
            res.onSuccess { notify("تم اعتماد الجائزة ومنحها للفائز بنجاح!") }
                .onFailure { err -> notify(err.message ?: "تعذر اعتماد الجائزة", isError = true) }
        }
    }

    fun deleteContest(contestId: String) {
        viewModelScope.launch {
            repository.deleteContest(currentUserId, contestId)
                .onSuccess { notify("تم حذف المسابقة") }
                .onFailure { notify(it.message ?: "فشل الحذف", isError = true) }
        }
    }

    fun createMerchant(
        fullName: String,
        email: String,
        whatsapp: String,
        commissionType: CommissionType,
        commissionValue: Double,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createMerchant(
                currentUserId, fullName, email, whatsapp, commissionType, commissionValue
            )
            _isBusy.value = false
            res.onSuccess {
                notify("تم إنشاء دعوة التاجر بحالة قيد القبول")
                onSuccess()
            }.onFailure { err ->
                notify(err.message ?: "تعذر إنشاء التاجر", isError = true)
            }
        }
    }

    fun updateUserStatus(targetUid: String, status: AccountStatus) {
        viewModelScope.launch {
            repository.updateUserStatus(currentUserId, targetUid, status)
                .onSuccess { notify("تم تحديث حالة الحساب إلى: ${status.arabicLabel}") }
                .onFailure { notify(it.message ?: "فشل التحديث", isError = true) }
        }
    }

    fun updateMerchantCommission(merchantId: String, type: CommissionType, value: Double) {
        viewModelScope.launch {
            repository.updateMerchantCommission(currentUserId, merchantId, type, value)
                .onSuccess { notify("تم تحديث عمولة التاجر") }
                .onFailure { notify(it.message ?: "فشل التحديث", isError = true) }
        }
    }

    fun adjustCustomerBalance(customerId: String, newBalance: Double) {
        viewModelScope.launch {
            repository.adjustCustomerBalance(currentUserId, customerId, newBalance)
                .onSuccess { notify("تم تعديل رصيد الزبون بنجاح") }
                .onFailure { notify(it.message ?: "فشل تعديل الرصيد", isError = true) }
        }
    }

    fun updateSettings(
        agencyPhone: String,
        whatsappNumber: String,
        loyaltyThreshold: Int,
        loyaltyDiscountPercent: Double
    ) {
        viewModelScope.launch {
            repository.updateSettings(currentUserId, agencyPhone, whatsappNumber, loyaltyThreshold, loyaltyDiscountPercent)
                .onSuccess { notify("تم حفظ إعدادات الوكالة بنجاح") }
                .onFailure { notify(it.message ?: "فشل حفظ الإعدادات", isError = true) }
        }
    }

    fun selectProfitPeriod(period: ProfitPeriod) {
        _selectedProfitPeriod.value = period
    }

    fun computeProfitSummary(orders: List<OrderEntity>, period: ProfitPeriod): ProfitSummary {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val completed = orders.filter { it.status == "completed" }
        val filtered = if (period == ProfitPeriod.ALL) completed else completed.filter {
            it.updatedAt >= now - period.daysBack * dayMs
        }

        return ProfitSummary(
            totalSales = filtered.sumOf { it.finalPrice },
            totalSupplierCost = filtered.sumOf { it.supplierCost },
            totalMerchantCommission = filtered.sumOf { it.merchantCommission },
            netProfit = filtered.sumOf { it.netProfit },
            todayProfit = completed.filter { it.updatedAt >= now - dayMs }.sumOf { it.netProfit },
            weekProfit = completed.filter { it.updatedAt >= now - 7 * dayMs }.sumOf { it.netProfit },
            monthProfit = completed.filter { it.updatedAt >= now - 30 * dayMs }.sumOf { it.netProfit },
            completedCount = filtered.size
        )
    }

    fun computeServiceProfits(orders: List<OrderEntity>, period: ProfitPeriod): List<ServiceProfitItem> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val completed = orders.filter {
            it.status == "completed" && (period == ProfitPeriod.ALL || it.updatedAt >= now - period.daysBack * dayMs)
        }
        return completed.groupBy { it.serviceCategory }.map { (catKey, list) ->
            val cat = ServiceCategory.fromKey(catKey)
            ServiceProfitItem(
                serviceName = cat.arabicTitle,
                category = cat,
                ordersCount = list.size,
                totalSales = list.sumOf { it.finalPrice },
                netProfit = list.sumOf { it.netProfit }
            )
        }.sortedByDescending { it.netProfit }
    }

    fun computeMerchantProfits(orders: List<OrderEntity>, period: ProfitPeriod): List<MerchantProfitItem> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val completed = orders.filter {
            it.status == "completed" && !it.merchantId.isNullOrBlank() &&
                (period == ProfitPeriod.ALL || it.updatedAt >= now - period.daysBack * dayMs)
        }
        return completed.groupBy { it.merchantId!! }.map { (mId, list) ->
            MerchantProfitItem(
                merchantId = mId,
                merchantName = list.firstOrNull()?.merchantName ?: "تاجر",
                ordersCount = list.size,
                totalSales = list.sumOf { it.finalPrice },
                totalCommission = list.sumOf { it.merchantCommission },
                netProfitGenerated = list.sumOf { it.netProfit }
            )
        }.sortedByDescending { it.totalSales }
    }

    fun computeDailySeries(orders: List<OrderEntity>, days: Int = 7): List<DailyProfitPoint> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val fmt = SimpleDateFormat("dd/MM", Locale("ar", "MA"))
        val completed = orders.filter { it.status == "completed" }

        return (days - 1 downTo 0).map { offset ->
            val dayEnd = now - offset * dayMs
            val dayStart = dayEnd - dayMs
            val dayOrders = completed.filter { it.updatedAt in dayStart..dayEnd }
            DailyProfitPoint(
                dayLabel = fmt.format(Date(dayEnd)),
                sales = dayOrders.sumOf { it.finalPrice },
                profit = dayOrders.sumOf { it.netProfit }
            )
        }
    }

    fun buildProfitsCsvReport(orders: List<OrderEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val header = "\uFEFFرقم الطلب,التاريخ,الزبون,الخدمة,الباقة,المنفذ,كود الخصم,سعر البيع,سعر الشراء,عمولة التاجر,صافي الربح,الحالة\n"
        val rows = orders.joinToString("\n") { o ->
            listOf(
                o.id,
                sdf.format(Date(o.updatedAt)),
                o.customerName.replace(",", " "),
                o.serviceName.replace(",", " "),
                o.packageLabel.replace(",", " "),
                (o.merchantName ?: "-").replace(",", " "),
                o.discountCodeUsed ?: "-",
                String.format(Locale.US, "%.2f", o.finalPrice),
                String.format(Locale.US, "%.2f", o.supplierCost),
                String.format(Locale.US, "%.2f", o.merchantCommission),
                String.format(Locale.US, "%.2f", o.netProfit),
                o.status
            ).joinToString(",")
        }
        return header + rows
    }
}
