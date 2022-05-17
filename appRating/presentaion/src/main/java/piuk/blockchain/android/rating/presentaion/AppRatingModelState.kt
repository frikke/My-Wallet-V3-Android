package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class AppRatingModelState(
    val dismiss: Boolean = false
) : ModelState
