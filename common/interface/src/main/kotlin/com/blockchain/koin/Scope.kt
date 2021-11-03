package com.blockchain.koin

import com.blockchain.logging.CrashLogger
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.java.KoinJavaComponent

private const val SCOPE_ID = "SCOPE_ID"

val payloadScope: Scope
    get() {
        val koin = KoinJavaComponent.getKoin()
        val crashLogger: CrashLogger = koin.get()
        return koin.getScopeOrNull(SCOPE_ID) ?: koin.createScope(SCOPE_ID, payloadScopeQualifier).also {
            crashLogger.logState("Payload Scope", "Payload scope opened")
        }.apply {
                this.registerCallback(
                    object : ScopeCallback {
                        override fun onScopeClose(scope: Scope) {
                            crashLogger.logState("Payload Scope", "Payload scope closed")
                        }
                    }
                )
            }
    }
