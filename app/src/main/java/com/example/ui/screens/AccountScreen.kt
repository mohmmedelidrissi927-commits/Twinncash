package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ContestEntity
import com.example.data.local.ContestEntryEntity
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ServiceEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.UserEntity
import com.example.data.model.ContestStatus
import com.example.data.model.DiscountType
import com.example.data.model.PackageJsonCodec
import com.example.data.model.ServiceCategory
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.ServiceOrderDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.components.formatShortDate
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
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
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors

@Composable
fun AccountScreen(
    user: UserEntity,
    settings: SettingsEntity,
    services: List<ServiceEntity>,
    discountCodes: List<DiscountCodeEntity>,
    contests: List<ContestEntity>,
    myContestEntries: List<ContestEntryEntity>,
    recentOrders: List<OrderEntity>,
    isBusy: Boolean,
    onUpdateProfile: (fullName: String, whatsapp: String, avatarUri: String?) -> Unit,
    onValidateCode: (serviceCategory: String, code: String, basePrice: Double, (Result<Pair<DiscountCodeEntity, Double>>) -> Unit) -> Unit,
    onPlaceOrder: (serviceId: String, packageId: String, playerInfo: String, note: String, discountCode: String?, onDone: () -> Unit) -> Unit,
    onJoinContest: (String) -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onOpenTopUp: () -> Unit,
    onNotify: (String, Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showEditDialog by remember { mutableStateOf(false) }
    var activeOrderService by remember { mutableStateOf<ServiceEntity?>(null) }

    val now = System.currentTimeMillis()
    val availableServices = remember(services) { services.filter { it.isAvailable } }
    val validCodes = remember(discountCodes, now) {
        discountCodes.filter { it.isActive && it.expiresAt > now && it.usedCount < it.maxUses }
    }
    val enteredContestIds = remember(myContestEntries) { myContestEntries.map { it.contestId }.toSet() }
    val threshold = settings.loyaltyThreshold.coerceAtLeast(1)
    val loyaltyProgress = (user.loyaltyCount.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // أ) معلوماتي — Customer Profile Card
        item {
            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("معلوماتي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp)) {
                            if (!user.avatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.avatarUri,
                                    contentDescription = "الصورة الرمزية",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(1.5.dp, NavyPrimary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_avatar_default),
                                    contentDescription = "الصورة الرمزية",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(1.5.dp, BorderLight, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Phone, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(user.whatsapp, style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            }
                        }
                    }

                    Surface(
                        color = SectionBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("الرصيد المتاح", color = TextSecondarySlate, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = formatMad(user.balance),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenTopUp,
                            colors = twinCashPrimaryButtonColors(),
                            shape = TwinCashButtonShape,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("account_topup_btn")
                        ) {
                            Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعبئة الرصيد", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            shape = TwinCashButtonShape,
                            border = TwinCashSecondaryButtonBorder,
                            colors = twinCashSecondaryButtonColors(),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("edit_profile_btn")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل البيانات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ب) خدماتي
        item {
            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("خدماتي المتاحة (${availableServices.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    if (availableServices.isEmpty()) {
                        Text("لا توجد خدمات متاحة حالياً", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(availableServices, key = { it.id }) { srv ->
                                val cat = ServiceCategory.fromKey(srv.category)
                                val hasPackages = PackageJsonCodec.fromJson(srv.packagesJson).isNotEmpty()
                                Surface(
                                    color = SectionBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier.width(156.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ServiceIconBadge(category = cat, size = 40.dp)
                                        Text(
                                            text = srv.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            maxLines = 1
                                        )
                                        Button(
                                            enabled = hasPackages,
                                            onClick = { activeOrderService = srv },
                                            colors = twinCashPrimaryButtonColors(),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("account_order_${srv.id}")
                                        ) {
                                            Text(
                                                text = if (hasPackages) "اطلب الآن" else "بدون باقات",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ج) الخصومات والأكواد
        item {
            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الخصومات والأكواد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    if (validCodes.isEmpty()) {
                        Text("لا توجد أكواد خصم متاحة لك حالياً", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                    } else {
                        validCodes.forEach { dc ->
                            val valueLabel = if (dc.discountType == DiscountType.PERCENTAGE.key) {
                                "${dc.discountValue.toInt()}%"
                            } else {
                                formatMad(dc.discountValue)
                            }
                            Surface(
                                color = SectionBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(dc.code, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                            StatusPill(
                                                label = if (dc.isPublic) "كود عام" else "كود خاص",
                                                bgColor = if (dc.isPublic) NavySoftContainer else SuccessBg,
                                                textColor = if (dc.isPublic) NavyPrimary else SuccessGreen
                                            )
                                        }
                                        Text(
                                            text = "خصم $valueLabel • المتبقي ${dc.maxUses - dc.usedCount} استخدام • ينتهي ${formatShortDate(dc.expiresAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondarySlate
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(dc.code))
                                            onNotify("تم نسخ كود الخصم ${dc.code}", false)
                                        }
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ الكود", tint = NavyPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // د) المسابقات
        item {
            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("المسابقات والجوائز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    if (contests.isEmpty()) {
                        Text("لا توجد مسابقات نشطة حالياً", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                    } else {
                        contests.forEach { contest ->
                            val iAmWinner = contest.winnerCustomerId == user.uid && contest.isPrizeApproved
                            val alreadyEntered = enteredContestIds.contains(contest.id)
                            val isActive = contest.status == ContestStatus.ACTIVE.key && now in contest.startsAt..contest.endsAt
                            Surface(
                                color = if (iAmWinner) SuccessBg.copy(alpha = 0.35f) else SectionBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (iAmWinner) SuccessGreen else BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(contest.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, modifier = Modifier.weight(1f))
                                        if (iAmWinner) {
                                            StatusPill("ربحت!", SuccessBg, SuccessGreen)
                                        } else {
                                            StatusPill(
                                                ContestStatus.fromKey(contest.status).arabicLabel,
                                                if (isActive) NavySoftContainer else BgWhite,
                                                if (isActive) NavyPrimary else TextSecondarySlate
                                            )
                                        }
                                    }
                                    Text(contest.description, style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                    Text("الجائزة: ${contest.prizeLabel}", style = MaterialTheme.typography.labelMedium, color = NavyInteractive, fontWeight = FontWeight.SemiBold)
                                    Text("الشروط: ${contest.rulesText}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                    Text("تنتهي: ${formatShortDate(contest.endsAt)}", style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)

                                    if (iAmWinner) {
                                        Text(
                                            text = "تهانينا! لقد ربحت في هذه المسابقة وتمت الموافقة على جائزتك.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SuccessGreen
                                        )
                                    } else if (isActive) {
                                        Button(
                                            enabled = !alreadyEntered && !isBusy,
                                            onClick = { onJoinContest(contest.id) },
                                            colors = if (alreadyEntered) ButtonDefaults.buttonColors(
                                                containerColor = BgWhite,
                                                contentColor = SuccessGreen,
                                                disabledContainerColor = BgWhite,
                                                disabledContentColor = SuccessGreen
                                            ) else twinCashPrimaryButtonColors(),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("join_contest_${contest.id}")
                                        ) {
                                            Text(
                                                text = if (alreadyEntered) "تمت المشاركة ✓" else "شارك في المسابقة",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // هـ) العروض والولاء
        item {
            TwinCashCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (user.loyaltyCount >= threshold) SuccessGreen else BorderLight,
                onClick = onNavigateToOffers
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("العروض والولاء \"فابور\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                        StatusPill(
                            label = "${user.loyaltyCount} / $threshold",
                            bgColor = if (user.loyaltyCount >= threshold) SuccessBg else NavySoftContainer,
                            textColor = if (user.loyaltyCount >= threshold) SuccessGreen else NavyPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { loyaltyProgress },
                        color = if (user.loyaltyCount >= threshold) SuccessGreen else NavyPrimary,
                        trackColor = SectionBg,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                    )
                    Text(
                        text = if (user.loyaltyCount >= threshold) {
                            "طلبك القادم بسعر الفابور! (خصم ${settings.loyaltyDiscountPercent.toInt()}% تلقائياً)"
                        } else {
                            "اضغط لاستكشاف عروض Free Fire ومتابعة تقدمك نحو الطلب الخامس المخفض."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.loyaltyCount >= threshold) SuccessGreen else TextSecondarySlate
                    )
                }
            }
        }

        // و) آخر طلباتي
        item {
            TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("آخر طلباتي (${recentOrders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Text(
                            text = "عرض الكل",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyInteractive,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onNavigateToOrders)
                        )
                    }
                    if (recentOrders.isEmpty()) {
                        Text("لم تقم بأي طلب بعد", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                    } else {
                        recentOrders.take(4).forEach { ord ->
                            Surface(
                                color = SectionBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ord.serviceName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Text("${ord.packageLabel} • ${formatMad(ord.finalPrice)}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                    }
                                    OrderStatusPill(ord.status)
                                }
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp + Logout
        item {
            TwinCashCard(
                containerColor = SectionBg,
                onClick = {
                    openWhatsApp(context, settings.whatsappNumber, "مرحباً وكالة TwinCash، معكم ${user.fullName}.")
                }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("تواصل مع الوكالة عبر واتساب", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Text("الرقم الرسمي: ${settings.agencyPhone}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                shape = TwinCashButtonShape,
                border = BorderStroke(1.dp, ErrorRed),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = BgWhite, contentColor = ErrorRed),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("account_logout_btn")
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    val serviceToOrder = activeOrderService
    if (serviceToOrder != null) {
        ServiceOrderDialog(
            service = serviceToOrder,
            user = user,
            settings = settings,
            isBusy = isBusy,
            onValidateCode = onValidateCode,
            onDismiss = { activeOrderService = null },
            onSubmit = { pkg, info, note, code ->
                onPlaceOrder(serviceToOrder.id, pkg.id, info, note, code) {
                    activeOrderService = null
                }
            },
            onError = { onNotify(it, true) }
        )
    }

    if (showEditDialog) {
        var newName by remember { mutableStateOf(user.fullName) }
        var newWhatsapp by remember { mutableStateOf(user.whatsapp) }
        var newAvatarUri by remember { mutableStateOf(user.avatarUri) }

        val avatarPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) newAvatarUri = uri.toString()
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = BgWhite,
            shape = TwinCashCardShape,
            title = { Text("تعديل بيانات الحساب", color = NavyPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("الاسم الكامل") },
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newWhatsapp,
                        onValueChange = { newWhatsapp = it },
                        label = { Text("رقم الواتساب") },
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = TwinCashButtonShape,
                        border = TwinCashSecondaryButtonBorder,
                        colors = twinCashSecondaryButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (newAvatarUri == null) "تغيير الصورة الرمزية" else "تم اختيار صورة جديدة ✓", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(newName, newWhatsapp, newAvatarUri)
                        showEditDialog = false
                    },
                    colors = twinCashPrimaryButtonColors(),
                    shape = TwinCashButtonShape
                ) { Text("حفظ", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("إلغاء", color = TextSecondarySlate) }
            }
        )
    }
}
