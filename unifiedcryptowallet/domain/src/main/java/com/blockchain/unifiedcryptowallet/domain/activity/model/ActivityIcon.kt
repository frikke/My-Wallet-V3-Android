package com.blockchain.unifiedcryptowallet.domain.activity.model

sealed interface ActivityIcon {
    data class SmallTag(
        val main: String,
        val tag: String
    ) : ActivityIcon

    data class OverlappingPair(
        val front: String,
        val back: String
    ) : ActivityIcon

    data class SingleIcon(
        val url: String
    ) : ActivityIcon

    object None : ActivityIcon
}
