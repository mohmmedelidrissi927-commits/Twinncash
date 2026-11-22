package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyProfitPoint
import com.example.data.model.ServiceProfitItem
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.NavyChartLight
import com.example.ui.theme.NavyInteractive
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate

@Composable
fun ProfitLineChartCard(
    points: List<DailyProfitPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "منحنى المبيعات والأرباح (آخر 7 أيام)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = NavyChartLight, label = "المبيعات")
                LegendDot(color = NavyPrimary, label = "صافي الربح")
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (points.isEmpty() || points.all { it.sales == 0.0 && it.profit == 0.0 }) {
                Text(
                    text = "لا توجد مبيعات مكتملة بعد لعرض المنحنى",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val maxVal = (points.maxOfOrNull { maxOf(it.sales, it.profit) } ?: 1.0).coerceAtLeast(10.0)
                    val padX = 16f
                    val padY = 16f
                    val chartW = (size.width - padX * 2).coerceAtLeast(1f)
                    val chartH = (size.height - padY * 2).coerceAtLeast(1f)

                    for (i in 0..3) {
                        val y = padY + (chartH / 3f) * i
                        drawLine(
                            color = BorderLight,
                            start = Offset(padX, y),
                            end = Offset(size.width - padX, y),
                            strokeWidth = 1.5f
                        )
                    }

                    val stepX = if (points.size > 1) chartW / (points.size - 1) else chartW
                    val salesPath = Path()
                    val profitPath = Path()

                    points.forEachIndexed { idx, p ->
                        val x = padX + idx * stepX
                        val ySales = padY + chartH - ((p.sales / maxVal).toFloat() * chartH)
                        val yProfit = padY + chartH - ((p.profit / maxVal).toFloat() * chartH)
                        if (idx == 0) {
                            salesPath.moveTo(x, ySales)
                            profitPath.moveTo(x, yProfit)
                        } else {
                            salesPath.lineTo(x, ySales)
                            profitPath.lineTo(x, yProfit)
                        }
                    }

                    drawPath(
                        path = salesPath,
                        color = NavyChartLight,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = profitPath,
                        color = NavyPrimary,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )

                    points.forEachIndexed { idx, p ->
                        val x = padX + idx * stepX
                        val ySales = padY + chartH - ((p.sales / maxVal).toFloat() * chartH)
                        val yProfit = padY + chartH - ((p.profit / maxVal).toFloat() * chartH)
                        drawCircle(color = NavyChartLight, radius = 6f, center = Offset(x, ySales))
                        drawCircle(color = NavyPrimary, radius = 6f, center = Offset(x, yProfit))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach {
                        Text(
                            text = it.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondarySlate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondarySlate)
    }
}

@Composable
fun ServiceBarChartCard(
    items: List<ServiceProfitItem>,
    modifier: Modifier = Modifier
) {
    val barColors = listOf(NavyPrimary, NavyInteractive, NavyChartLight)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "مقارنة أرباح الخدمات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (items.isEmpty()) {
                Text(
                    text = "لا توجد عمليات مكتملة في الفترة المحددة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                val maxProfit = items.maxOfOrNull { it.netProfit }?.coerceAtLeast(1.0) ?: 1.0
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val count = items.size.coerceAtLeast(1)
                    val slotW = size.width / count
                    val barW = (slotW * 0.42f).coerceIn(18f, 56f)
                    val maxH = (size.height - 16f).coerceAtLeast(1f)

                    drawLine(
                        color = BorderLight,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )

                    items.forEachIndexed { idx, item ->
                        val ratio = (item.netProfit / maxProfit).toFloat().coerceIn(0.06f, 1f)
                        val barH = maxH * ratio
                        val left = idx * slotW + (slotW - barW) / 2f
                        val top = size.height - barH

                        drawRoundRect(
                            color = barColors[idx % barColors.size],
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    items.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = item.category.shortBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${item.netProfit.toInt()} د.م",
                                style = MaterialTheme.typography.labelSmall,
                                color = NavyInteractive
                            )
                        }
                    }
                }
            }
        }
    }
}
