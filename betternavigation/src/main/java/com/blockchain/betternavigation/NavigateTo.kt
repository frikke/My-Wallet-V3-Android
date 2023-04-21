package com.blockchain.betternavigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import java.io.Serializable

class BetterNavigationContext internal constructor(
    val navController: NavController,
    internal val argsHolder: NavigationArgsHolder,
)

fun BetterNavigationContext.navigateUp() {
    navController.navigateUp()
}

fun BetterNavigationContext.navigateTo(
    destination: BetterDestination,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) {
    destination.navigate(navController, navOptions, navigatorExtras)
}

fun <Args : Serializable?> BetterNavigationContext.navigateTo(
    destination: BetterDestinationWithArgs<Args>,
    args: Args,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) {
    destination.navigate(navController, argsHolder, args, navOptions, navigatorExtras)
}
