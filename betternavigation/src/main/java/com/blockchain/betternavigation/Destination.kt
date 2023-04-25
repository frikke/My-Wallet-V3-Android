package com.blockchain.betternavigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import java.io.Serializable
import java.util.*

abstract class NavGraph : Destination() {
    override val baseRoute: String = this::class.java.name
}

abstract class Destination : DestinationWithArgs<Nothing>() {
    override val baseRoute: String = this::class.java.name

    override val route
        get() = baseRoute

    internal fun navigate(
        navController: NavController,
        navOptions: (NavOptionsBuilder.() -> Unit)? = null,
    ) {
        if (navOptions != null) {
            navController.navigate(route, navOptions)
        } else {
            navController.navigate(route)
        }
    }
}

abstract class DestinationWithArgs<Args : Serializable?> {
    protected open val baseRoute: String = this::class.java.name

    internal open val route
        get() = "$baseRoute?$KEY_ARGS_ID={$KEY_ARGS_ID}"

    internal fun navigate(
        navController: NavController,
        argsHolder: NavArgsHolder,
        args: Args,
        navOptions: (NavOptionsBuilder.() -> Unit)? = null,
    ) {
        val argsId = UUID.randomUUID().toString()
        argsHolder[argsId] = args
        val routeWithArgs = "$baseRoute?$KEY_ARGS_ID=$argsId"
        if (navOptions != null) {
            navController.navigate(routeWithArgs, navOptions)
        } else {
            navController.navigate(routeWithArgs)
        }
    }

    companion object {
        internal const val KEY_ARGS_ID = "argsId"
    }
}
