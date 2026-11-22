package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val TwinCashLightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = BgWhite,
    primaryContainer = NavySoftContainer,
    onPrimaryContainer = NavyPrimary,
    secondary = NavyInteractive,
    onSecondary = BgWhite,
    secondaryContainer = SectionBg,
    onSecondaryContainer = NavyPrimary,
    tertiary = NavyInteractive,
    onTertiary = BgWhite,
    background = BgWhite,
    onBackground = TextPrimaryDark,
    surface = BgWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = SectionBg,
    onSurfaceVariant = TextSecondarySlate,
    surfaceContainerLowest = BgWhite,
    surfaceContainerLow = SectionBg,
    surfaceContainer = BgWhite,
    surfaceContainerHigh = SectionBg,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = ErrorRed,
    onError = BgWhite,
    errorContainer = ErrorBg,
    onErrorContainer = ErrorRed
)

val TwinCashShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun twinCashTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = BgWhite,
    unfocusedContainerColor = BgWhite,
    disabledContainerColor = SectionBg,
    focusedBorderColor = NavyPrimary,
    unfocusedBorderColor = BorderLight,
    focusedLabelColor = NavyPrimary,
    unfocusedLabelColor = TextSecondarySlate,
    focusedTextColor = TextPrimaryDark,
    unfocusedTextColor = TextPrimaryDark,
    focusedLeadingIconColor = NavyPrimary,
    unfocusedLeadingIconColor = TextSecondarySlate,
    focusedTrailingIconColor = NavyPrimary,
    unfocusedTrailingIconColor = TextSecondarySlate,
    cursorColor = NavyPrimary,
    errorBorderColor = ErrorRed,
    errorLabelColor = ErrorRed
)

@Composable
fun twinCashPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = NavyPrimary,
    contentColor = BgWhite,
    disabledContainerColor = BorderLight,
    disabledContentColor = TextSecondarySlate
)

@Composable
fun twinCashSecondaryButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
    containerColor = BgWhite,
    contentColor = NavyPrimary,
    disabledContainerColor = SectionBg,
    disabledContentColor = TextSecondarySlate
)

val TwinCashSecondaryButtonBorder = BorderStroke(1.dp, NavyPrimary)
val TwinCashButtonShape = RoundedCornerShape(12.dp)
val TwinCashCardShape = RoundedCornerShape(16.dp)
val TwinCashInputShape = RoundedCornerShape(12.dp)

@Composable
fun twinCashChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = BgWhite,
    labelColor = TextSecondarySlate,
    selectedContainerColor = NavyPrimary,
    selectedLabelColor = BgWhite,
    selectedLeadingIconColor = BgWhite
)

@Composable
fun TwinCashTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = TwinCashLightColorScheme,
            typography = TwinCashTypography,
            shapes = TwinCashShapes,
            content = content
        )
    }
}
