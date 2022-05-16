package piuk.blockchain.android.rating.presentaion.composable

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import piuk.blockchain.android.rating.presentaion.AppRatingNavigationEvent
import piuk.blockchain.android.rating.presentaion.composable.AppRatingDestination.Companion.ARG_WITH_FEEDBACK

class AppRatingNavigationRouter(override val navController: NavHostController) :
    ComposeNavigationRouter<AppRatingNavigationEvent> {

    override fun route(navigationEvent: AppRatingNavigationEvent) {
        val destination = when (navigationEvent) {
            AppRatingNavigationEvent.Feedback -> {
                AppRatingDestination.Feedback.route
            }

            is AppRatingNavigationEvent.Completed -> {
                AppRatingDestination.Completed.routeWithParsedArgs(
                    listOf(NavArgument(ARG_WITH_FEEDBACK, navigationEvent.withFeedback))
                )
            }
        }

        navController.navigate(destination)
    }
}