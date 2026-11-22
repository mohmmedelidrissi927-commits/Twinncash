package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.local.SeedData
import com.example.data.remote.onGoogleSignInClicked
import com.example.ui.components.HumanCheckWidget
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CairoFontFamily
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
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.theme.twinCashSecondaryButtonColors
import com.example.ui.theme.twinCashTextFieldColors

@Composable
fun AuthScreen(
    isBusy: Boolean,
    verifiedGoogleEmail: String? = null,
    suggestedName: String = "",
    onGoogleAuthSuccess: (verifiedEmail: String, suggestedName: String, pendingFullName: String?, pendingWhatsapp: String?) -> Unit = { _, _, _, _ -> },
    onCompleteRegistration: (fullName: String, whatsapp: String) -> Unit = { _, _ -> },
    onSwitchGoogleAccount: () -> Unit = {},
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isCompletionMode = !verifiedGoogleEmail.isNullOrBlank()
    var selectedTab by remember(isCompletionMode) {
        mutableIntStateOf(if (isCompletionMode) 1 else 0)
    }
    var fullName by remember(suggestedName) { mutableStateOf(suggestedName) }
    var emailInput by remember(verifiedGoogleEmail) { mutableStateOf(verifiedGoogleEmail.orEmpty()) }
    var whatsapp by remember { mutableStateOf("") }
    var humanVerified by remember { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }

    LaunchedEffect(verifiedGoogleEmail) {
        if (!verifiedGoogleEmail.isNullOrBlank()) {
            selectedTab = 1
            emailInput = verifiedGoogleEmail
        }
    }

    val effectiveEmail = (verifiedGoogleEmail ?: emailInput).trim().lowercase()
    val isAdminEmail = effectiveEmail == SeedData.ADMIN_EMAIL.lowercase()
    val combinedLoading = isBusy || googleLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_twincash_logo),
                contentDescription = "شعار TwinCash",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TwinCash",
                    fontFamily = CairoFontFamily,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "شحن الألعاب والبطاقات الرقمية بكل بساطة وأمان",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                shape = TwinCashCardShape,
                colors = CardDefaults.cardColors(containerColor = BgWhite),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SectionBg,
                        contentColor = NavyPrimary,
                        indicator = { tabPositions ->
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = NavyPrimary,
                                height = 2.5.dp
                            )
                        },
                        divider = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            enabled = !isCompletionMode,
                            onClick = {
                                selectedTab = 0
                                humanVerified = false
                            },
                            text = {
                                Text(
                                    "تسجيل الدخول",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) NavyPrimary else TextSecondarySlate
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_login")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                humanVerified = false
                            },
                            text = {
                                Text(
                                    "حساب جديد",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) NavyPrimary else TextSecondarySlate
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_register")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (selectedTab == 0 && !isCompletionMode) {
                        Surface(
                            color = SectionBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = NavyPrimary)
                                    Text(
                                        text = "دخول آمن عبر حساب Google",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                }
                                Text(
                                    text = "سجّل الدخول مباشرة بحساب Google الخاص بك. إذا كانت هذه زيارتك الأولى، يمكنك التوجه لتبويب \"حساب جديد\" لإدخال رقم الواتساب وتفعيل حسابك.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySlate
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HumanCheckWidget(
                            verified = humanVerified,
                            onVerifyToggle = { humanVerified = it }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            enabled = !combinedLoading,
                            onClick = {
                                if (!humanVerified) {
                                    onError("يرجى تأكيد التحقق الأمني أنك لست روبوتاً أولاً")
                                    return@Button
                                }
                                googleLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onAuthSuccess = { fbUser ->
                                        googleLoading = false
                                        onGoogleAuthSuccess(
                                            fbUser.email.orEmpty(),
                                            fbUser.displayName.orEmpty(),
                                            null,
                                            null
                                        )
                                    },
                                    onAuthError = { errMsg ->
                                        googleLoading = false
                                        onError(errMsg)
                                    },
                                    onAuthCancelled = {
                                        googleLoading = false
                                    }
                                )
                            },
                            colors = twinCashPrimaryButtonColors(),
                            shape = TwinCashButtonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (combinedLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = BgWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول عبر Google",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (isCompletionMode) {
                                Surface(
                                    color = SuccessBg,
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
                                                text = "حساب Google موثق",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                            Text(
                                                text = verifiedGoogleEmail.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimaryDark
                                            )
                                        }
                                        TextButton(onClick = onSwitchGoogleAccount) {
                                            Text("تغيير الحساب", color = NavyInteractive, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("الاسم الكامل") },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                singleLine = true,
                                shape = TwinCashInputShape,
                                colors = twinCashTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input")
                            )

                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("رقم الواتساب (إجباري)") },
                                placeholder = { Text("06XXXXXXXX") },
                                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = TwinCashInputShape,
                                colors = twinCashTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_whatsapp_input")
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { if (!isCompletionMode) emailInput = it },
                                readOnly = isCompletionMode,
                                label = { Text("البريد الإلكتروني (حساب Google)") },
                                placeholder = { Text("example@gmail.com") },
                                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = TwinCashInputShape,
                                colors = twinCashTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input")
                            )

                            AnimatedVisibility(visible = isAdminEmail) {
                                Surface(
                                    color = NavySoftContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = NavyPrimary)
                                        Text(
                                            text = "تم التعرف على بريد الإدارة: سيتم منحك دور admin تلقائياً عند التوثيق بحساب Google.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )
                                    }
                                }
                            }

                            HumanCheckWidget(
                                verified = humanVerified,
                                onVerifyToggle = { humanVerified = it }
                            )

                            Button(
                                enabled = !combinedLoading,
                                onClick = {
                                    if (!humanVerified) {
                                        onError("يرجى تأكيد التحقق الأمني أنك لست روبوتاً أولاً")
                                        return@Button
                                    }
                                    val cleanName = fullName.trim()
                                    val cleanWa = whatsapp.trim().replace(" ", "").replace("-", "")
                                    if (cleanName.length < 3) {
                                        onError("يرجى إدخال الاسم الكامل (3 أحرف على الأقل)")
                                        return@Button
                                    }
                                    if (cleanWa.length < 8) {
                                        onError("رقم الواتساب إجباري ويجب أن يكون صحيحاً")
                                        return@Button
                                    }

                                    if (isCompletionMode) {
                                        onCompleteRegistration(cleanName, cleanWa)
                                    } else {
                                        val typedEmail = emailInput.trim().lowercase()
                                        googleLoading = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onAuthSuccess = { fbUser ->
                                                googleLoading = false
                                                val fbEmail = fbUser.email?.trim()?.lowercase().orEmpty()
                                                if (typedEmail.isNotBlank() && typedEmail != fbEmail) {
                                                    onError("البريد المختار في Google ($fbEmail) يختلف عن البريد المدخل ($typedEmail). تم اختيار حساب Google الموثق.")
                                                }
                                                onGoogleAuthSuccess(
                                                    fbEmail,
                                                    fbUser.displayName.orEmpty(),
                                                    cleanName,
                                                    cleanWa
                                                )
                                            },
                                            onAuthError = { errMsg ->
                                                googleLoading = false
                                                onError(errMsg)
                                            },
                                            onAuthCancelled = {
                                                googleLoading = false
                                            }
                                        )
                                    }
                                },
                                colors = twinCashPrimaryButtonColors(),
                                shape = TwinCashButtonShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("auth_submit_button")
                            ) {
                                if (combinedLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = BgWhite,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isCompletionMode) "إكمال وإنشاء الحساب" else "إنشاء حساب جديد عبر Google",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isCompletionMode) {
                                OutlinedButton(
                                    onClick = { selectedTab = 0 },
                                    shape = TwinCashButtonShape,
                                    border = TwinCashSecondaryButtonBorder,
                                    colors = twinCashSecondaryButtonColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("لدي حساب مسجل؟ تسجيل الدخول عبر Google", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
