package com.blockchain.featureflag

import io.reactivex.rxjava3.core.Single

interface FeatureFlag {
    val key: String
    val readableName: String
    val enabled: Single<Boolean>
    val isEnabled: Boolean
    suspend fun coEnabled(): Boolean
}
