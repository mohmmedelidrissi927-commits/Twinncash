package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ContestEntity
import com.example.data.local.ContestEntryEntity
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.OrderEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.ContestPrizeType
import com.example.data.model.ContestStatus
import com.example.data.model.DiscountType
import com.example.data.model.ServiceCategory
import com.example.ui.components.OrderStatusPill
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
import com.example.ui.theme.NavySoftContainer
import com.example.ui.theme.SectionBg
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.TwinCashCardShape
import com.example.ui.theme.TwinCashInputShape
import com.example.ui.theme.TwinCashSecondaryButtonBorder
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors

@Composable
fun AdminCustomerCard(
    customer: UserEntity,
    ordersCount: Int,
    loyaltyThreshold: Int,
    onViewDetails: () -> Unit,
    onAdjustBalance: () -> Unit,
    onToggleBan: () -> Unit,
    onGrantCode: () -> Unit
) {
    val isClosed = customer.status == AccountStatus.CLOSED.key
    val isLoyaltyReady = customer.loyaltyCount >= loyaltyThreshold

    TwinCashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(customer.fullName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        StatusPill(
                            label = if (isClosed) "محظور" else "نشط",
                            bgColor = if (isClosed) ErrorBg else SuccessBg,
                            textColor = if (isClosed) ErrorRed else SuccessGreen
                        )
                    }
                    Text("${customer.email} • ${customer.whatsapp}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                }
                Text(
                    text = formatMad(customer.balance),
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    label = "الطلبات: $ordersCount",
                    bgColor = SectionBg,
                    textColor = NavyPrimary
                )
                StatusPill(
                    label = "الولاء: ${customer.loyaltyCount}/$loyaltyThreshold" + if (isLoyaltyReady) " (فابور جاهز)" else "",
                    bgColor = if (isLoyaltyReady) SuccessBg else NavySoftContainer,
                    textColor = if (isLoyaltyReady) SuccessGreen else NavyPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = TwinCashButtonShape,
                    border = TwinCashSecondaryButtonBorder,
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier.weight(1f).testTag("customer_details_${customer.uid}")
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("التفاصيل", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAdjustBalance,
                    shape = TwinCashButtonShape,
                    border = TwinCashSecondaryButtonBorder,
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier.weight(1f).testTag("customer_balance_${customer.uid}")
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الرصيد", maxLines = 1)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onGrantCode,
                    shape = TwinCashButtonShape,
                    border = TwinCashSecondaryButtonBorder,
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier.weight(1f).testTag("customer_grant_code_${customer.uid}")
                ) {
                    Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("منح كود", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onToggleBan,
                    shape = TwinCashButtonShape,
                    border = BorderStroke(1.dp, if (isClosed) SuccessGreen else ErrorRed),
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier.weight(1f).testTag("customer_ban_${customer.uid}")
                ) {
                    Icon(
                        Icons.Outlined.Block,
                        contentDescription = null,
                        tint = if (isClosed) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isClosed) "رفع الحظر" else "حظر",
                        color = if (isClosed) SuccessGreen else ErrorRed,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: Int,
    onSelectFilter: (Int) -> Unit
) {
    val filters = listOf("الكل", "نشط", "محظور", "مؤهل للفابور")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("بحث بالاسم، الإيميل أو الواتساب") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = NavyPrimary) },
            singleLine = true,
            shape = TwinCashInputShape,
            colors = twinCashTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("admin_customer_search")
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters.indices.toList()) { idx ->
                FilterChip(
                    selected = selectedFilter == idx,
                    onClick = { onSelectFilter(idx) },
                    label = { Text(filters[idx]) },
                    colors = twinCashChipColors()
                )
            }
        }
    }
}

