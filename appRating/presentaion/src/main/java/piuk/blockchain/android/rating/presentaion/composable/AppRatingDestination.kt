package piuk.blockchain.android.rating.presentaion.composable

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg

sealed class AppRatingDestination(override val route: String) : ComposeNavigationDestination {
    companion object {
        const val ARG_WITH_FEEDBACK = "withFeedback"
    }

    object Stars : AppRatingDestination("Stars")
    object Feedback : AppRatingDestination("Feedback")
    object Completed : AppRatingDestination("Completed/${ARG_WITH_FEEDBACK.wrappedArg()}")
}
