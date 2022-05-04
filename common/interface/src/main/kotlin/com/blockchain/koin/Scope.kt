package com.blockchain.koin

import com.blockchain.logging.RemoteLogger
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.java.KoinJavaComponent

private const val SCOPE_ID = "SCOPE_ID"

val payloadScope: Scope
    get() {
        val koin = KoinJavaComponent.getKoin()
        val logger: RemoteLogger = koin.get()
        return koin.getScopeOrNull(SCOPE_ID) ?: koin.createScope(SCOPE_ID, payloadScopeQualifier).also {
            logger.logEvent("Payload scope opened")
        }.apply {
            this.registerCallback(
                object : ScopeCallback {
                    override fun onScopeClose(scope: Scope) {
                        logger.logEvent("Payload scope closed")
                    }
                }
            )
        }
    }
