package com.blockchain.chrome

import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The value must have a type that could be stored in [android.os.Bundle]
 * @see [androidx.lifecycle.SavedStateHandle.set]
 */
fun <T> NavController.setResult(key: String, value: T): Boolean {
    return previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
        ?.run { true } ?: false
}

fun <T> NavController.getResultFlow(key: String, initialValue: T): StateFlow<T> {
    return currentBackStackEntry?.savedStateHandle?.getStateFlow(key, initialValue)
        ?: MutableStateFlow(initialValue)
}

fun <T> NavController.clearResult(key: String): T? {
    return currentBackStackEntry
        ?.savedStateHandle
        ?.remove<T>(key)
}
