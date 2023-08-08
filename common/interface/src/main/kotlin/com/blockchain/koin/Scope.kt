package com.blockchain.koin

import com.blockchain.logging.RemoteLogger
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
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

val payloadScopeOrNull: Scope?
    get() = KoinJavaComponent.getKoin().getScopeOrNull(SCOPE_ID)

inline fun <reified T> KoinComponent.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) { payloadScope.get(qualifier, parameters) }
