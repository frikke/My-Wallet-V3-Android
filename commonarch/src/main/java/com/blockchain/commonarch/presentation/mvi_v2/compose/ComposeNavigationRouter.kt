package com.blockchain.commonarch.presentation.mvi_v2.compose

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.fragment.app.Fragment
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.createGraph
import androidx.navigation.get
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

interface ComposeNavigationRouter<TNavEvent : NavigationEvent> : NavigationRouter<TNavEvent> {
    val navController: NavHostController

    fun finishHostFragment() {
        (navController.context as? BlockchainActivity)?.supportFragmentManager?.popBackStack()
    }

    fun replaceCurrentFragment(@IdRes containerViewId: Int, fragment: Fragment, addToBackStack: Boolean = true) {
        (navController.context as? BlockchainActivity)?.supportFragmentManager
            ?.beginTransaction()
            ?.addTransactionAnimation()
            ?.replace(containerViewId, fragment, fragment::class.simpleName)
            ?.apply {
                if (addToBackStack) {
                    addToBackStack(fragment::class.simpleName)
                }
            }
            ?.commitAllowingStateLoss()
    }
}

interface ComposeNavigationDestination {

    val route: String

    fun routeWithArgs(args: List<NavArgument>): String {
        var finalRoute = route

        args.forEach { (key, value) ->
            key.wrappedArg().let { argKey ->
                if (finalRoute.contains(argKey)) {
                    finalRoute = finalRoute.replace(argKey, value.toString())
                }
            }
        }

        return finalRoute
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalMaterialApi::class, InternalCoroutinesApi::class)
@Composable
fun <TNavEvent : NavigationEvent> MviBottomSheetNavHost(
    navigationRouter: ComposeNavigationRouter<TNavEvent>,
    startDestination: ComposeNavigationDestination,
    navEvents: Flow<TNavEvent>,
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit = {},
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    LaunchedEffect(key1 = Unit) {
        navEvents.collectLatest { navigationEvent ->
            navigationRouter.route(navigationEvent)
        }
    }

    val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
    navigationRouter.navController.navigatorProvider.addNavigator(bottomSheetNavigator)

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(
            topEnd = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
            topStart = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
        ),
        sheetBackgroundColor = AppTheme.colors.backgroundSecondary,
        scrimColor = AppTheme.colors.overlay
    ) {
        NavHost(
            navigationRouter.navController,
            remember(route, startDestination.route, builder) {
                navigationRouter.navController.createGraph(startDestination.route, route, builder)
            },
            modifier
        )

        val navigatorSheetState = bottomSheetNavigator.navigatorSheetState

        if (navigatorSheetState.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    onCollapse()
                }
            }
        }
    }
}

@Composable
fun <TNavEvent : NavigationEvent> MviFragmentNavHost(
    navigationRouter: ComposeNavigationRouter<TNavEvent>,
    startDestination: ComposeNavigationDestination,
    navEvents: Flow<TNavEvent>,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    LaunchedEffect(key1 = Unit) {
        navEvents.collectLatest { navigationEvent ->
            navigationRouter.route(navigationEvent)
        }
    }

    NavHost(
        navigationRouter.navController,
        remember(route, startDestination.route, builder) {
            navigationRouter.navController.createGraph(startDestination.route, route, builder)
        },
        modifier
    )
}

fun NavGraphBuilder.composable(
    navigationEvent: ComposeNavigationDestination,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(provider[ComposeNavigator::class], content).apply {
            this.route = navigationEvent.route
            arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}

fun NavHostController.navigate(
    destination: ComposeNavigationDestination,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    navigate(destination = destination, args = listOf(), builder = builder)
}

fun NavHostController.navigate(
    destination: ComposeNavigationDestination,
    args: List<NavArgument>,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    builder?.let {
        navigate(destination.routeWithArgs(args), it)
    } ?: navigate(destination.routeWithArgs(args))
}

fun NavHostController.printBackStackToConsole() {
    Timber.d("NavHostController Backstack:")
    this.backQueue.forEach {
        Timber.d("\n\t ${it.destination.route}")
    }
}

@ExperimentalMaterialNavigationApi
fun NavGraphBuilder.bottomSheet(
    navigationEvent: ComposeNavigationDestination,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable ColumnScope.(backstackEntry: NavBackStackEntry) -> Unit
) {

    addDestination(
        BottomSheetNavigator.Destination(
            provider[BottomSheetNavigator::class]
        ) {
            val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            content(it)
            Spacer(modifier = Modifier.size(navBarHeight))
        }.apply {
            this.route = navigationEvent.route
            arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialNavigationApi::class)
@Composable
fun rememberBottomSheetNavigator(
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    skipHalfExpanded: Boolean = true
): BottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        animationSpec = animationSpec,
        skipHalfExpanded = skipHalfExpanded
    )

    return remember(sheetState) {
        BottomSheetNavigator(sheetState = sheetState)
    }
}

data class NavArgument(val key: String, val value: Any)

fun String.wrappedArg() = "{$this}"

fun Bundle.getComposeArgument(key: String) = getString(key)?.let {
    if (it == key.wrappedArg()) null else it
}
