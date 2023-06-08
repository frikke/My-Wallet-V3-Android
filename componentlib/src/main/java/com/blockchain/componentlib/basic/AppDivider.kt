package com.blockchain.componentlib.basic

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppColors

@Composable
fun AppDivider(color: Color = AppColors.background) {
    Divider(color = color)
}
