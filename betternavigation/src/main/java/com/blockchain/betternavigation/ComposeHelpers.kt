package com.blockchain.betternavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import java.io.Serializable

internal val LocalNavArgsHolderProvider = compositionLocalOf<NavArgsHolder> {
    error("No navigation args holder controller provided.")
}

val LocalNavControllerProvider = staticCompositionLocalOf<NavHostController> {
    error("No navigation host controller provided.")
}

@Composable
internal fun rememberArgsHolder(): NavArgsHolder {
    return rememberSaveable(
        saver = mapSaver(
            save = { value -> value.backingField },
            restore = { value ->
                @Suppress("UNCHECKED_CAST")
                NavArgsHolder(value as MutableMap<String, Serializable>)
            }
        )
    ) {
        NavArgsHolder()
    }
}
