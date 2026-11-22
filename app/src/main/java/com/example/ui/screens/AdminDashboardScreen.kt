package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AuditLogEntity
import com.example.data.local.ContestEntity
import com.example.data.local.ContestEntryEntity
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.OfferEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ServiceEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.TopUpEntity
import com.example.data.local.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.CommissionType
import com.example.data.model.ContestPrizeType
import com.example.data.model.DiscountType
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePackage
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.RejectReasonDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.components.formatShortDate
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ErrorBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyInteractive
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SectionBg
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.TwinCashCardShape
import com.example.ui.theme.TwinCashInputShape
import com.example.ui.theme.TwinCashSecondaryButtonBorder
import com.example.ui.theme.WarningBg
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors

@Composable
fun AdminDashboardScreen(
    users: List<UserEntity>,
    services: List<ServiceEntity>,
    offers: List<OfferEntity>,
    discountCodes: List<DiscountCodeEntity>,
    contests: List<ContestEntity>,
    contestEntries: List<ContestEntryEntity>,
    orders: List<OrderEntity>,
    topUps: List<TopUpEntity>,
    auditLogs: List<AuditLogEntity>,
    settings: SettingsEntity,
    todayProfit: Double,
    isBusy: Boolean,
    onAcceptOrder: (String) -> Unit,
    onRejectOrder: (String, String) -> Unit,
    onApproveTopUp: (String) -> Unit,
    onRejectTopUp: (String, String) -> Unit,
    onSaveService: (id: String?, category: ServiceCategory, name: String, desc: String, packages: List<ServicePackage>, available: Boolean, displayOrder: Int, onDone: () -> Unit) -> Unit,
    onToggleServiceAvailability: (String, Boolean) -> Unit,
    onDeleteService: (String) -> Unit,
    onSaveOffer: (id: String?, title: String, desc: String, price: Double, origPrice: Double, days: Int, active: Boolean, onDone: () -> Unit) -> Unit,
    onDeleteOffer: (String) -> Unit,
    onSaveDiscountCode: (id: String?, code: String, type: DiscountType, value: Double, categoriesCsv: String, daysValid: Int, maxUses: Int, isPublic: Boolean, targetCustomerId: String?, isActive: Boolean, onDone: () -> Unit) -> Unit,
    onToggleDiscountCode: (String, Boolean) -> Unit,
    onDeleteDiscountCode: (String) -> Unit,
    onSaveContest: (id: String?, title: String, desc: String, prizeType: ContestPrizeType, prizeLabel: String, prizeAmount: Double, rules: String, days: Int, onDone: () -> Unit) -> Unit,
    onSelectContestWinner: (contestId: String, specificCustomerId: String?) -> Unit,
    onApproveContestPrize: (String) -> Unit,
    onDeleteContest: (String) -> Unit,
    onCreateMerchant: (name: String, email: String, whatsapp: String, type: CommissionType, value: Double, onDone: () -> Unit) -> Unit,
    onUpdateUserStatus: (uid: String, status: AccountStatus) -> Unit,
    onUpdateMerchantCommission: (uid: String, type: CommissionType, value: Double) -> Unit,
    onAdjustCustomerBalance: (uid: String, newBalance: Double) -> Unit,
    onUpdateSettings: (phone: String, whatsapp: String, threshold: Int, discountPct: Double) -> Unit
) {
    val sections = listOf("الطلبات والشحن", "إدارة الخدمات", "الزبائن", "الخصومات والمسابقات", "العروض", "التجار والإعدادات")
    var selectedSection by remember { mutableIntStateOf(0) }

    var rejectOrderId by remember { mutableStateOf<String?>(null) }
    var rejectTopUpId by remember { mutableStateOf<String?>(null) }
    var showCreateMerchant by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<OfferEntity?>(null) }
    var showNewOffer by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceEntity?>(null) }
    var showNewService by remember { mutableStateOf(false) }
    var adjustBalanceUser by remember { mutableStateOf<UserEntity?>(null) }
    var editCommissionMerchant by remember { mutableStateOf<UserEntity?>(null) }
    var detailsCustomer by remember { mutableStateOf<UserEntity?>(null) }

    var showNewCode by remember { mutableStateOf(false) }
    var editingCode by remember { mutableStateOf<DiscountCodeEntity?>(null) }
    var grantCodeCustomer by remember { mutableStateOf<UserEntity?>(null) }

    var showNewContest by remember { mutableStateOf(false) }
    var editingContest by remember { mutableStateOf<ContestEntity?>(null) }
    var participantsContest by remember { mutableStateOf<ContestEntity?>(null) }

    var customerSearch by remember { mutableStateOf("") }
    var customerFilter by remember { mutableIntStateOf(0) }

    val customers = remember(users) { users.filter { it.role == UserRole.CUSTOMER.key } }
    val merchants = remember(users) { users.filter { it.role == UserRole.MERCHANT.key } }
    val pendingOrdersCount = remember(orders) { orders.count { it.status == "pending" } }
    val pendingTopUpsCount = remember(topUps) { topUps.count { it.status == "pending" } }

    val filteredCustomers = remember(customers, customerSearch, customerFilter, settings.loyaltyThreshold) {
        val q = customerSearch.trim().lowercase()
        customers.filter { c ->
            val matchesQuery = q.isEmpty() ||
                c.fullName.lowercase().contains(q) ||
                c.email.lowercase().contains(q) ||
                c.whatsapp.lowercase().contains(q)
            val matchesFilter = when (customerFilter) {
                1 -> c.status == AccountStatus.ACTIVE.key
                2 -> c.status == AccountStatus.CLOSED.key
                3 -> c.loyaltyCount >= settings.loyaltyThreshold
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { AdminQuickStat("الزبائن", "${customers.size}", NavyPrimary) }
            item { AdminQuickStat("طلبات معلقة", "${pendingOrdersCount + pendingTopUpsCount}", WarningOrange) }
            item { AdminQuickStat("رصيد الوكالة", formatMad(settings.agencyBalance), NavyInteractive) }
            item { AdminQuickStat("أرباح اليوم", formatMad(todayProfit), SuccessGreen) }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sections.indices.toList()) { idx ->
                FilterChip(
                    selected = selectedSection == idx,
                    onClick = { selectedSection = idx },
                    label = { Text(sections[idx], fontWeight = FontWeight.Bold) },
                    colors = twinCashChipColors(),
                    modifier = Modifier.testTag("admin_section_tab_$idx")
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedSection) {
                // 0: Orders & Top-Ups
                0 -> {
                    item {
                        Text("طلبات تعبئة الرصيد (${pendingTopUpsCount} معلق)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    if (topUps.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.AddCard,
                                title = "لا توجد طلبات تعبئة رصيد بعد",
                                subtitle = "ستظهر هنا طلبات تعبئة الرصيد الواردة من الزبائن"
                            )
                        }
                    } else {
                        items(topUps, key = { it.id }) { topUp ->
                            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${topUp.customerName} • ${formatMad(topUp.amount)}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        OrderStatusPill(topUp.status)
                                    }
                                    Text("${topUp.paymentMethod} • المرجع: ${topUp.transactionRef} • واتساب: ${topUp.customerWhatsapp}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                    if (!topUp.proofImageUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = topUp.proofImageUri,
                                            contentDescription = "إثبات التحويل",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, BorderLight, RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (topUp.status == "pending") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                enabled = !isBusy,
                                                onClick = { onApproveTopUp(topUp.id) },
                                                colors = twinCashPrimaryButtonColors(),
                                                shape = TwinCashButtonShape,
                                                modifier = Modifier.weight(1f).testTag("admin_approve_topup_${topUp.id}")
                                            ) { Text("قبول وإضافة الرصيد", fontWeight = FontWeight.Bold) }
                                            OutlinedButton(
                                                enabled = !isBusy,
                                                onClick = { rejectTopUpId = topUp.id },
                                                shape = TwinCashButtonShape,
                                                border = BorderStroke(1.dp, ErrorRed),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("رفض", color = ErrorRed, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("طلبات الخدمات (${pendingOrdersCount} معلق)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    if (orders.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.ReceiptLong,
                                title = "لا توجد طلبات خدمات بعد",
                                subtitle = "ستظهر هنا جميع طلبات الزبائن فور إرسالها"
                            )
                        }
                    } else {
                        items(orders, key = { it.id }) { order ->
                            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${order.id} • ${order.serviceName}", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        OrderStatusPill(order.status)
                                    }
                                    Text("${order.customerName} • ${order.packageLabel} • ${order.playerAccountInfo}", style = MaterialTheme.typography.bodySmall, color = NavyInteractive)
                                    Text(
                                        "السعر: ${formatMad(order.finalPrice)} | التكلفة: ${formatMad(order.supplierCost)} | الربح: ${formatMad(order.netProfit)}" +
                                            (order.discountCodeUsed?.let { " | كود: $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySlate
                                    )
                                    if (order.status == "pending") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                enabled = !isBusy,
                                                onClick = { onAcceptOrder(order.id) },
                                                colors = twinCashPrimaryButtonColors(),
                                                shape = TwinCashButtonShape,
                                                modifier = Modifier.weight(1f).testTag("admin_accept_order_${order.id}")
                                            ) {
                                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("قبول وإتمام", fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                enabled = !isBusy,
                                                onClick = { rejectOrderId = order.id },
                                                shape = TwinCashButtonShape,
                                                border = BorderStroke(1.dp, ErrorRed),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Outlined.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("رفض", color = ErrorRed, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1: Admin Services CRUD
                1 -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("إدارة الخدمات (${services.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Button(
                                onClick = { showNewService = true },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape,
                                modifier = Modifier.testTag("admin_add_service_btn")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة خدمة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (services.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.Storefront,
                                title = "لا توجد خدمات مضافة بعد",
                                subtitle = "اضغط على إضافة خدمة لإنشاء أول خدمة وباقاتها"
                            )
                        }
                    } else {
                        items(services, key = { it.id }) { srv ->
                            AdminServiceCard(
                                service = srv,
                                onToggleAvailability = { onToggleServiceAvailability(srv.id, it) },
                                onEdit = { editingService = srv },
                                onDelete = { onDeleteService(srv.id) }
                            )
                        }
                    }
                }

                // 2: Customers list & hub actions
                2 -> {
                    item {
                        Text("قائمة الزبائن (${filteredCustomers.size}/${customers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    item {
                        CustomerFilterBar(
                            searchQuery = customerSearch,
                            onSearchChange = { customerSearch = it },
                            selectedFilter = customerFilter,
                            onSelectFilter = { customerFilter = it }
                        )
                    }
                    if (filteredCustomers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.Group,
                                title = if (customers.isEmpty()) "لا يوجد زبائن مسجلون بعد" else "لا توجد نتائج مطابقة للبحث",
                                subtitle = if (customers.isEmpty()) "سيظهر كل زبون هنا بمجرد إنشاء حسابه" else "جرب تغيير كلمة البحث أو الفلتر"
                            )
                        }
                    } else {
                        items(filteredCustomers, key = { it.uid }) { cust ->
                            val isClosed = cust.status == AccountStatus.CLOSED.key
                            AdminCustomerCard(
                                customer = cust,
                                ordersCount = orders.count { it.customerId == cust.uid },
                                loyaltyThreshold = settings.loyaltyThreshold,
                                onViewDetails = { detailsCustomer = cust },
                                onAdjustBalance = { adjustBalanceUser = cust },
                                onToggleBan = { onUpdateUserStatus(cust.uid, if (isClosed) AccountStatus.ACTIVE else AccountStatus.CLOSED) },
                                onGrantCode = { grantCodeCustomer = cust }
                            )
                        }
                    }
                }

                // 3: Discount Codes & Contests
                3 -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("أكواد الخصم (${discountCodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Button(
                                onClick = { showNewCode = true },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape,
                                modifier = Modifier.testTag("admin_add_discount_btn")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة كود", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (discountCodes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.ConfirmationNumber,
                                title = "لا توجد أكواد خصم بعد",
                                subtitle = "أنشئ أكواد خصم عامة أو مخصصة لزبون معين"
                            )
                        }
                    } else {
                        items(discountCodes, key = { it.id }) { dc ->
                            AdminDiscountCodeCard(
                                code = dc,
                                onToggleActive = { onToggleDiscountCode(dc.id, it) },
                                onEdit = { editingCode = dc },
                                onDelete = { onDeleteDiscountCode(dc.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("إدارة المسابقات (${contests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Button(
                                onClick = { showNewContest = true },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape,
                                modifier = Modifier.testTag("admin_add_contest_btn")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة مسابقة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (contests.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.EmojiEvents,
                                title = "لا توجد مسابقات حالياً",
                                subtitle = "أضف مسابقة جديدة لتنشيط مشاركة الزبائن"
                            )
                        }
                    } else {
                        items(contests, key = { it.id }) { cnt ->
                            val count = contestEntries.count { it.contestId == cnt.id }
                            AdminContestCard(
                                contest = cnt,
                                participantsCount = count,
                                onOpenParticipants = { participantsContest = cnt },
                                onApprovePrize = { onApproveContestPrize(cnt.id) },
                                onEdit = { editingContest = cnt },
                                onDelete = { onDeleteContest(cnt.id) }
                            )
                        }
                    }
                }

                // 4: Free Fire Offers
                4 -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("عروض Free Fire الحصرية (${offers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Button(
                                onClick = { showNewOffer = true },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة عرض", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (offers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.LocalOffer,
                                title = "لا توجد عروض Free Fire بعد",
                                subtitle = "أضف عروضاً خاصة لتظهر للزبائن في قسم العروض"
                            )
                        }
                    } else {
                        items(offers, key = { it.id }) { off ->
                            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(off.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Text("${formatMad(off.price)} (الأصل: ${formatMad(off.originalPrice)}) • ينتهي: ${formatShortDate(off.expiresAt)}", style = MaterialTheme.typography.bodySmall, color = NavyInteractive)
                                    }
                                    Row {
                                        IconButton(onClick = { editingOffer = off }) { Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = NavyPrimary) }
                                        IconButton(onClick = { onDeleteOffer(off.id) }) { Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = ErrorRed) }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5: Merchants, Settings & Audit Logs
                5 -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("إدارة التجار (${merchants.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Button(
                                onClick = { showCreateMerchant = true },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape,
                                modifier = Modifier.testTag("admin_add_merchant_btn")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة تاجر", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (merchants.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.Storefront,
                                title = "لا يوجد تجار مضافون بعد",
                                subtitle = "أنشئ حساب تاجر جديد وفعّله من هنا"
                            )
                        }
                    } else {
                        items(merchants, key = { it.uid }) { m ->
                            val status = AccountStatus.fromKey(m.status)
                            val (bg, fg) = when (status) {
                                AccountStatus.ACTIVE -> SuccessBg to SuccessGreen
                                AccountStatus.PENDING_APPROVAL -> WarningBg to WarningOrange
                                AccountStatus.CLOSED -> ErrorBg to ErrorRed
                            }
                            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(m.fullName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                            Text("${m.email} • ${m.whatsapp}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                            Text("العمولة: ${m.commissionValue} (${CommissionType.fromKey(m.commissionType).arabicLabel})", style = MaterialTheme.typography.labelSmall, color = NavyInteractive)
                                        }
                                        StatusPill(label = status.arabicLabel, bgColor = bg, textColor = fg)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        if (status != AccountStatus.ACTIVE) {
                                            Button(
                                                onClick = { onUpdateUserStatus(m.uid, AccountStatus.ACTIVE) },
                                                colors = twinCashPrimaryButtonColors(),
                                                shape = TwinCashButtonShape,
                                                modifier = Modifier.weight(1f).testTag("approve_merchant_${m.uid}")
                                            ) { Text("قبول وتفعيل", fontWeight = FontWeight.Bold) }
                                        }
                                        if (status != AccountStatus.CLOSED) {
                                            OutlinedButton(
                                                onClick = { onUpdateUserStatus(m.uid, AccountStatus.CLOSED) },
                                                shape = TwinCashButtonShape,
                                                border = BorderStroke(1.dp, ErrorRed),
                                                modifier = Modifier.weight(1f).testTag("close_merchant_${m.uid}")
                                            ) {
                                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إغلاق", color = ErrorRed, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { editCommissionMerchant = m },
                                            shape = TwinCashButtonShape,
                                            border = TwinCashSecondaryButtonBorder,
                                            colors = twinCashSecondaryButtonColors(),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("العمولة", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { AdminSettingsCard(settings = settings, onSave = onUpdateSettings) }
                    item { Text("سجل نشاط التجار والإدارة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary) }
                    if (auditLogs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.History,
                                title = "لا توجد سجلات نشاط بعد",
                                subtitle = "سيتم توثيق جميع العمليات الإدارية والتجارية هنا تلقائياً"
                            )
                        }
                    } else {
                        items(auditLogs, key = { it.id }) { log ->
                            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${log.actorName} (${UserRole.fromKey(log.actorRole).arabicLabel})", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        Text(log.actionType, style = MaterialTheme.typography.labelSmall, color = NavyInteractive)
                                    }
                                    Text(log.details, style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark)
                                    Text(formatShortDate(log.createdAt), style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Dialogs
    rejectOrderId?.let { id ->
        RejectReasonDialog("رفض الطلب $id", { rejectOrderId = null }) { reason ->
            onRejectOrder(id, reason)
            rejectOrderId = null
        }
    }
    rejectTopUpId?.let { id ->
        RejectReasonDialog("رفض طلب الشحن $id", { rejectTopUpId = null }) { reason ->
            onRejectTopUp(id, reason)
            rejectTopUpId = null
        }
    }
    if (showCreateMerchant) {
        CreateMerchantDialog({ showCreateMerchant = false }) { n, e, w, t, v ->
            onCreateMerchant(n, e, w, t, v) { showCreateMerchant = false }
        }
    }
    if (showNewOffer || editingOffer != null) {
        OfferEditorDialog(
            initial = editingOffer,
            onDismiss = { showNewOffer = false; editingOffer = null },
            onSave = { id, title, desc, price, orig, days, active ->
                onSaveOffer(id, title, desc, price, orig, days, active) { showNewOffer = false; editingOffer = null }
            }
        )
    }
    if (showNewService || editingService != null) {
        AdminServiceEditorDialog(
            initial = editingService,
            defaultOrder = services.size + 1,
            onDismiss = { showNewService = false; editingService = null },
            onSave = { id, cat, name, desc, pkgs, avail, order ->
                onSaveService(id, cat, name, desc, pkgs, avail, order) { showNewService = false; editingService = null }
            }
        )
    }
    if (showNewCode || editingCode != null || grantCodeCustomer != null) {
        DiscountCodeEditorDialog(
            initial = editingCode,
            preselectedCustomer = grantCodeCustomer,
            customers = customers,
            onDismiss = { showNewCode = false; editingCode = null; grantCodeCustomer = null },
            onSave = { id, code, type, value, catsCsv, days, maxUses, isPub, targetId, active ->
                onSaveDiscountCode(id, code, type, value, catsCsv, days, maxUses, isPub, targetId, active) {
                    showNewCode = false; editingCode = null; grantCodeCustomer = null
                }
            }
        )
    }
    if (showNewContest || editingContest != null) {
        ContestEditorDialog(
            initial = editingContest,
            onDismiss = { showNewContest = false; editingContest = null },
            onSave = { id, title, desc, prizeType, prizeLabel, prizeAmt, rules, days ->
                onSaveContest(id, title, desc, prizeType, prizeLabel, prizeAmt, rules, days) {
                    showNewContest = false; editingContest = null
                }
            }
        )
    }
    participantsContest?.let { cnt ->
        val latest = contests.firstOrNull { it.id == cnt.id } ?: cnt
        ContestParticipantsDialog(
            contest = latest,
            entries = contestEntries.filter { it.contestId == latest.id },
            onDismiss = { participantsContest = null },
            onRandomDraw = { onSelectContestWinner(latest.id, null) },
            onPickWinner = { custId -> onSelectContestWinner(latest.id, custId) }
        )
    }
    detailsCustomer?.let { cust ->
        CustomerDetailsDialog(cust, orders, discountCodes, settings) { detailsCustomer = null }
    }
    adjustBalanceUser?.let { cust ->
        var balText by remember(cust.uid) { mutableStateOf(cust.balance.toString()) }
        AlertDialog(
            onDismissRequest = { adjustBalanceUser = null },
            containerColor = BgWhite,
            shape = TwinCashCardShape,
            title = { Text("تعديل رصيد ${cust.fullName}", color = NavyPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = balText,
                    onValueChange = { balText = it },
                    label = { Text("الرصيد الجديد (د.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdjustCustomerBalance(cust.uid, balText.toDoubleOrNull() ?: cust.balance)
                        adjustBalanceUser = null
                    },
                    colors = twinCashPrimaryButtonColors(),
                    shape = TwinCashButtonShape
                ) { Text("حفظ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { adjustBalanceUser = null }) { Text("إلغاء", color = TextSecondarySlate) } }
        )
    }
    editCommissionMerchant?.let { merch ->
        var type by remember(merch.uid) { mutableStateOf(CommissionType.fromKey(merch.commissionType)) }
        var valText by remember(merch.uid) { mutableStateOf(merch.commissionValue.toString()) }
        AlertDialog(
            onDismissRequest = { editCommissionMerchant = null },
            containerColor = BgWhite,
            shape = TwinCashCardShape,
            title = { Text("عمولة ${merch.fullName}", color = NavyPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommissionType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.arabicLabel) }, colors = twinCashChipColors())
                        }
                    }
                    OutlinedTextField(
                        value = valText,
                        onValueChange = { valText = it },
                        label = { Text("القيمة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMerchantCommission(merch.uid, type, valText.toDoubleOrNull() ?: 0.0)
                        editCommissionMerchant = null
                    },
                    colors = twinCashPrimaryButtonColors(),
                    shape = TwinCashButtonShape
                ) { Text("حفظ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editCommissionMerchant = null }) { Text("إلغاء", color = TextSecondarySlate) } }
        )
    }
}

@Composable
private fun AdminQuickStat(label: String, value: String, color: Color) {
    TwinCashCard(modifier = Modifier.width(136.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun AdminSettingsCard(
    settings: SettingsEntity,
    onSave: (phone: String, whatsapp: String, threshold: Int, discountPct: Double) -> Unit
) {
    var phone by remember(settings.agencyPhone) { mutableStateOf(settings.agencyPhone) }
    var wa by remember(settings.whatsappNumber) { mutableStateOf(settings.whatsappNumber) }
    var threshold by remember(settings.loyaltyThreshold) { mutableStateOf(settings.loyaltyThreshold.toString()) }
    var discount by remember(settings.loyaltyDiscountPercent) { mutableStateOf(settings.loyaltyDiscountPercent.toString()) }

    TwinCashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("إعدادات الوكالة ونظام الولاء \"فابور\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الوكالة للدفع") }, singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = wa, onValueChange = { wa = it }, label = { Text("رقم الواتساب (بدون +)") }, singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("عدد الطلبات للولاء") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.weight(1f))
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("نسبة خصم الفابور %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.weight(1f))
            }
            Button(
                onClick = { onSave(phone, wa, threshold.toIntOrNull() ?: 4, discount.toDoubleOrNull() ?: 25.0) },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("حفظ الإعدادات", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CreateMerchantDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, email: String, whatsapp: String, type: CommissionType, value: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var commType by remember { mutableStateOf(CommissionType.PERCENTAGE) }
    var commValue by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text("إنشاء حساب تاجر جديد", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("يُنشأ التاجر بحالة \"قيد القبول\" حتى تفعّله من القائمة، ويدخل عبر حساب Google الخاص به.", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم الكامل") }, singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("بريد Google الخاص بالتاجر") }, singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("رقم الواتساب") }, singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommissionType.entries.forEach { t ->
                        FilterChip(selected = commType == t, onClick = { commType = t }, label = { Text(t.arabicLabel) }, colors = twinCashChipColors())
                    }
                }
                OutlinedTextField(value = commValue, onValueChange = { commValue = it }, label = { Text("قيمة العمولة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, email, whatsapp, commType, commValue.toDoubleOrNull() ?: 0.0) },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape
            ) {
                Text("إنشاء", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondarySlate) } }
    )
}

@Composable
private fun OfferEditorDialog(
    initial: OfferEntity?,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, desc: String, price: Double, orig: Double, days: Int, active: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var origPrice by remember { mutableStateOf(initial?.originalPrice?.toString() ?: "") }
    var days by remember { mutableStateOf("7") }
    var active by remember { mutableStateOf(initial?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text(if (initial == null) "إضافة عرض Free Fire" else "تعديل العرض", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان العرض") }, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("الوصف") }, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("سعر العرض (د.م)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = origPrice, onValueChange = { origPrice = it }, label = { Text("السعر قبل الخصم") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("مدة الصلاحية (أيام)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("العرض نشط", color = TextPrimaryDark)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val o = origPrice.toDoubleOrNull() ?: p
                    onSave(initial?.id, title, desc, p, o, days.toIntOrNull() ?: 7, active)
                },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape
            ) { Text("حفظ", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondarySlate) } }
    )
}
