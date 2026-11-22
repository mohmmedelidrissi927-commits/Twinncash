package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.OfferEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.UserEntity
import com.example.data.model.ServiceCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HumanCheckWidget
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.components.formatShortDate
import com.example.ui.theme.BgWhite
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
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors

@Composable
fun OffersScreen(
    user: UserEntity,
    settings: SettingsEntity,
    offers: List<OfferEntity>,
    isBusy: Boolean,
    onPlaceOfferOrder: (offerId: String, playerId: String, note: String, onDone: () -> Unit) -> Unit,
    onError: (String) -> Unit
) {
    var selectedOffer by remember { mutableStateOf<OfferEntity?>(null) }
    val threshold = settings.loyaltyThreshold.coerceAtLeast(1)
    val progress = (user.loyaltyCount.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)
    val now = System.currentTimeMillis()
    val visibleOffers = offers.filter { it.isActive && it.expiresAt > now }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Minimal Header Card
        item {
            TwinCashCard(containerColor = NavyPrimary, borderColor = NavyPrimary) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "عروض Free Fire الحصرية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BgWhite
                        )
                        Text(
                            text = "باقات محدودة المدة للشحن المباشر عبر Player ID",
                            style = MaterialTheme.typography.bodySmall,
                            color = BgWhite.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Outlined.LocalOffer,
                        contentDescription = null,
                        tint = BgWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Loyalty Progress Tracker
        item {
            TwinCashCard(
                borderColor = if (user.loyaltyCount >= threshold) SuccessGreen else com.example.ui.theme.BorderLight
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "نظام الولاء \"فابور\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        StatusPill(
                            label = "${user.loyaltyCount} / $threshold",
                            bgColor = if (user.loyaltyCount >= threshold) SuccessBg else NavySoftContainer,
                            textColor = if (user.loyaltyCount >= threshold) SuccessGreen else NavyPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        color = if (user.loyaltyCount >= threshold) SuccessGreen else NavyPrimary,
                        trackColor = SectionBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Text(
                        text = if (user.loyaltyCount >= threshold) {
                            "طلبك القادم بسعر الفابور! سيتم تطبيق خصم ${settings.loyaltyDiscountPercent.toInt()}% تلقائياً في الشوب العادي."
                        } else {
                            "أكمل ${threshold - user.loyaltyCount} طلبات مكتملة للحصول على خصم ${settings.loyaltyDiscountPercent.toInt()}% على طلبك الخامس."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.loyaltyCount >= threshold) SuccessGreen else TextSecondarySlate
                    )
                }
            }
        }

        if (visibleOffers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.LocalOffer,
                    title = "لا توجد عروض نشطة حالياً",
                    subtitle = "ستظهر عروض Free Fire هنا فور إضافتها من الإدارة"
                )
            }
        } else {
            items(visibleOffers, key = { it.id }) { offer ->
                TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ServiceIconBadge(category = ServiceCategory.FREE_FIRE, size = 48.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(offer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text(offer.description, style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = formatMad(offer.price),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                if (offer.originalPrice > offer.price) {
                                    Text(
                                        text = formatMad(offer.originalPrice),
                                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                        color = TextSecondarySlate
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = NavyInteractive, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ينتهي: ${formatShortDate(offer.expiresAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondarySlate
                                )
                            }
                        }

                        Button(
                            onClick = { selectedOffer = offer },
                            colors = twinCashPrimaryButtonColors(),
                            shape = TwinCashButtonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("claim_offer_btn_${offer.id}")
                        ) {
                            Text("استفد من العرض الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    val offerToOrder = selectedOffer
    if (offerToOrder != null) {
        var playerId by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var humanVerified by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedOffer = null },
            containerColor = BgWhite,
            shape = TwinCashCardShape,
            title = { Text(offerToOrder.title, color = NavyPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "السعر: ${formatMad(offerToOrder.price)} | رصيدك: ${formatMad(user.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NavyInteractive,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = playerId,
                        onValueChange = { playerId = it },
                        label = { Text("معرف اللاعب (Free Fire ID)") },
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offer_player_id_input")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("ملاحظة (اختياري)") },
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    HumanCheckWidget(
                        verified = humanVerified,
                        onVerifyToggle = { humanVerified = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isBusy,
                    onClick = {
                        if (!humanVerified) {
                            onError("يرجى تأكيد التحقق البشري أولاً")
                        } else {
                            onPlaceOfferOrder(offerToOrder.id, playerId, note) {
                                selectedOffer = null
                            }
                        }
                    },
                    colors = twinCashPrimaryButtonColors(),
                    shape = TwinCashButtonShape,
                    modifier = Modifier.testTag("confirm_offer_order_btn")
                ) {
                    Text("تأكيد طلب العرض", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOffer = null }) {
                    Text("إلغاء", color = TextSecondarySlate)
                }
            }
        )
    }
}
