package com.blockchain.componentlib.utils

import androidx.compose.foundation.shape.CircleShape
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
import com.blockchain.image.LocalLogo
import com.blockchain.image.LogoValueSource

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
