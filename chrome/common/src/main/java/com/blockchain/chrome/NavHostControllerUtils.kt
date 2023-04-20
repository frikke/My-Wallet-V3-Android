package com.blockchain.chrome

import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The value must have a type that could be stored in [android.os.Bundle]
 * @see [androidx.lifecycle.SavedStateHandle.set]
 */
fun <T> NavHostController.setResult(key: String, value: T): Boolean {
    return previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
        ?.run { true } ?: false
}

fun <T> NavHostController.getResultFlow(key: String, initialValue: T): StateFlow<T> {
    return currentBackStackEntry?.savedStateHandle?.getStateFlow(key, initialValue)
        ?: MutableStateFlow(initialValue)
}
