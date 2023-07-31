package com.blockchain.domain.experiments

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.KSerializer

interface RemoteConfigService {

    fun getIfFeatureEnabled(key: String): Single<Boolean>

    suspend fun getValueForFeature(key: String): Any

    suspend fun <T : Any> getParsedJsonValue(key: String, serializer: KSerializer<T>): Outcome<Exception, T>

    fun getRawJson(key: String): Single<String>
}
