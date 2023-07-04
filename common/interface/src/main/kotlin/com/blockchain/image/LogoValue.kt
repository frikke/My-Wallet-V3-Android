package com.blockchain.image

sealed interface LogoValue {
    data class SmallTag(
        val main: LogoValueSource,
        val tag: LogoValueSource
    ) : LogoValue {
        constructor(main: String, tag: String) : this(
            LogoValueSource.Remote(main),
            LogoValueSource.Remote(tag)
        )
    }

    data class OverlappingPair(
        val front: LogoValueSource,
        val back: LogoValueSource
    ) : LogoValue

    data class SingleIcon(
        val icon: LogoValueSource
    ) : LogoValue {
        constructor(icon: LocalLogo) : this(LogoValueSource.Local(icon))
        constructor(url: String) : this(LogoValueSource.Remote(url))
    }

    object None : LogoValue
}

sealed interface LogoValueSource {
    data class Remote(val url: String) : LogoValueSource
    data class Local(val icon: LocalLogo) : LogoValueSource
}

enum class LocalLogo {
    Buy, Sell, Send, Receive, Swap, Rewards
}