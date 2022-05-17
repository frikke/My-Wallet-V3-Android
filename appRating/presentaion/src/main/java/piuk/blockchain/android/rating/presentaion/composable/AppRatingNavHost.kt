package piuk.blockchain.android.rating.presentaion.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviFragmentNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import piuk.blockchain.android.rating.presentaion.AppRatingViewModel
import piuk.blockchain.android.rating.presentaion.composable.AppRatingDestination.Companion.ARG_WITH_FEEDBACK

@Composable
fun AppRatingNavHost(
    viewModel: AppRatingViewModel,
    triggerInAppReview: () -> Unit
) {

    viewModel.viewCreated(ModelConfigArgs.NoArgs)

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    MviFragmentNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = AppRatingNavigationRouter(
            navController = rememberNavController(),
            triggerInAppReview = triggerInAppReview
        ),
        startDestination = AppRatingDestination.Stars,
    ) {
        // Rating Stars
        appRatingStarsDestination(viewModel)

        // Rating Feedback
        appRatingFeedbackDestination(viewModel)

        // Rating Completed
        appRatingCompletedDestination(viewModel)
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

private fun NavGraphBuilder.appRatingCompletedDestination(viewModel: AppRatingViewModel) {
    composable(
        navigationEvent = AppRatingDestination.Completed,
        arguments = listOf(
            navArgument(ARG_WITH_FEEDBACK) { type = NavType.BoolType }
        )
    ) { backStackEntry ->
        AppRatingCompleted(
            viewModel = viewModel,
            withFeedback = backStackEntry.arguments?.getBoolean(ARG_WITH_FEEDBACK)
                ?: error("arg ARG_WITH_FEEDBACK missing")
        )
    }
}
