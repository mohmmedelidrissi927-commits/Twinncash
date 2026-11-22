package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OrderEntity
import com.example.data.model.DailyProfitPoint
import com.example.data.model.MerchantProfitItem
import com.example.data.model.ProfitPeriod
import com.example.data.model.ProfitSummary
import com.example.data.model.ServiceProfitItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProfitLineChartCard
import com.example.ui.components.ServiceBarChartCard
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.NavyInteractive
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SectionBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors

@Composable
fun AdminProfitsScreen(
    orders: List<OrderEntity>,
    selectedPeriod: ProfitPeriod,
    summary: ProfitSummary,
    serviceProfits: List<ServiceProfitItem>,
    merchantProfits: List<MerchantProfitItem>,
    dailySeries: List<DailyProfitPoint>,
    onSelectPeriod: (ProfitPeriod) -> Unit,
    onBuildCsv: (List<OrderEntity>) -> String,
    onToast: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val csvContent = remember(orders) { onBuildCsv(orders) }

    val csvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                onToast("تم تصدير تقرير الأرباح بنجاح (CSV / Excel)", false)
            } catch (_: Exception) {
                onToast("تعذر حفظ ملف التقرير", true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لوحة الأرباح",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Button(
                    onClick = { csvExporter.launch("twincash_profits_${System.currentTimeMillis()}.csv") },
                    colors = twinCashPrimaryButtonColors(),
                    shape = TwinCashButtonShape,
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير CSV", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProfitPeriod.entries) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(period.arabicLabel) },
                        colors = twinCashChipColors()
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfitKpiCard(
                    title = "إجمالي المبيعات (${selectedPeriod.arabicLabel})",
                    value = formatMad(summary.totalSales),
                    subtitle = "${summary.completedCount} عملية مكتملة",
                    accentColor = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                ProfitKpiCard(
                    title = "صافي الربح",
                    value = formatMad(summary.netProfit),
                    subtitle = "بعد خصم المورد والعمولة",
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfitKpiCard(
                    title = "اليوم",
                    value = formatMad(summary.todayProfit),
                    subtitle = "صافي",
                    accentColor = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                ProfitKpiCard(
                    title = "الأسبوع",
                    value = formatMad(summary.weekProfit),
                    subtitle = "صافي",
                    accentColor = NavyInteractive,
                    modifier = Modifier.weight(1f)
                )
                ProfitKpiCard(
                    title = "الشهر",
                    value = formatMad(summary.monthProfit),
                    subtitle = "صافي",
                    accentColor = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TwinCashCard(containerColor = SectionBg, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("سعر الشراء من المورد", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        Text(formatMad(summary.totalSupplierCost), fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("عمولات التجار", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                        Text(formatMad(summary.totalMerchantCommission), fontWeight = FontWeight.Bold, color = NavyInteractive)
                    }
                }
            }
        }

        item {
            ProfitLineChartCard(points = dailySeries)
        }

        item {
            ServiceBarChartCard(items = serviceProfits)
        }

        item {
            Text("أرباح كل خدمة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
        }

        if (serviceProfits.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.BarChart,
                    title = "لا توجد أرباح خدمات بعد",
                    subtitle = "ستظهر إحصائيات كل خدمة فور إتمام الطلبات"
                )
            }
        } else {
            item {
                TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        serviceProfits.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    ServiceIconBadge(category = item.category, size = 40.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.serviceName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Text("${item.ordersCount} طلبات • مبيعات: ${formatMad(item.totalSales)}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                    }
                                }
                                Text(formatMad(item.netProfit), fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            if (idx < serviceProfits.lastIndex) {
                                HorizontalDivider(color = BorderLight)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("أرباح وأداء كل تاجر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
        }

        if (merchantProfits.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Storefront,
                    title = "لا توجد عمليات تجار مكتملة بعد",
                    subtitle = "ستظهر تقارير أداء التجار وعمولاتهم هنا"
                )
            }
        } else {
            item {
                TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        merchantProfits.forEachIndexed { idx, m ->
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(m.merchantName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    Text("${m.ordersCount} طلبات", style = MaterialTheme.typography.bodySmall, color = NavyInteractive)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المبيعات: ${formatMad(m.totalSales)}", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                    Text("العمولة: ${formatMad(m.totalCommission)}", style = MaterialTheme.typography.bodySmall, color = NavyPrimary)
                                    Text("الربح: ${formatMad(m.netProfitGenerated)}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (idx < merchantProfits.lastIndex) {
                                HorizontalDivider(color = BorderLight)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun ProfitKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    TwinCashCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate, maxLines = 1)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate, maxLines = 1)
        }
    }
}
