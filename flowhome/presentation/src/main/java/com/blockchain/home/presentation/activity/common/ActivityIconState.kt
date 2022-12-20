package com.blockchain.home.presentation.activity.common

import androidx.annotation.DrawableRes

sealed interface ActivityIconState {
    sealed interface SmallTag : ActivityIconState {
        data class Remote(
            val main: String,
            val tag: String
        ) : SmallTag

        data class Local(
            @DrawableRes val main: Int,
            @DrawableRes val tag: Int
        ) : SmallTag
    }

    sealed interface OverlappingPair : ActivityIconState {
        data class Remote(
            val front: String,
            val back: String
        ) : OverlappingPair

        data class Local(
            @DrawableRes val front: Int,
            @DrawableRes val back: Int
        ) : OverlappingPair
    }

    sealed interface SingleIcon : ActivityIconState {
        data class Remote(
            val url: String
        ) : SingleIcon

        data class Local(
            @DrawableRes val res: Int
        ) : SingleIcon
    }

    object None : ActivityIconState
}
