package com.blockchain.betternavigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import java.io.Serializable

class NavContext internal constructor(
    val navController: NavController,
    internal val argsHolder: NavArgsHolder
)

fun NavContext.navigateUp() {
    navController.navigateUp()
}

fun NavContext.popBackStack() {
    navController.popBackStack()
}

fun NavContext.popBackStack(
    destination: DestinationWithArgs<*>,
    inclusive: Boolean,
    saveState: Boolean = false
) {
    navController.popBackStack(
        destination.route,
        inclusive,
        saveState
    )
}

fun NavContext.navigateTo(
    destination: Destination,
    navOptions: (NavOptionsBuilder.() -> Unit)? = null
) {
    destination.navigate(navController, navOptions)
}

fun <Args : Serializable?> NavContext.navigateTo(
    destination: DestinationWithArgs<Args>,
    args: Args,
    navOptions: (NavOptionsBuilder.() -> Unit)? = null
) {
    destination.navigate(navController, argsHolder, args, navOptions)
}

fun NavOptionsBuilder.popUpTo(
    destination: DestinationWithArgs<*>,
    inclusive: Boolean = false,
    saveState: Boolean = false
) {
    popUpTo(destination.route) {
        this.inclusive = inclusive
        this.saveState = saveState
    }
}
