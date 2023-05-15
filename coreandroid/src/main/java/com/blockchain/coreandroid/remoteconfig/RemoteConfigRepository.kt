package com.blockchain.coreandroid.remoteconfig

import androidx.annotation.VisibleForTesting
import com.blockchain.core.experiments.cache.ExperimentsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.RemoteConfigPrefs
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class RemoteConfigRepository(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val remoteConfigPrefs: RemoteConfigPrefs,
    private val experimentsStore: ExperimentsStore,
    private val json: Json
) : RemoteConfigService {

    override fun getIfFeatureEnabled(key: String): Single<Boolean> =
        rxSingle {
            getRemoteConfig(key).toString().toBoolean()
        }

    override suspend fun getValueForFeature(key: String): Any =
        getRemoteConfig(key)

    override suspend fun <T : Any> getParsedJsonValue(key: String, serializer: KSerializer<T>): Outcome<Exception, T> {
        val jsonString = getRemoteConfig(key).toString()
        return try {
            Outcome.Success(json.decodeFromString(serializer, jsonString))
        } catch (ex: Exception) {
            Outcome.Failure(ex)
        }
    }

    override fun getRawJson(key: String): Single<String> =
        rxSingle {
            getRemoteConfig(key) as String
        }

    private suspend fun getRemoteConfig(key: String): Any {
        val remoteConfigValue = configuration.await().getValue(key).asString()
        return deepMap(remoteConfigValue)
    }

    private suspend fun getValueFromCacheFlow(): Map<String, Int> {
        return experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .filter { it !is DataResource.Loading }
            .map { dataResourceMap ->
                when (dataResourceMap) {
                    is DataResource.Data -> dataResourceMap.data
                    is DataResource.Error -> emptyMap()
                    DataResource.Loading -> {
                        error("experimentsStore illegal argument exception -  we should never reach this point")
                    }
                }
            }
            .firstOrNull().orEmpty()
    }

    /*
     deepMapJson is a simple algorithm over a Map<Key, Any> to recursively traverse the tree and map it’s contents,
     reducing it back into a Map<Key, Any>- once mapped and computed this can be then decoded into the final result.
     */
    private fun Map<*, *>.deepMapJson(
        transform: (Map.Entry<*, *>) -> Pair<*, *>
    ): Map<*, *> = map {
        val (key, value) = transform(it)
        when (value) {
            is Map<*, *> -> (key to value.deepMapJson(transform))
            is List<*> -> (
                key to value.map { v ->
                    if (v is Map<*, *>) v.deepMapJson(transform) else v
                }
                )
            else -> (key to value)
        }
    }.toMap()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun getMapToReturn(inputJson: Map<String, Any>): Map<*, *> {
        val experimentStoreValues = getValueFromCacheFlow()

        return inputJson.deepMapJson {
            val k = it.key
            when (val v = it.value) {
                is Map<*, *> -> {
                    v["{returns}"]
                        // {"{returns}":{"experiment":{"experimentName": {}}}}
                        ?.let { returns -> returns as? Map<*, Map<*, Map<*, *>>> }
                        ?.let { returns -> returns["experiment"] }
                        ?.let { experiment ->
                            if (experiment.isNotEmpty()) {
                                experiment.keys.firstAndOnly()
                                    ?.let { id -> Pair(id, experimentStoreValues[id]) } // find experiment id in cache
                                    ?.let { group -> experiment[group.first]?.get(group.second.toString()) }
                                    ?.let { config -> Pair(k, getContent(config)) }

                                    ?: v["default"]?.let { default ->
                                        Pair(k, getContent(default))
                                    } ?: throw NoSuchElementException("Experiment value and default does not exist.")
                            } else {
                                v["default"]?.let { default ->
                                    Pair(k, getContent(default))
                                } ?: throw NoSuchElementException("Experiment value and default does not exist.")
                            }
                        }
                        ?: Pair(k, getContent(v))
                }
                else -> Pair(k, getContent(v))
            }
        }
    }

    fun <T> Iterable<T>.firstAndOnly(): T? {
        val iterator = iterator()
        val first = iterator.next()
        if (iterator.hasNext()) {
            throw NoSuchElementException("Collection has more than one element.")
        }
        return first
    }

    suspend fun deepMap(remoteConfigValue: String): Any {
        return if (remoteConfigValue.contains("{returns}")) {
            try {
                val json = json.decodeFromString<JsonObject>(remoteConfigValue)
                getMapToReturn(mapOf<String, Any>("key" to json))["key"] as Any
            } catch (noSuchElementException: NoSuchElementException) {
                noSuchElementException
            } catch (e: Exception) {
                remoteConfigValue
            }
        } else {
            remoteConfigValue
        }
    }

    private fun getContent(result: Any?): Any? {
        return when (result) {
            is JsonPrimitive -> result.content
            is JsonNull -> result.content
            is JsonObject -> {
                val newMap = mutableMapOf<String, Any>()
                result.toMap().entries.forEach {
                    getContent(it.value)?.let { value ->
                        newMap[it.key] = value
                    }
                }
                newMap
            }
            is JsonArray -> {
                val newList = mutableListOf<Any>()
                result.toList().forEach {
                    getContent(it)?.let { value ->
                        newList.add(value)
                    }
                }
                newList
            }
            else -> result
        }
    }

    private var remoteConfigFetchAndActivate: Completable? = null

    private val configuration: Single<FirebaseRemoteConfig>
        get() = updateRemoteConfig().toSingle { firebaseRemoteConfig }

    private fun updateRemoteConfig(): Completable {
        return remoteConfigFetchAndActivate ?: fetchAndActivateCache(
            remoteConfigPrefs.isRemoteConfigStale
        )
            .doFinally {
                remoteConfigFetchAndActivate = null
            }.also {
                remoteConfigFetchAndActivate = it
            }
    }

    private fun fetchAndActivateCache(isRemoteConfigStale: Boolean): Completable = Completable.create { emitter ->
        val cacheExpirationTimeOut =
            if (isRemoteConfigStale) {
                cacheExpirationForStaleValues
            } else {
                cacheExpirationForUpdatedValues
            }

        firebaseRemoteConfig.fetch(cacheExpirationTimeOut)
            .addOnCompleteListener {
                firebaseRemoteConfig.activate().addOnCompleteListener {
                    if (isRemoteConfigStale) {
                        remoteConfigPrefs.updateRemoteConfigStaleStatus(false)
                    }
                    if (!emitter.isDisposed) {
                        emitter.onComplete()
                    }
                }
            }.addOnFailureListener {
                if (!emitter.isDisposed) {
                    emitter.onError(it)
                }
            }
    }.cache()

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val cacheExpirationForStaleValues = 0L
        private const val cacheExpirationForUpdatedValues = 14400L
    }
}
