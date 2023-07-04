package com.blockchain.componentlib.utils

import androidx.compose.foundation.shape.CircleShape
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
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
    LocalLogo.Rewards -> Icons.Rewards
}

fun LogoValueSource.Remote.toImageResource() = ImageResource.Remote(url = url, shape = CircleShape)
fun LogoValueSource.Local.toImageResource() = icon.toImageResource()