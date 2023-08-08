package com.blockchain.home.presentation.accouncement.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Unlock
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.ImageValue
import com.blockchain.componentlib.utils.value
import com.blockchain.home.presentation.accouncement.LocalAnnouncement

@Composable
fun LocalAnnouncements(
    announcements: List<LocalAnnouncement>,
    onClick: (LocalAnnouncement) -> Unit
) {
    if (announcements.isNotEmpty()) {
        // todo support multiple announcements
        announcements.first().let { announcement ->
            AnnouncementCard(
                title = announcement.title.value(),
                subtitle = announcement.subtitle.value(),
                icon = StackedIcon.SingleIcon(
                    when (val icon = announcement.icon) {
                        is ImageValue.Local -> {
                            ImageResource.Local(icon.res, size = AppTheme.dimensions.hugeSpacing).run {
                                icon.tint?.let { withTint(it) } ?: this
                            }
                        }
                        is ImageValue.Remote -> {
                            ImageResource.Remote(icon.url, size = AppTheme.dimensions.hugeSpacing)
                        }
                    }
                ),
                onClick = { onClick(announcement) }
            )
        }
    }
}

@Composable
fun AnnouncementCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: StackedIcon,
    elevation: Dp = AppTheme.dimensions.mediumElevation,
    contentAlphaProvider: () -> Float = { 1F },
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = AppTheme.colors.backgroundSecondary,
        elevation = elevation,
        shape = AppTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = AppTheme.dimensions.smallSpacing, vertical = AppTheme.dimensions.standardSpacing)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomStackedIcon(
                icon = icon,
                iconBackground = Color.Transparent,
                size = AppTheme.dimensions.hugeSpacing,
                alphaProvider = contentAlphaProvider
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Column {
                Text(
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlphaProvider()
                    },
                    text = title,
                    style = AppTheme.typography.caption1,
                    color = AppColors.body
                )
                Text(
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlphaProvider()
                    },
                    text = subtitle,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewAnnouncementCard() {
    AnnouncementCard(
        title = "Secure your wallets",
        subtitle = "Backup your Seed Phrase to keep your DeFi Wallet safe",
        icon = StackedIcon.SingleIcon(Icons.Filled.Unlock.withTint(AppColors.negative).withSize(40.dp)),
        onClick = {}
    )
}