@Composable
fun CustomerDetailsDialog(
    customer: UserEntity,
    orders: List<OrderEntity>,
    codes: List<DiscountCodeEntity>,
    settings: SettingsEntity,
    onDismiss: () -> Unit
) {
    val custOrders = remember(orders, customer.uid) { orders.filter { it.customerId == customer.uid } }
    val custCodes = remember(codes, customer.uid) {
        codes.filter { it.isActive && (it.isPublic || it.targetCustomerId == customer.uid) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text("ملف الزبون: ${customer.fullName}", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الإيميل: ${customer.email}", style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark)
                Text("الواتساب: ${customer.whatsapp}", style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark)
                Text("الرصيد: ${formatMad(customer.balance)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text("عدّاد الولاء: ${customer.loyaltyCount}/${settings.loyaltyThreshold}", style = MaterialTheme.typography.bodySmall, color = NavyInteractive)

                Spacer(modifier = Modifier.height(4.dp))
                Text("الأكواد المتاحة (${custCodes.size}):", fontWeight = FontWeight.Bold, color = NavyPrimary)
                if (custCodes.isEmpty()) {
                    Text("لا توجد أكواد مخصصة حالياً", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                } else {
                    custCodes.forEach { dc ->
                        Text("• ${dc.code} (${if (dc.isPublic) "عام" else "خاص"})", style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("آخر الطلبات (${custOrders.size}):", fontWeight = FontWeight.Bold, color = NavyPrimary)
                if (custOrders.isEmpty()) {
                    Text("لم يقم بأي طلب بعد", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                } else {
                    custOrders.take(5).forEach { ord ->
                        Surface(
                            color = SectionBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${ord.serviceName} (${ord.packageLabel})", style = MaterialTheme.typography.labelMedium, color = TextPrimaryDark)
                                    Text(formatMad(ord.finalPrice), style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                                }
                                OrderStatusPill(ord.status)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", color = NavyPrimary, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun AdminDiscountCodeCard(
    code: DiscountCodeEntity,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val valLabel = if (code.discountType == DiscountType.PERCENTAGE.key) "${code.discountValue.toInt()}%" else formatMad(code.discountValue)
    val scopeLabel = if (code.isPublic) "عام لكل الزبائن" else "خاص بـ: ${code.targetCustomerName ?: "زبون محدد"}"
    val catsLabel = if (code.includedCategoriesCsv == "ALL") "كل الخدمات" else code.includedCategoriesCsv

    TwinCashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(code.code, fontWeight = FontWeight.Bold, color = NavyPrimary, style = MaterialTheme.typography.titleMedium)
                    StatusPill(
                        label = valLabel,
                        bgColor = NavySoftContainer,
                        textColor = NavyPrimary
                    )
                }
                Switch(checked = code.isActive, onCheckedChange = onToggleActive)
            }
            Text("$scopeLabel • الفئات: $catsLabel", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("الاستعمال: ${code.usedCount}/${code.maxUses} • ينتهي: ${formatShortDate(code.expiresAt)}", style = MaterialTheme.typography.labelSmall, color = NavyInteractive)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "تعديل الكود", tint = NavyPrimary) }
                    IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "حذف الكود", tint = ErrorRed) }
                }
            }
        }
    }
}

@Composable
fun DiscountCodeEditorDialog(
    initial: DiscountCodeEntity?,
    preselectedCustomer: UserEntity?,
    customers: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        code: String,
        type: DiscountType,
        value: Double,
        categoriesCsv: String,
        daysValid: Int,
        maxUses: Int,
        isPublic: Boolean,
        targetCustomerId: String?,
        isActive: Boolean
    ) -> Unit
) {
    var codeText by remember { mutableStateOf(initial?.code ?: "") }
    var discountType by remember { mutableStateOf(initial?.let { DiscountType.fromKey(it.discountType) } ?: DiscountType.PERCENTAGE) }
    var discountVal by remember { mutableStateOf(initial?.discountValue?.toString() ?: "10") }
    var allCategories by remember { mutableStateOf(initial == null || initial.includedCategoriesCsv == "ALL") }
    val selectedCats = remember {
        mutableStateListOf<String>().apply {
            if (initial != null && initial.includedCategoriesCsv != "ALL") {
                addAll(initial.includedCategoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
        }
    }
    var daysValid by remember { mutableStateOf("30") }
    var maxUses by remember { mutableStateOf((initial?.maxUses ?: if (preselectedCustomer != null) 1 else 50).toString()) }
    var isPublic by remember { mutableStateOf(if (preselectedCustomer != null) false else (initial?.isPublic ?: true)) }
    var targetCustomerId by remember { mutableStateOf(preselectedCustomer?.uid ?: initial?.targetCustomerId ?: customers.firstOrNull()?.uid) }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text(if (initial == null) "إنشاء كود خصم" else "تعديل كود الخصم", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it.uppercase() },
                    label = { Text("نص الكود") },
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("discount_code_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscountType.entries.forEach { dt ->
                        FilterChip(
                            selected = discountType == dt,
                            onClick = { discountType = dt },
                            label = { Text(dt.arabicLabel) },
                            colors = twinCashChipColors()
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountVal,
                        onValueChange = { discountVal = it },
                        label = { Text("قيمة الخصم") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxUses,
                        onValueChange = { maxUses = it },
                        label = { Text("أقصى استعمال") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = daysValid,
                    onValueChange = { daysValid = it },
                    label = { Text("الصلاحية (أيام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شامل لجميع الخدمات", color = TextPrimaryDark)
                    Switch(checked = allCategories, onCheckedChange = { allCategories = it })
                }
                if (!allCategories) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ServiceCategory.entries) { cat ->
                            val picked = selectedCats.contains(cat.key)
                            FilterChip(
                                selected = picked,
                                onClick = { if (picked) selectedCats.remove(cat.key) else selectedCats.add(cat.key) },
                                label = { Text(cat.shortBadge) },
                                colors = twinCashChipColors()
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("عام لكل الزبائن", color = TextPrimaryDark)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                if (!isPublic && customers.isNotEmpty()) {
                    Text("اختر الزبون المستهدف:", style = MaterialTheme.typography.labelMedium, color = NavyInteractive)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(customers, key = { it.uid }) { c ->
                            FilterChip(
                                selected = targetCustomerId == c.uid,
                                onClick = { targetCustomerId = c.uid },
                                label = { Text(c.fullName) },
                                colors = twinCashChipColors()
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الكود مفعّل", color = TextPrimaryDark)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catsCsv = if (allCategories || selectedCats.isEmpty()) "ALL" else selectedCats.joinToString(",")
                    onSave(
                        initial?.id,
                        codeText,
                        discountType,
                        discountVal.toDoubleOrNull() ?: 0.0,
                        catsCsv,
                        daysValid.toIntOrNull() ?: 30,
                        maxUses.toIntOrNull() ?: 1,
                        isPublic,
                        if (isPublic) null else targetCustomerId,
                        isActive
                    )
                },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape,
                modifier = Modifier.testTag("save_discount_code_btn")
            ) { Text("حفظ الكود", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondarySlate) } }
    )
}

@Composable
fun AdminContestCard(
    contest: ContestEntity,
    participantsCount: Int,
    onOpenParticipants: () -> Unit,
    onApprovePrize: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    TwinCashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(contest.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                }
                StatusPill(
                    label = ContestStatus.fromKey(contest.status).arabicLabel,
                    bgColor = if (contest.status == "active") NavySoftContainer else SuccessBg,
                    textColor = if (contest.status == "active") NavyPrimary else SuccessGreen
                )
            }
            Text("الجائزة: ${contest.prizeLabel} • المشاركون: $participantsCount", style = MaterialTheme.typography.bodySmall, color = NavyInteractive)
            if (contest.winnerCustomerName != null) {
                Text(
                    text = "الفائز المختار: ${contest.winnerCustomerName} • " + if (contest.isPrizeApproved) "تم اعتماد الجائزة ✓" else "بانتظار اعتماد الجائزة",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (contest.isPrizeApproved) SuccessGreen else NavyPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onOpenParticipants,
                    shape = TwinCashButtonShape,
                    border = TwinCashSecondaryButtonBorder,
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier.weight(1f).testTag("contest_participants_${contest.id}")
                ) {
                    Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المشاركون والسحب")
                }
                if (contest.winnerCustomerId != null && !contest.isPrizeApproved) {
                    Button(
                        onClick = onApprovePrize,
                        colors = twinCashPrimaryButtonColors(),
                        shape = TwinCashButtonShape,
                        modifier = Modifier.weight(1f).testTag("approve_prize_${contest.id}")
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اعتماد الجائزة", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "تعديل المسابقة", tint = NavyPrimary) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "حذف المسابقة", tint = ErrorRed) }
            }
        }
    }
}

@Composable
fun ContestEditorDialog(
    initial: ContestEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        desc: String,
        prizeType: ContestPrizeType,
        prizeLabel: String,
        prizeAmount: Double,
        rules: String,
        days: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var prizeType by remember { mutableStateOf(initial?.let { ContestPrizeType.fromKey(it.prizeType) } ?: ContestPrizeType.BALANCE) }
    var prizeLabel by remember { mutableStateOf(initial?.prizeLabel ?: "") }
    var prizeAmount by remember { mutableStateOf((initial?.prizeAmount ?: 0.0).let { if (it == 0.0) "" else it.toString() }) }
    var rules by remember { mutableStateOf(initial?.rulesText ?: "مشاركة واحدة لكل زبون نشط.") }
    var days by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text(if (initial == null) "إضافة مسابقة جديدة" else "تعديل المسابقة", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان المسابقة") }, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("الوصف") }, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Text("نوع الجائزة:", style = MaterialTheme.typography.labelMedium, color = NavyInteractive)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ContestPrizeType.entries) { pt ->
                        FilterChip(selected = prizeType == pt, onClick = { prizeType = pt }, label = { Text(pt.arabicLabel) }, colors = twinCashChipColors())
                    }
                }
                OutlinedTextField(value = prizeLabel, onValueChange = { prizeLabel = it }, label = { Text("وصف الجائزة") }, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = prizeAmount,
                    onValueChange = { prizeAmount = it },
                    label = { Text("قيمة الجائزة (رصيد أو مبلغ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("شروط المشاركة") }, shape = TwinCashInputShape, colors = twinCashTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text("المدة (أيام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial?.id,
                        title,
                        desc,
                        prizeType,
                        prizeLabel,
                        prizeAmount.toDoubleOrNull() ?: 0.0,
                        rules,
                        days.toIntOrNull() ?: 7
                    )
                },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape
            ) { Text("حفظ المسابقة", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondarySlate) } }
    )
}

@Composable
fun ContestParticipantsDialog(
    contest: ContestEntity,
    entries: List<ContestEntryEntity>,
    onDismiss: () -> Unit,
    onRandomDraw: () -> Unit,
    onPickWinner: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text("مشاركو: ${contest.title}", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entries.isEmpty()) {
                    Text("لا يوجد مشاركون في هذه المسابقة حتى الآن.", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                } else {
                    Button(
                        onClick = onRandomDraw,
                        colors = twinCashPrimaryButtonColors(),
                        shape = TwinCashButtonShape,
                        modifier = Modifier.fillMaxWidth().testTag("random_draw_btn_${contest.id}")
                    ) {
                        Icon(Icons.Outlined.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سحب عشوائي للفائز", fontWeight = FontWeight.Bold)
                    }
                    Text("أو اختر الفائز يدوياً (${entries.size} مشارك):", style = MaterialTheme.typography.labelMedium, color = NavyInteractive)
                    entries.forEach { e ->
                        val isWinner = contest.winnerCustomerId == e.customerId
                        Surface(
                            color = if (isWinner) SuccessBg else SectionBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isWinner) SuccessGreen else BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(e.customerName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    Text(e.customerWhatsapp, style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)
                                }
                                if (isWinner) {
                                    StatusPill("الفائز ✓", SuccessBg, SuccessGreen)
                                } else {
                                    TextButton(onClick = { onPickWinner(e.customerId) }) {
                                        Text("تعيين فائز", color = NavyPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", color = NavyPrimary) }
        }
    )
}
