package piuk.blockchain.android.rating.presentaion.composable

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import piuk.blockchain.android.rating.presentaion.AppRatingNavigationEvent
import piuk.blockchain.android.rating.presentaion.composable.AppRatingDestination.Companion.ARG_WITH_FEEDBACK

class AppRatingNavigationRouter(
    override val navController: NavHostController,
    val triggerInAppReview: () -> Unit
) : ComposeNavigationRouter<AppRatingNavigationEvent> {

    override fun route(navigationEvent: AppRatingNavigationEvent) {
        when (navigationEvent) {
            AppRatingNavigationEvent.RequestInAppReview -> {
                triggerInAppReview()
            }

            AppRatingNavigationEvent.Feedback -> {
                val route = AppRatingDestination.Feedback.route
                navController.navigate(route)
            }

            is AppRatingNavigationEvent.Completed -> {
                val route = AppRatingDestination.Completed.routeWithArgs(
                    listOf(NavArgument(ARG_WITH_FEEDBACK, navigationEvent.withFeedback))
                )
                navController.navigate(route)
            }
        }
    }
}
