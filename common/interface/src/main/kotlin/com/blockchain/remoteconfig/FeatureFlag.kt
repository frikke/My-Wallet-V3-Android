package com.blockchain.remoteconfig

import io.reactivex.rxjava3.core.Single

interface FeatureFlag {
    val key: String
    val readableName: String
    val enabled: Single<Boolean>
}
