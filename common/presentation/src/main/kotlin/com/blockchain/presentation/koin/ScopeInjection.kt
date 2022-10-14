package com.blockchain.presentation.koin

import android.content.ComponentCallbacks
import com.blockchain.koin.payloadScope
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> ComponentCallbacks.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = payloadScope.inject<T>(qualifier, LazyThreadSafetyMode.SYNCHRONIZED, parameters)
