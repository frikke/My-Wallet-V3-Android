package com.blockchain.domain.experiments

import io.reactivex.rxjava3.core.Single

interface RemoteConfigService {

    fun getIfFeatureEnabled(key: String): Single<Boolean>

    suspend fun getValueForFeature(key: String): Any

    fun getRawJson(key: String): Single<String>
}
