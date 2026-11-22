package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.OrderEntity
import com.example.data.local.TopUpEntity
import com.example.data.model.ServiceCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.ServiceIconBadge
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
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.WarningBg
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors

@Composable
fun OrdersScreen(
    orders: List<OrderEntity>,
    topUps: List<TopUpEntity>,
    onOpenTopUp: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Orders, 1 = TopUps
    var statusFilter by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgWhite,
            contentColor = NavyPrimary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NavyPrimary,
                    height = 2.5.dp
                )
            },
            divider = { HorizontalDivider(color = BorderLight) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "طلباتي (${orders.size})",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) NavyPrimary else TextSecondarySlate
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "تعبئة الرصيد (${topUps.size})",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) NavyPrimary else TextSecondarySlate
                    )
                }
            )
        }

        if (selectedTab == 0) {
            val filters = listOf(null to "الكل", "pending" to "قيد الانتظار", "completed" to "مكتمل", "rejected" to "مرفوض")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) },
                        colors = twinCashChipColors()
                    )
                }
            }

            val visibleOrders = if (statusFilter == null) orders else orders.filter { it.status == statusFilter }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (visibleOrders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "لا توجد طلبات بعد",
                            subtitle = "جميع طلبات الخدمات والعروض ستظهر هنا مع حالتها الفورية"
                        )
                    }
                } else {
                    items(visibleOrders, key = { it.id }) { order ->
                        val cat = ServiceCategory.fromKey(order.serviceCategory)
                        TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ServiceIconBadge(category = cat, size = 42.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(order.serviceName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                            Text("${order.id} • ${order.packageLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                        }
                                    }
                                    OrderStatusPill(order.status)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الحساب: ${order.playerAccountInfo}", style = MaterialTheme.typography.bodySmall, color = NavyInteractive)
                                    Text(formatMad(order.finalPrice), fontWeight = FontWeight.Bold, color = NavyPrimary)
                                }

                                if (order.discountApplied > 0.0) {
                                    Text(
                                        text = "شمل خصم بقيمة ${formatMad(order.discountApplied)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen
                                    )
                                }

                                if (order.status == "rejected" && !order.rejectionReason.isNullOrBlank()) {
                                    Surface(
                                        color = ErrorBg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "سبب الرفض: ${order.rejectionReason} (تم إرجاع ${formatMad(order.finalPrice)} لرصيدك)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ErrorRed,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = formatShortDate(order.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondarySlate
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Button(
                        onClick = onOpenTopUp,
                        colors = twinCashPrimaryButtonColors(),
                        shape = TwinCashButtonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.AddCard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("طلب تعبئة رصيد جديد", fontWeight = FontWeight.Bold)
                    }
                }
                if (topUps.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.AddCard,
                            title = "لا توجد طلبات تعبئة رصيد بعد",
                            subtitle = "يمكنك إرسال طلب تعبئة الرصيد وسنراجعه في أقرب وقت",
                            actionLabel = "تعبئة الرصيد الآن",
                            onAction = onOpenTopUp
                        )
                    }
                } else {
                    items(topUps, key = { it.id }) { topUp ->
                        val (label, bg, fg) = when (topUp.status) {
                            "accepted" -> Triple("مقبول", SuccessBg, SuccessGreen)
                            "rejected" -> Triple("مرفوض", ErrorBg, ErrorRed)
                            else -> Triple("قيد الانتظار", WarningBg, WarningOrange)
                        }
                        TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formatMad(topUp.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    StatusPill(label = label, bgColor = bg, textColor = fg)
                                }
                                Text("${topUp.paymentMethod} • رقم العملية: ${topUp.transactionRef}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
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
                                if (topUp.status == "rejected" && !topUp.rejectionReason.isNullOrBlank()) {
                                    Text(
                                        text = "سبب الرفض: ${topUp.rejectionReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorRed
                                    )
                                }
                                Text(formatShortDate(topUp.createdAt), style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}
