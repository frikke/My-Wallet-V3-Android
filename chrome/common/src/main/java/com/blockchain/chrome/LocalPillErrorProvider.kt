package com.blockchain.chrome

import androidx.compose.runtime.compositionLocalOf
import com.blockchain.componentlib.alert.PillAlert
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

val LocalChromePillProvider = compositionLocalOf<ChromePill> {
    error("provides not set")
}

object ChromePill {
    private val alertFlow = MutableSharedFlow<PillAlert?>()
    val alert: SharedFlow<PillAlert?> get() = alertFlow

    suspend fun show(alert: PillAlert) {
        alertFlow.emit(alert)
    }
}
