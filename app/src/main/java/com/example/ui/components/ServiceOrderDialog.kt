package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.DiscountCodeEntity
import com.example.data.local.ServiceEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.UserEntity
import com.example.data.model.PackageJsonCodec
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePackage
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySoftContainer
import com.example.ui.theme.SectionBg
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
fun ServiceOrderDialog(
    service: ServiceEntity,
    user: UserEntity,
    settings: SettingsEntity,
    isBusy: Boolean,
    onValidateCode: (serviceCategory: String, code: String, basePrice: Double, (Result<Pair<DiscountCodeEntity, Double>>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (pkg: ServicePackage, info: String, note: String, discountCode: String?) -> Unit,
    onError: (String) -> Unit
) {
    val category = ServiceCategory.fromKey(service.category)
    val packages = remember(service.packagesJson) { PackageJsonCodec.fromJson(service.packagesJson) }
    if (packages.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = BgWhite,
            shape = TwinCashCardShape,
            title = { Text(service.name, color = NavyPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "لا توجد باقات متاحة لهذه الخدمة حالياً. ستتم إضافتها من طرف الإدارة قريباً.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate
                )
            },
            confirmButton = {
                Button(onClick = onDismiss, colors = twinCashPrimaryButtonColors(), shape = TwinCashButtonShape) {
                    Text("حسناً")
                }
            }
        )
        return
    }

    var selectedPkg by remember { mutableStateOf(packages.first()) }
    var accountInfo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var appliedCode by remember { mutableStateOf<String?>(null) }
    var codeDiscount by remember { mutableStateOf(0.0) }
    var codeFeedback by remember { mutableStateOf<String?>(null) }
    var codeIsError by remember { mutableStateOf(false) }
    var humanVerified by remember { mutableStateOf(false) }

    val loyaltyEligible = user.loyaltyCount >= settings.loyaltyThreshold
    val loyaltyDiscount = if (loyaltyEligible) selectedPkg.sellPrice * (settings.loyaltyDiscountPercent / 100.0) else 0.0
    val priceAfterLoyalty = (selectedPkg.sellPrice - loyaltyDiscount).coerceAtLeast(0.0)
    val totalDiscount = (loyaltyDiscount + codeDiscount).coerceAtMost(selectedPkg.sellPrice)
    val finalPrice = (selectedPkg.sellPrice - totalDiscount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = {
            Text(service.name, color = NavyPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("اختر الباقة:", style = MaterialTheme.typography.labelMedium, color = TextSecondarySlate)
                packages.forEach { pkg ->
                    val isSelected = selectedPkg.id == pkg.id
                    Surface(
                        color = if (isSelected) NavySoftContainer else BgWhite,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NavyPrimary else BorderLight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedPkg = pkg
                                appliedCode = null
                                codeDiscount = 0.0
                                codeFeedback = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pkg.label, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text(pkg.quantityLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            }
                            Text(
                                text = formatMad(pkg.sellPrice),
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = accountInfo,
                    onValueChange = { accountInfo = it },
                    label = { Text(category.inputHint) },
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_player_info_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it.uppercase()
                            if (appliedCode != null) {
                                appliedCode = null
                                codeDiscount = 0.0
                                codeFeedback = null
                            }
                        },
                        label = { Text("كود الخصم (اختياري)") },
                        singleLine = true,
                        shape = TwinCashInputShape,
                        colors = twinCashTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("order_discount_code_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            onValidateCode(service.category, codeInput, priceAfterLoyalty) { res ->
                                res.onSuccess { (entity, amount) ->
                                    appliedCode = entity.code
                                    codeDiscount = amount
                                    codeFeedback = "تم تطبيق الكود ${entity.code}: خصم ${formatMad(amount)}"
                                    codeIsError = false
                                }.onFailure { err ->
                                    appliedCode = null
                                    codeDiscount = 0.0
                                    codeFeedback = err.message ?: "كود غير صالح"
                                    codeIsError = true
                                }
                            }
                        },
                        shape = TwinCashButtonShape,
                        border = TwinCashSecondaryButtonBorder,
                        colors = twinCashSecondaryButtonColors(),
                        modifier = Modifier.testTag("apply_discount_code_btn")
                    ) {
                        Text("تطبيق", fontWeight = FontWeight.Bold)
                    }
                }

                if (codeFeedback != null) {
                    Text(
                        text = codeFeedback!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (codeIsError) ErrorRed else SuccessGreen
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة إضافية (اختياري)") },
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = SectionBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("رصيدك الحالي:", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                            Text(formatMad(user.balance), style = MaterialTheme.typography.bodySmall, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        if (totalDiscount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("السعر الأصلي:", style = MaterialTheme.typography.bodySmall, color = TextSecondarySlate)
                                Text(
                                    formatMad(selectedPkg.sellPrice),
                                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                    color = TextSecondarySlate
                                )
                            }
                        }
                        if (loyaltyEligible) {
                            Text(
                                text = "طلبك الخامس بسعر الفابور! (خصم ولاء ${settings.loyaltyDiscountPercent.toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المبلغ المطلوب:", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(formatMad(finalPrice), fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

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
                        onError("يرجى تأكيد التحقق البشري قبل إرسال الطلب")
                    } else {
                        onSubmit(selectedPkg, accountInfo, note, appliedCode ?: codeInput.trim().ifBlank { null })
                    }
                },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape,
                modifier = Modifier.testTag("confirm_service_order_btn")
            ) {
                Text("تأكيد ودفع ${formatMad(finalPrice)}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondarySlate)
            }
        }
    )
}
