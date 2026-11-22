package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ContestEntity
import com.example.data.local.OfferEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ServiceEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.TopUpEntity
import com.example.data.local.UserEntity
import com.example.data.model.PackageJsonCodec
import com.example.data.model.ServiceCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.NavyInteractive
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySoftContainer
import com.example.ui.theme.SectionBg
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.TwinCashSecondaryButtonBorder
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors

@Composable
fun HomeScreen(
    user: UserEntity,
    settings: SettingsEntity,
    services: List<ServiceEntity>,
    offers: List<OfferEntity>,
    contests: List<ContestEntity>,
    recentOrders: List<OrderEntity>,
    unreadTopUps: List<TopUpEntity>,
    unreadOrders: List<OrderEntity>,
    onNavigateToServices: (ServiceCategory?) -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onOpenTopUp: () -> Unit,
    onDismissNotifications: () -> Unit
) {
    val context = LocalContext.current
    val loyaltyMax = settings.loyaltyThreshold.coerceAtLeast(1)
    val loyaltyProgress = (user.loyaltyCount.toFloat() / loyaltyMax.toFloat()).coerceIn(0f, 1f)
    val now = System.currentTimeMillis()
    val wonUnnotified = contests.filter { it.winnerCustomerId == user.uid && it.isPrizeApproved && !it.isWinnerNotified }
    val availableServices = services.filter { it.isAvailable }
    val serviceRows = availableServices.chunked(2)
    val activeOffers = offers.filter { it.isActive && it.expiresAt > now }
    val activeContest = contests.firstOrNull { it.status == "active" && it.endsAt > now }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (wonUnnotified.isNotEmpty()) {
            item {
                TwinCashCard(
                    containerColor = SuccessBg.copy(alpha = 0.4f),
                    borderColor = SuccessGreen
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("مبروك! ربحت في المسابقة", fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Text(
                                text = "إخفاء",
                                style = MaterialTheme.typography.labelMedium,
                                color = NavyPrimary,
                                modifier = Modifier.clickable(onClick = onDismissNotifications)
                            )
                        }
                        wonUnnotified.forEach { c ->
                            Text(
                                text = "• ${c.title}: تم اعتماد جائزتك (${c.prizeLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        if (unreadOrders.isNotEmpty() || unreadTopUps.isNotEmpty()) {
            item {
                TwinCashCard(
                    containerColor = NavySoftContainer,
                    borderColor = NavyPrimary
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحديثات فورية لطلباتك", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                            Text(
                                text = "تمت القراءة",
                                style = MaterialTheme.typography.labelMedium,
                                color = NavyInteractive,
                                modifier = Modifier.clickable(onClick = onDismissNotifications)
                            )
                        }
                        unreadOrders.take(2).forEach { ord ->
                            Text(
                                text = "• الطلب ${ord.id} (${ord.serviceName}): ${if (ord.status == "completed") "مكتمل" else "مرفوض — ${ord.rejectionReason ?: "تم استرجاع الرصيد"}"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryDark
                            )
                        }
                        unreadTopUps.take(2).forEach { top ->
                            Text(
                                text = "• تعبئة ${top.amount} د.م: ${if (top.status == "accepted") "تمت الموافقة وإضافة الرصيد" else "مرفوضة — ${top.rejectionReason ?: ""}"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        // Clean White Greeting Card
        item {
            TwinCashCard(containerColor = SectionBg) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "مرحباً، ${user.fullName}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "اختر خدمتك المفضلة أو قم بتعبئة رصيدك للطلب الفوري.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToServices(null) },
                            colors = twinCashPrimaryButtonColors(),
                            shape = TwinCashButtonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_order_service_btn")
                        ) {
                            Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اطلب خدمة", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenTopUp,
                            shape = TwinCashButtonShape,
                            border = TwinCashSecondaryButtonBorder,
                            colors = twinCashSecondaryButtonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_topup_btn")
                        ) {
                            Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعبئة الرصيد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Balance & Loyalty Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TwinCashCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("رصيدك المتاح", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMad(user.balance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "رقم الوكالة: ${settings.agencyPhone}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondarySlate
                        )
                    }
                }

                TwinCashCard(
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToOffers
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مكافأة الولاء", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${user.loyaltyCount} / $loyaltyMax",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (user.loyaltyCount >= loyaltyMax) SuccessGreen else NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { loyaltyProgress },
                            color = if (user.loyaltyCount >= loyaltyMax) SuccessGreen else NavyPrimary,
                            trackColor = SectionBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }

        // Services Grid Section (2 Columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الخدمات المتاحة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text(
                    text = "عرض الكل",
                    style = MaterialTheme.typography.labelMedium,
                    color = NavyInteractive,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToServices(null) }
                )
            }
        }

        if (availableServices.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Bolt,
                    title = "لا توجد خدمات مفعلة حالياً",
                    subtitle = "ستظهر الخدمات هنا فور تفعيلها من طرف الإدارة"
                )
            }
        } else {
            items(serviceRows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { service ->
                        val cat = ServiceCategory.fromKey(service.category)
                        val pkgs = PackageJsonCodec.fromJson(service.packagesJson)
                        val minPrice = pkgs.minOfOrNull { it.sellPrice }
                        TwinCashCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToServices(cat) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ServiceIconBadge(category = cat, size = 44.dp)
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (minPrice != null) "ابتداءً من ${formatMad(minPrice)}" else "لم تُضف باقات بعد",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (minPrice != null) NavyInteractive else TextSecondarySlate
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Offers Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("العروض الحصرية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text(
                    text = "صفحة العروض",
                    style = MaterialTheme.typography.labelMedium,
                    color = NavyInteractive,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onNavigateToOffers)
                )
            }
        }

        if (activeOffers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.LocalOffer,
                    title = "لا توجد عروض نشطة حالياً",
                    subtitle = "ترقبوا عروض Free Fire والخصومات الحصرية قريباً"
                )
            }
        } else {
            items(activeOffers.take(2), key = { it.id }) { offer ->
                TwinCashCard(onClick = onNavigateToOffers) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("عرض Free Fire", style = MaterialTheme.typography.labelSmall, color = NavyInteractive, fontWeight = FontWeight.SemiBold)
                            Text(offer.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(
                                text = "فقط ${formatMad(offer.price)} بدلاً من ${formatMad(offer.originalPrice)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        ServiceIconBadge(category = ServiceCategory.FREE_FIRE, size = 46.dp)
                    }
                }
            }
        }

        if (activeContest != null) {
            item {
                TwinCashCard(
                    containerColor = SectionBg,
                    onClick = onNavigateToAccount
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("مسابقة TwinCash جارية الآن", style = MaterialTheme.typography.labelSmall, color = NavyInteractive, fontWeight = FontWeight.SemiBold)
                            Text(activeContest.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("الجائزة: ${activeContest.prizeLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        }
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }

        // Recent Orders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("آخر طلباتك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text(
                    text = "سجل الطلبات",
                    style = MaterialTheme.typography.labelMedium,
                    color = NavyInteractive,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onNavigateToOrders)
                )
            }
        }

        if (recentOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "لا توجد طلبات بعد",
                    subtitle = "ابدأ بطلب أول خدمة من قسم الخدمات المتاحة"
                )
            }
        } else {
            items(recentOrders.take(3), key = { it.id }) { order ->
                TwinCashCard {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.serviceName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("${order.packageLabel} • ${order.playerAccountInfo}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            OrderStatusPill(order.status)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatMad(order.finalPrice), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }
            }
        }

        // Direct WhatsApp Support Card
        item {
            TwinCashCard(
                containerColor = SectionBg,
                onClick = {
                    openWhatsApp(context, settings.whatsappNumber, "مرحباً وكالة TwinCash، أحتاج استفساراً.")
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الدعم المباشر عبر واتساب", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Text("تواصل مع وكالة TwinCash (${settings.agencyPhone})", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                    }
                }
            }
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
