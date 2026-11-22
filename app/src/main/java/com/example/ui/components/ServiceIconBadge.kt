package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ServiceCategory
import com.example.ui.theme.BorderLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SectionBg

@Composable
fun ServiceIconBadge(
    category: ServiceCategory,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val icon = categoryLinearIcon(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(SectionBg)
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.arabicTitle,
            tint = NavyPrimary,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

private fun categoryLinearIcon(category: ServiceCategory): ImageVector = when (category) {
    ServiceCategory.FREE_FIRE -> Icons.Outlined.Diamond
    ServiceCategory.PUBG -> Icons.Outlined.Bolt
    ServiceCategory.ROBUX -> Icons.Outlined.Token
    ServiceCategory.STEAM -> Icons.Outlined.AccountBalanceWallet
    ServiceCategory.PLAYSTATION -> Icons.Outlined.SportsEsports
    ServiceCategory.XBOX -> Icons.Outlined.Games
}
