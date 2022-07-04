package com.blockchain.presentation.onboarding.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviFragmentNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.presentation.onboarding.screens.DeFiOnboardingComplete
import com.blockchain.presentation.onboarding.screens.DeFiOnboardingIntro
import com.blockchain.presentation.onboarding.viewmodel.DeFiOnboardingViewModel

@Composable
fun DeFiOnboardingNavHost(
    viewModel: DeFiOnboardingViewModel
) {
    viewModel.viewCreated(ModelConfigArgs.NoArgs)

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    MviFragmentNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = DeFiOnboardingNavigationRouter(
            navController = rememberNavController()
        ),
        startDestination = DeFiOnboardingDestination.DeFiOnboardingIntro,
    ) {
        // Intro
        deFiOnboardingIntroDestination(viewModel)

        // Complete
        deFiOnboardingCompleteDestination(viewModel)
    }
}

private fun NavGraphBuilder.deFiOnboardingIntroDestination(viewModel: DeFiOnboardingViewModel) {
    composable(navigationEvent = DeFiOnboardingDestination.DeFiOnboardingIntro) {
        DeFiOnboardingIntro(viewModel)
    }
}

private fun NavGraphBuilder.deFiOnboardingCompleteDestination(viewModel: DeFiOnboardingViewModel) {
    composable(navigationEvent = DeFiOnboardingDestination.DeFiOnboardingComplete) {
        DeFiOnboardingComplete(viewModel)
    }
}
