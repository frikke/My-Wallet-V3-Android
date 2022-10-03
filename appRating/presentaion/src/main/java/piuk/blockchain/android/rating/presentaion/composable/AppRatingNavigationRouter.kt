package piuk.blockchain.android.rating.presentaion.composable

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import piuk.blockchain.android.rating.presentaion.AppRatingNavigationEvent

class AppRatingNavigationRouter(
    override val navController: NavHostController
) : ComposeNavigationRouter<AppRatingNavigationEvent> {

    override fun route(navigationEvent: AppRatingNavigationEvent) {
        when (navigationEvent) {
            AppRatingNavigationEvent.Feedback -> {
                val route = AppRatingDestination.Feedback.route
                navController.navigate(route)
            }
        }
    }
}
