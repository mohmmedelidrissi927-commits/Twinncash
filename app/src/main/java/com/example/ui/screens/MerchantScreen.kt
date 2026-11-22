package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OrderEntity
import com.example.data.local.UserEntity
import com.example.data.model.CommissionType
import com.example.data.model.ServiceCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.RejectReasonDialog
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.components.formatShortDate
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyInteractive
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SectionBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.twinCashPrimaryButtonColors

@Composable
fun MerchantScreen(
    merchant: UserEntity,
    orders: List<OrderEntity>,
    isBusy: Boolean,
    onAcceptOrder: (String) -> Unit,
    onRejectOrder: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pending Queue, 1 = Processed by Me
    var rejectingOrderId by remember { mutableStateOf<String?>(null) }

    val pendingOrders = remember(orders) { orders.filter { it.status == "pending" } }
    val myHistory = remember(orders, merchant.uid) {
        orders.filter { it.merchantId == merchant.uid && it.status != "pending" }
    }
    val totalMyCommission = remember(myHistory) {
        myHistory.filter { it.status == "completed" }.sumOf { it.merchantCommission }
    }
    val commissionLabel = when (CommissionType.fromKey(merchant.commissionType)) {
        CommissionType.PERCENTAGE -> "${merchant.commissionValue}%"
        CommissionType.FIXED -> formatMad(merchant.commissionValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        TwinCashCard(
            containerColor = SectionBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لوحة التاجر: ${merchant.fullName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("نسبة/قيمة العمولة", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        Text(commissionLabel, fontWeight = FontWeight.Bold, color = NavyInteractive)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("طلبات قيد التنفيذ", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        Text("${pendingOrders.size}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("عمولاتي المكتسبة", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        Text(formatMad(totalMyCommission), fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
                Text(
                    text = "تنبيه: جميع عمليات القبول والرفض مسجلة في سجل النشاط الخاص بالإدارة.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondarySlate
                )
            }
        }

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
                        "طلبات جديدة (${pendingOrders.size})",
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
                        "الطلبات المنفذة (${myHistory.size})",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) NavyPrimary else TextSecondarySlate
                    )
                }
            )
        }

        val list = if (selectedTab == 0) pendingOrders else myHistory
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (list.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.ReceiptLong,
                        title = if (selectedTab == 0) "لا توجد طلبات معلقة حالياً" else "لم تقم بمعالجة أي طلبات بعد",
                        subtitle = if (selectedTab == 0) "ستظهر هنا طلبات الزبائن فور إرسالها" else "سيظهر سجل الطلبات المنفذة وعمولاتك هنا"
                    )
                }
            } else {
                items(list, key = { it.id }) { order ->
                    val cat = ServiceCategory.fromKey(order.serviceCategory)
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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    ServiceIconBadge(category = cat, size = 42.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("${order.serviceName} (${order.id})", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Text(order.packageLabel, style = MaterialTheme.typography.bodySmall, color = NavyInteractive)
                                    }
                                }
                                OrderStatusPill(order.status)
                            }

                            Text("الزبون: ${order.customerName} (${order.customerWhatsapp})", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            Text("بيانات الحساب: ${order.playerAccountInfo}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                            if (order.note.isNotBlank()) {
                                Text("ملاحظة: ${order.note}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("سعر البيع: ${formatMad(order.finalPrice)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text(formatShortDate(order.createdAt), style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)
                            }

                            if (order.status == "pending") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        enabled = !isBusy,
                                        onClick = { onAcceptOrder(order.id) },
                                        colors = twinCashPrimaryButtonColors(),
                                        shape = TwinCashButtonShape,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("merchant_accept_${order.id}")
                                    ) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("قبول وتنفيذ", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        enabled = !isBusy,
                                        onClick = { rejectingOrderId = order.id },
                                        shape = TwinCashButtonShape,
                                        border = BorderStroke(1.dp, ErrorRed),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = BgWhite, contentColor = ErrorRed),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("merchant_reject_${order.id}")
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = null, tint = ErrorRed)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("رفض", color = ErrorRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (order.status == "completed") {
                                Text(
                                    text = "عمولتك من هذا الطلب: ${formatMad(order.merchantCommission)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SuccessGreen
                                )
                            } else if (order.status == "rejected" && !order.rejectionReason.isNullOrBlank()) {
                                Text(
                                    text = "سبب الرفض: ${order.rejectionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    val rejId = rejectingOrderId
    if (rejId != null) {
        RejectReasonDialog(
            title = "رفض الطلب $rejId",
            onDismiss = { rejectingOrderId = null },
            onConfirm = { reason ->
                onRejectOrder(rejId, reason)
                rejectingOrderId = null
            }
        )
    }
}
