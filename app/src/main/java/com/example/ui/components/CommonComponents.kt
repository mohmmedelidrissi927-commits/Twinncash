package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.SettingsEntity
import com.example.data.local.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.ErrorBg
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
import com.example.ui.theme.WarningBg
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.twinCashChipColors
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatMad(amount: Double): String = String.format(Locale.US, "%.2f د.م", amount)

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar", "MA"))
    return sdf.format(Date(millis))
}

fun openWhatsApp(context: Context, whatsappNumber: String, message: String) {
    val cleanPhone = whatsappNumber.replace("+", "").replace(" ", "").ifBlank { "212670533324" }
    val encoded = try {
        URLEncoder.encode(message, "UTF-8")
    } catch (_: Exception) {
        ""
    }
    val url = "https://wa.me/$cleanPhone?text=$encoded"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Composable
fun TwinCashCard(
    modifier: Modifier = Modifier,
    containerColor: Color = BgWhite,
    borderColor: Color = BorderLight,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Card(
        modifier = clickModifier,
        shape = TwinCashCardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TwinCashCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = BgWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SectionBg)
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = twinCashPrimaryButtonColors(),
                    shape = TwinCashButtonShape
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TwinCashTopBar(
    user: UserEntity,
    unreadNotificationCount: Int,
    onNotificationClick: () -> Unit,
    onTopUpClick: (() -> Unit)? = null,
    onLogoutClick: () -> Unit
) {
    val role = UserRole.fromKey(user.role)
    Surface(
        color = BgWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_twincash_logo),
                        contentDescription = "شعار TwinCash",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TwinCash",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(
                                label = role.arabicLabel,
                                bgColor = NavySoftContainer,
                                textColor = NavyPrimary
                            )
                        }
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (role == UserRole.CUSTOMER && onTopUpClick != null) {
                        Surface(
                            color = SectionBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onTopUpClick)
                                .testTag("topbar_balance_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "الرصيد",
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatMad(user.balance),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (role == UserRole.CUSTOMER && unreadNotificationCount > 0) {
                        IconButton(
                            onClick = onNotificationClick,
                            modifier = Modifier.testTag("notifications_button")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "إشعارات الطلبات",
                                    tint = NavyPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ErrorRed)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = NavyPrimary
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = BorderLight)
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderStatusPill(status: String) {
    val (label, bg, fg) = when (status) {
        "pending" -> Triple("قيد الانتظار", WarningBg, WarningOrange)
        "accepted" -> Triple("مقبول", SuccessBg, SuccessGreen)
        "completed" -> Triple("مكتمل", SuccessBg, SuccessGreen)
        "rejected" -> Triple("مرفوض", ErrorBg, ErrorRed)
        else -> Triple(status, SectionBg, TextSecondarySlate)
    }
    StatusPill(label = label, bgColor = bg, textColor = fg)
}

@Composable
fun WhatsAppFab(
    whatsappNumber: String,
    customerName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    FloatingActionButton(
        onClick = {
            openWhatsApp(
                context,
                whatsappNumber,
                "مرحباً وكالة TwinCash، معكم $customerName. أحتاج مساعدة بخصوص خدماتي."
            )
        },
        containerColor = NavyPrimary,
        contentColor = BgWhite,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
        modifier = modifier.testTag("whatsapp_floating_button")
    ) {
        Icon(
            imageVector = Icons.Outlined.Chat,
            contentDescription = "تواصل عبر واتساب"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopUpDialog(
    settings: SettingsEntity,
    customerName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, method: String, txRef: String, proofUri: String?) -> Unit,
    onCopiedPhone: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val methods = listOf("CIH Bank", "CashPlus", "Wafacash", "Barid Bank", "تعبئة اتصالات")

    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(methods.first()) }
    var txRef by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) proofUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = {
            Text(
                text = "تعبئة الرصيد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SectionBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حول المبلغ لرقم الوكالة:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate
                            )
                            Text(
                                text = settings.agencyPhone,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.agencyPhone))
                                onCopiedPhone()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "نسخ الرقم",
                                tint = NavyPrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المحول (د.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topup_amount_input")
                )

                Text(
                    text = "طريقة الدفع:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondarySlate
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    methods.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) },
                            colors = twinCashChipColors()
                        )
                    }
                }

                OutlinedTextField(
                    value = txRef,
                    onValueChange = { txRef = it },
                    label = { Text("رقم العملية أو اسم المرسل") },
                    singleLine = true,
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topup_ref_input")
                )

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = TwinCashButtonShape,
                    border = TwinCashSecondaryButtonBorder,
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topup_pick_proof_button")
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (proofUri == null) "إرفاق صورة إثبات التحويل" else "تم إرفاق صورة الإثبات ✓")
                }

                if (proofUri != null) {
                    AsyncImage(
                        model = proofUri,
                        contentDescription = "صورة إثبات التحويل",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                OutlinedButton(
                    onClick = {
                        val msg = "مرحباً وكالة TwinCash، أنا $customerName. أرسلت طلب تعبئة رصيد بمبلغ ${amountText.ifBlank { "---" }} د.م عبر $selectedMethod."
                        openWhatsApp(context, settings.whatsappNumber, msg)
                    },
                    shape = TwinCashButtonShape,
                    border = BorderStroke(1.dp, NavyInteractive),
                    colors = twinCashSecondaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = NavyInteractive)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الوصل عبر واتساب الوكالة", color = NavyInteractive, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(amt, selectedMethod, txRef, proofUri)
                },
                colors = twinCashPrimaryButtonColors(),
                shape = TwinCashButtonShape,
                modifier = Modifier.testTag("topup_submit_button")
            ) {
                Text("إرسال طلب التعبئة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondarySlate)
            }
        }
    )
}

@Composable
fun RejectReasonDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgWhite,
        shape = TwinCashCardShape,
        title = { Text(title, color = ErrorRed, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سيتم إرجاع المبلغ تلقائياً إلى رصيد الزبون مع إشعاره بالسبب:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الرفض") },
                    placeholder = { Text("مثال: معرف اللاعب غير صحيح") },
                    shape = TwinCashInputShape,
                    colors = twinCashTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reject_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = BgWhite),
                shape = TwinCashButtonShape,
                modifier = Modifier.testTag("confirm_reject_button")
            ) {
                Text("تأكيد الرفض", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("تراجع", color = TextSecondarySlate)
            }
        }
    )
}
