package piuk.blockchain.android.rating.presentaion.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviFragmentNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import piuk.blockchain.android.rating.presentaion.AppRatingTriggerSource
import piuk.blockchain.android.rating.presentaion.AppRatingViewModel

@Composable
fun AppRatingNavHost(viewModel: AppRatingViewModel, appRatingTriggerSource: AppRatingTriggerSource) {

    viewModel.viewCreated(appRatingTriggerSource)

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    MviFragmentNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = AppRatingNavigationRouter(
            navController = rememberNavController()
        ),
        startDestination = AppRatingDestination.Stars,
    ) {
        // Rating Stars
        appRatingStarsDestination(viewModel)

        // Rating Feedback
        appRatingFeedbackDestination(viewModel)
    }
}

private fun NavGraphBuilder.appRatingStarsDestination(viewModel: AppRatingViewModel) {
    composable(navigationEvent = AppRatingDestination.Stars) {
        AppRatingStars(viewModel)
    }
}

private fun NavGraphBuilder.appRatingFeedbackDestination(viewModel: AppRatingViewModel) {
    composable(navigationEvent = AppRatingDestination.Feedback) {
        AppRatingFeedback(viewModel)
    }
}
