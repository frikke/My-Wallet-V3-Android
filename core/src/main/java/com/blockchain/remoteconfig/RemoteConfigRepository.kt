package com.blockchain.remoteconfig

import androidx.annotation.VisibleForTesting
import com.blockchain.core.experiments.cache.ExperimentsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.preferences.RemoteConfigPrefs
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle
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
    private val json: Json,
) : RemoteConfigService {

    override fun getIfFeatureEnabled(key: String): Single<Boolean> =
        rxSingle {
            getRemoteConfig(key).toString().toBoolean()
        }

    override suspend fun getValueForFeature(key: String): Any =
        getRemoteConfig(key)

    override fun getRawJson(key: String): Single<String> =
        rxSingle {
            getRemoteConfig(key) as String
        }

    /* This json structure may change
    {
    "{returns}": {
        "experiment": {
            "experiment-2": {
                "0": "WEEKLY",
                "1": "BIWEEKLY",
                "2": "MONTHLY"
            }
        }
    },
    "default": "WEEKLY"
    }

    {
    "experiment-1" : 2
    "experiment-2" : 1
    "experiment-3" : 4
    }
    */

    private suspend fun getRemoteConfig(key: String): Any {
        val remoteConfigValue = configuration.await().getValue(key).asString()
        return deepMap(remoteConfigValue)
    }

    //    private fun deepMap2(remoteConfigValue: String) {
    //        val returnsValue: Map<String, String>
    //        val json = json.decodeFromString<JsonObject>(remoteConfigValue)
    //        json.forEach { key, value ->
    //            if (isLast(json, key)) {
    //                deepMap2(key)
    //            } else {
    //
    //            }
    //        }
    //    }
    //
    //    private fun isLast(json: Map<String, Any>, key: String): Boolean {
    //        return try {
    //            json.getValue(key) as Map<String, Any>
    //            true
    //        } catch (e: NoSuchElementException) {
    //            false
    //        }
    //    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun deepMap(remoteConfigValue: String): Any {

        if (remoteConfigValue.contains("{returns}") && remoteConfigValue.contains("experiment")) {
            val json = json.decodeFromString<JsonObject>(remoteConfigValue)
            // {"{returns}":{"experiment":{"experiment-2":{"0":"WEEKLY","1":"BIWEEKLY","2":"MONTHLY"}}},"default":"WEEKLY"}

            val returnsValue: Map<String, String>
            val jsonMap = mutableMapOf<String, Any>()
            val titleValue = json["title"]
            if (titleValue != null) {
                jsonMap["title"] = getContent(titleValue)

                val jsonMessage = json.getValue("message") as Map<String, String>
                returnsValue = jsonMessage.getValue("{returns}") as Map<String, String>
            } else {
                returnsValue = json.getValue("{returns}") as Map<String, String>
                // {"experiment":{"experiment-2":{"0":"WEEKLY","1":"BIWEEKLY","2":"MONTHLY"}}}
            }

            val experimentValue = returnsValue.getValue("experiment") as Map<String, String>
            // {"experiment-2":{"0":"WEEKLY","1":"BIWEEKLY","2":"MONTHLY"}}

            val experimentKey = experimentValue.keys.firstOrNull()
            // "experiment-2"

            val result = experimentKey?.let { key ->
                val experimentValues = experimentValue[key] as Map<String, Any>
                // {"0":"WEEKLY","1":"BIWEEKLY","2":"MONTHLY"}

                getValueFromCacheFlow(experimentValues, json, key)
            } ?: run {

                tryOrCatch(json, "default")
            }
            val resultString = getContent(result)

            return if (jsonMap.isNotEmpty() && resultString !is NoSuchElementException) {
                jsonMap["message"] = resultString
                jsonMap
            } else {
                resultString
            }
        } else {
            return remoteConfigValue
        }
    }

    private fun tryOrCatch(json: JsonObject, value: String): Any {
        return try {
            getContent(json.getValue(value))
        } catch (exception: NoSuchElementException) {
            exception
        }
    }

    private fun getContent(result: Any): Any {
        return when (result) {
            is JsonPrimitive -> result.content
            is JsonNull -> result.content
            is JsonObject -> {
                val newMap = mutableMapOf<String, Any>()
                result.toMap().entries.forEach {
                    newMap[it.key] = getContent(it.value)
                }
                newMap
            }
            is JsonArray -> {
                val newList = mutableListOf<Any>()
                result.toList().forEach {
                    newList.add(getContent(it))
                }
                newList
            }
            else -> result
        }
    }

    private suspend fun getValueFromCacheFlow(
        experimentValues: Map<String, Any>,
        json: JsonObject,
        experimentKey: String
    ): Any? {
        return experimentsStore.stream(FreshnessStrategy.Cached(forceRefresh = false))
            .filter { it !is DataResource.Loading }
            .map { dataResourceMap ->
                when (dataResourceMap) {
                    is DataResource.Data -> {
                        // {"experiment-2": 0}
                        val assignedValue = dataResourceMap.data[experimentKey]
                        // 0
                        experimentValues[assignedValue.toString()] ?: tryOrCatch(json, "default")
                    }

                    is DataResource.Error -> tryOrCatch(json, "default")

                    DataResource.Loading -> {
                        error("experimentsStore illegal argument exception -  we should never reach this point")
                    }
                }
            }.firstOrNull()
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
                    if (!emitter.isDisposed)
                        emitter.onComplete()
                }
            }.addOnFailureListener {
                if (!emitter.isDisposed)
                    emitter.onError(it)
            }
    }.cache()

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val cacheExpirationForStaleValues = 0L
        private const val cacheExpirationForUpdatedValues = 14400L
    }
}
