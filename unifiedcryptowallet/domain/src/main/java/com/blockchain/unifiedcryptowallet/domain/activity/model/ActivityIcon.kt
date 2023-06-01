package com.blockchain.unifiedcryptowallet.domain.activity.model

sealed interface ActivityIcon {
    data class SmallTag(
        val main: ActivityIconSource,
        val tag: ActivityIconSource
    ) : ActivityIcon

    data class OverlappingPair(
        val front: ActivityIconSource,
        val back: ActivityIconSource
    ) : ActivityIcon

    data class SingleIcon(
        val icon: ActivityIconSource
    ) : ActivityIcon

    object None : ActivityIcon
}

sealed interface ActivityIconSource {
    data class Remote(val url: String) : ActivityIconSource
    data class Local(val icon: ActivityLocalIcon) : ActivityIconSource
}

enum class ActivityLocalIcon {
    Buy, Sell, Send, Receive, Reward, Swap
}
