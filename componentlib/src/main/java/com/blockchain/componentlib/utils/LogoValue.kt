package com.blockchain.componentlib.utils

import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Rewards
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap

/**
 * Logo can either be Remote with a String URL - or Local with a drawable resource
 */
sealed interface LogoValue {
    data class Remote(val value: String) : LogoValue
    data class Local(val value: LocalLogo) : LogoValue
}

enum class LocalLogo {
    Buy, Sell, Send, Receive, Swap, Rewards
}

fun LocalLogo.toImageResource(): ImageResource.Local = when (this) {
    LocalLogo.Buy -> Icons.Plus
    LocalLogo.Sell -> Icons.Minus
    LocalLogo.Send -> Icons.Send
    LocalLogo.Receive -> Icons.Receive
    LocalLogo.Swap -> Icons.Swap
    LocalLogo.Rewards -> Icons.Rewards
}
