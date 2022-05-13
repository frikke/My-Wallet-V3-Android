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
    onDismiss: () -> Unit
) {

    viewModel.viewCreated(ModelConfigArgs.NoArgs)

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    MviFragmentNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = AppRatingNavigationRouter(rememberNavController()),
        startDestination = AppRatingDestination.Stars,
    ) {
        // Rating Stars
        appRatingStarsDestination(viewModel)

        // Rating Feedback
        appRatingFeedbackDestination(viewModel)

        // Rating Completed
        appRatingCompletedDestination(onDismiss)
    }
}

private fun NavGraphBuilder.appRatingStarsDestination(viewModel: AppRatingViewModel) {
    composable(AppRatingDestination.Stars) {
        AppRatingStars(viewModel)
    }
}

private fun NavGraphBuilder.appRatingFeedbackDestination(viewModel: AppRatingViewModel) {
    composable(AppRatingDestination.Feedback) {
        AppRatingFeedback(viewModel)
    }
}

private fun NavGraphBuilder.appRatingCompletedDestination(onDismiss: () -> Unit) {
    composable(
        AppRatingDestination.Completed,
        listOf(
            navArgument(ARG_WITH_FEEDBACK) { type = NavType.BoolType }
        )
    ) { backStackEntry ->
        AppRatingCompleted(
            withFeedback = backStackEntry.arguments?.getBoolean(ARG_WITH_FEEDBACK)
                ?: error("arg ARG_WITH_FEEDBACK missing"),
            onSubmit = onDismiss
        )
    }
}