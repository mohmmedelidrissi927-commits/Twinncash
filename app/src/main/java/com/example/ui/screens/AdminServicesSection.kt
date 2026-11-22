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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import com.example.data.local.ServiceEntity
import com.example.data.model.PackageJsonCodec
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePackage
import com.example.ui.components.ServiceIconBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.TwinCashCard
import com.example.ui.components.formatMad
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
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors

@Composable
fun AdminServiceCard(
    service: ServiceEntity,
    onToggleAvailability: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = ServiceCategory.fromKey(service.category)
    val packages = remember(service.packagesJson) { PackageJsonCodec.fromJson(service.packagesJson) }

    TwinCashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    ServiceIconBadge(category = cat, size = 44.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(service.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("#${service.displayOrder}", style = MaterialTheme.typography.labelSmall, color = NavyInteractive)
                        }
                        Text(service.description, style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate, maxLines = 1)
                    }
                }
                StatusPill(
                    label = if (service.isAvailable) "متاحة" else "مخفية",
                    bgColor = if (service.isAvailable) SuccessBg else ErrorBg,
                    textColor = if (service.isAvailable) SuccessGreen else ErrorRed
                )
            }

            if (packages.isEmpty()) {
                Surface(
                    color = SectionBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد باقات مضافة بعد — اضغط على أيقونة التعديل لإضافة الباقات والأسعار",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                packages.forEach { pkg ->
                    Surface(
                        color = SectionBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pkg.label, style = MaterialTheme.typography.bodySmall, color = TextPrimaryDark, modifier = Modifier.weight(1f))
                            Text(
                                text = "مورد: ${formatMad(pkg.supplierCost)} | بيع: ${formatMad(pkg.sellPrice)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (service.isAvailable) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = if (service.isAvailable) SuccessGreen else TextSecondarySlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (service.isAvailable) "ظاهرة للزبائن" else "مخفية مؤقتاً",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondarySlate
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = service.isAvailable,
                        onCheckedChange = onToggleAvailability,
                        modifier = Modifier.testTag("toggle_service_vis_${service.id}")
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_service_${service.id}")) {
                        Icon(Icons.Outlined.Edit, contentDescription = "تعديل الخدمة", tint = NavyPrimary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_service_${service.id}")) {
                        Icon(Icons.Outlined.Delete, contentDescription = "حذف الخدمة", tint = ErrorRed)
                    }
                }
            }
        }
    }
}

private data class EditablePkgRow(
    val id: String,
    var label: String,
    var quantityLabel: String,
    var supplierCost: String,
    var sellPrice: String
)

@Composable
fun AdminServiceEditorDialog(
    initial: ServiceEntity?,
    defaultOrder: Int,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        category: ServiceCategory,
        name: String,
        desc: String,
        packages: List<ServicePackage>,
        available: Boolean,
        displayOrder: Int
    ) -> Unit
) {
    var category by remember { mutableStateOf(initial?.let { ServiceCategory.fromKey(it.category) } ?: ServiceCategory.FREE_FIRE) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var orderText by remember { mutableStateOf((initial?.displayOrder ?: defaultOrder).toString()) }
    var available by remember { mutableStateOf(initial?.isAvailable ?: true) }

    val initialPkgs = remember(initial) {
        val parsed = initial?.let { PackageJsonCodec.fromJson(it.packagesJson) }.orEmpty()
        if (parsed.isEmpty()) {
            listOf(
                EditablePkgRow(
                    id = "pkg_${System.currentTimeMillis()}",
                    label = "",
                    quantityLabel = "",
                    supplierCost = "",
                    sellPrice = ""
                )
            )
        } else {
            parsed.map {
                EditablePkgRow(
                    id = it.id,
                    label = it.label,
                    quantityLabel = it.quantityLabel,
                    supplierCost = it.supplierCost.toString(),
                    sellPrice = it.sellPrice.toString()
                )
            }
        }
    }
    val pkgRows = remember { mutableStateListOf<EditablePkgRow>().apply { addAll(initialPkgs) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text(if (initial == null) "إضافة خدمة جديدة" else "تعديل الخدمة والباقات", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الفئة والأيقونة:", style = MaterialTheme.typography.labelMedium, color = NavyInteractive)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ServiceCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortBadge) },
                            colors = twinCashChipColors()
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الخدمة") },
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف المختصر") },
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it },
                    label = { Text("ترتيب العرض") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الباقات (${pkgRows.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    OutlinedButton(
                        onClick = {
                            pkgRows.add(
                                EditablePkgRow(
                                    id = "pkg_${System.currentTimeMillis()}_${pkgRows.size}",
                                    label = "",
                                    quantityLabel = "",
                                    supplierCost = "",
                                    sellPrice = ""
                                )
                            )
                        },
                        shape = TwinCashButtonShape,
                        border = TwinCashSecondaryButtonBorder,
                        colors = twinCashSecondaryButtonColors()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة باقة")
                    }
                }

                pkgRows.forEachIndexed { idx, row ->
                    Surface(
                        color = SectionBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("باقة #${idx + 1}", style = MaterialTheme.typography.labelMedium, color = NavyPrimary, fontWeight = FontWeight.Bold)
                                if (pkgRows.size > 1) {
                                    IconButton(
                                        onClick = { pkgRows.removeAt(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "حذف باقة", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = row.label,
                                onValueChange = { pkgRows[idx] = row.copy(label = it) },
                                label = { Text("اسم الباقة (مثال: 520 جوهرة)") },
                                singleLine = true,
                                shape = TwinCashInputShape,
                                colors = twinCashTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = row.quantityLabel,
                                onValueChange = { pkgRows[idx] = row.copy(quantityLabel = it) },
                                label = { Text("الكمية") },
                                singleLine = true,
                                shape = TwinCashInputShape,
                                colors = twinCashTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = row.supplierCost,
                                    onValueChange = { pkgRows[idx] = row.copy(supplierCost = it) },
                                    label = { Text("سعر المورد") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = TwinCashInputShape,
                                    colors = twinCashTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = row.sellPrice,
                                    onValueChange = { pkgRows[idx] = row.copy(sellPrice = it) },
                                    label = { Text("سعر البيع") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = TwinCashInputShape,
                                    colors = twinCashTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("الخدمة متاحة للزبائن", color = TextPrimaryDark)
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mapped = pkgRows.mapNotNull { row ->
                        val sell = row.sellPrice.toDoubleOrNull()
                        val cost = row.supplierCost.toDoubleOrNull()
                        if (row.label.isBlank() || sell == null || cost == null) null
                        else ServicePackage(
                            id = row.id,
                            label = row.label.trim(),
                            quantityLabel = row.quantityLabel.trim().ifBlank { row.label.trim() },
                            supplierCost = cost,
                            sellPrice = sell
                        )
                    }
                    onSave(
                        initial?.id,
                        category,
                        name,
                        desc,
                        mapped,
                        available,
                        orderText.toIntOrNull() ?: defaultOrder
                    )
                },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape,
                modifier = Modifier.testTag("save_service_confirm_btn")
            ) { Text("حفظ الخدمة", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondarySlate) } }
    )
}
