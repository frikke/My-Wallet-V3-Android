package com.blockchain.componentlib.icon

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun StackedIcons(
    iconTopUrl: String,
    iconButtomUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(width = 32.dp, height = 40.dp)
    ) {
        Image(
            painter = rememberImagePainter(
                data = iconButtomUrl,
                builder = {
                    crossfade(true)
                    placeholder(ColorDrawable(AppTheme.colors.light.toArgb()))
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .align(Alignment.BottomEnd)
        )

        Image(
            painter = rememberImagePainter(
                data = iconTopUrl,
                builder = {
                    crossfade(true)
                    placeholder(ColorDrawable(AppTheme.colors.light.toArgb()))
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.background)
                .border(2.dp, AppTheme.colors.background, shape = CircleShape)
                .align(Alignment.TopStart)
        )
    }
}

@Preview
@Composable
fun StackedIcons_Basic() {
    AppTheme {
        AppSurface {
            StackedIcons(
                iconTopUrl = "",
                iconButtomUrl = ""
            )
        }
    }
}