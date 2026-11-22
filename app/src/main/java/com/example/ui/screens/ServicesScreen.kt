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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.ServiceEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.UserEntity
import com.example.data.model.PackageJsonCodec
import com.example.data.model.ServiceCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.ServiceOrderDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
import com.example.ui.theme.BgWhite
import com.example.ui.theme.ErrorBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SectionBg
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors

@Composable
fun ServicesScreen(
    user: UserEntity,
    settings: SettingsEntity,
    services: List<ServiceEntity>,
    initialCategory: ServiceCategory?,
    isBusy: Boolean,
    onValidateCode: (serviceCategory: String, code: String, basePrice: Double, (Result<Pair<DiscountCodeEntity, Double>>) -> Unit) -> Unit,
    onPlaceOrder: (serviceId: String, packageId: String, playerInfo: String, note: String, discountCode: String?, onDone: () -> Unit) -> Unit,
    onError: (String) -> Unit
) {
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var activeOrderService by remember { mutableStateOf<ServiceEntity?>(null) }

    val filtered = remember(services, selectedCategory) {
        if (selectedCategory == null) services else services.filter { it.category == selectedCategory?.key }
    }
    val loyaltyReady = user.loyaltyCount >= settings.loyaltyThreshold

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("الكل") },
                    colors = twinCashChipColors()
                )
            }
            items(ServiceCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.arabicTitle) },
                    colors = twinCashChipColors()
                )
            }
        }

        if (loyaltyReady) {
            TwinCashCard(
                containerColor = SuccessBg.copy(alpha = 0.4f),
                borderColor = SuccessGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "طلبك القادم بسعر الفابور! وصلت إلى ${user.loyaltyCount}/${settings.loyaltyThreshold} وسيتم خصم ${settings.loyaltyDiscountPercent.toInt()}% تلقائياً.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Storefront,
                        title = "لا توجد خدمات في هذا القسم حالياً",
                        subtitle = "ستظهر الخدمات هنا فور إضافتها من الإدارة"
                    )
                }
            } else {
                items(filtered, key = { it.id }) { service ->
                    val cat = ServiceCategory.fromKey(service.category)
                    val packages = remember(service.packagesJson) { PackageJsonCodec.fromJson(service.packagesJson) }
                    TwinCashCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ServiceIconBadge(category = cat, size = 48.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = service.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondarySlate
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPill(
                                    label = if (service.isAvailable) "متاحة" else "غير متاحة",
                                    bgColor = if (service.isAvailable) SuccessBg else ErrorBg,
                                    textColor = if (service.isAvailable) SuccessGreen else ErrorRed
                                )
                            }

                            if (packages.isEmpty()) {
                                Surface(
                                    color = SectionBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "لم تُضف باقات لهذه الخدمة بعد",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySlate,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    packages.take(3).forEach { pkg ->
                                        Surface(
                                            color = SectionBg,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(pkg.label, style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark)
                                                Text(formatMad(pkg.sellPrice), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                enabled = service.isAvailable && packages.isNotEmpty(),
                                onClick = { activeOrderService = service },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("order_service_btn_${service.id}")
                            ) {
                                val btnText = when {
                                    !service.isAvailable -> "الخدمة متوقفة حالياً"
                                    packages.isEmpty() -> "بانتظار إضافة الباقات"
                                    else -> "طلب هذه الخدمة (${packages.size} باقات)"
                                }
                                Text(text = btnText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
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
            onError = onError
        )
    }
}
