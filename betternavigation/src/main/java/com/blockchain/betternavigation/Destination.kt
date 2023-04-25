package com.blockchain.betternavigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator
import androidx.navigation.navOptions
import java.io.Serializable
import java.util.*

abstract class BetterNavGraph : BetterDestination() {
    override val baseRoute: String = this::class.java.name
}

abstract class BetterDestination : BetterDestinationWithArgs<Nothing>() {
    override val baseRoute: String = this::class.java.name

    override val route
        get() = baseRoute

    internal fun navigate(
        navController: NavController,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null,
    ) {
        navController.navigate(route, navOptions, navigatorExtras)
    }
}

abstract class BetterDestinationWithArgs<Args : Serializable?> {
    protected open val baseRoute: String = this::class.java.name

      open val route
        get() = "$baseRoute?$KEY_ARGS_ID={$KEY_ARGS_ID}"

    internal fun navigate(
        navController: NavController,
        argsHolder: NavigationArgsHolder,
        args: Args,
        navigatorExtras: Navigator.Extras? = null,
        builder: (NavOptionsBuilder.() -> Unit)? = null
    ) {
        val argsId = UUID.randomUUID().toString()
        argsHolder[argsId] = args
        val routeWithArgs = "$baseRoute?$KEY_ARGS_ID=$argsId"
        navController.navigate(routeWithArgs, builder?.let { navOptions(builder) }, navigatorExtras)
    }

    companion object {
        internal const val KEY_ARGS_ID = "argsId"
    }
}
