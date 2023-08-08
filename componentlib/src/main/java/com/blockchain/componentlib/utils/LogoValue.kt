package com.blockchain.componentlib.utils

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Prices
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Rewards
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.image.LocalLogo
import com.blockchain.image.LogoValue
import com.blockchain.image.LogoValueSource

@Composable
fun LogoValue.toStackedIcon() = when (this) {
    is LogoValue.OverlappingPair -> StackedIcon.OverlappingPair(
        front = front.toImageResource(),
        back = back.toImageResource()
    )

    is LogoValue.SmallTag -> StackedIcon.SmallTag(
        main = main.toImageResource(),
        tag = tag.toImageResource()
    )

    is LogoValue.SingleIcon -> StackedIcon.SingleIcon(
        icon = icon.toImageResource()
    )

    LogoValue.None -> StackedIcon.None
}

@Composable
private fun LogoValueSource.toImageResource(): ImageResource {
    return when (this) {
        is LogoValueSource.Remote -> ImageResource.Remote(url)
        is LogoValueSource.Local -> icon.toImageResource().withTint(AppTheme.colors.title)
            .withBackground(
                backgroundColor = AppTheme.colors.light,
                backgroundSize = AppTheme.dimensions.standardSpacing
            )
    }
}

fun LocalLogo.toImageResource(): ImageResource.Local = when (this) {
    LocalLogo.Buy -> Icons.Plus
    LocalLogo.Sell -> Icons.Minus
    LocalLogo.Send -> Icons.Send
    LocalLogo.Receive -> Icons.Receive
    LocalLogo.Swap -> Icons.Swap
    LocalLogo.PassiveRewards -> Icons.Rewards
    LocalLogo.StakingRewards -> Icons.Filled.Lock.withSize(16.dp)
    LocalLogo.ActiveRewards -> Icons.Filled.Prices.withSize(16.dp)
}

fun LogoValueSource.Remote.toImageResource() = ImageResource.Remote(url = url, shape = CircleShape)
fun LogoValueSource.Local.toImageResource() = icon.toImageResource()
