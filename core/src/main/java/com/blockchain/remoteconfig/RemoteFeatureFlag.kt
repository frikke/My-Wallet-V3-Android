package com.blockchain.remoteconfig

import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.RemoteConfigPrefs
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

interface ABTestExperiment {
    fun getABVariant(key: String): Single<String>
}

interface RemoteConfig {

    fun isFeatureEnabled(key: String): Boolean

    fun getIfFeatureEnabled(key: String): Single<Boolean>

    fun getRawJson(key: String): Single<String>

    fun getFeatureCount(key: String): Single<Long>
}

class RemoteConfiguration(
    private val remoteConfig: FirebaseRemoteConfig,
    private val remoteConfigPrefs: RemoteConfigPrefs,
    private val environmentConfig: EnvironmentConfig
) : RemoteConfig, ABTestExperiment {

    private var remoteConfigFetchAndActivate: Completable? = null

    private val configuration: Single<FirebaseRemoteConfig>
        get() = updateRemoteConfig().toSingle { remoteConfig }

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
            if (isRemoteConfigStale || environmentConfig.isRunningInDebugMode())
                cacheExpirationForStaleValues
            else
                cacheExpirationForUpdatedValues

        remoteConfig.fetch(cacheExpirationTimeOut)
            .addOnCompleteListener {
                remoteConfig.activate().addOnCompleteListener {
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

    override fun getRawJson(key: String): Single<String> =
        configuration.map {
            it.getString(key)
        }

    override fun isFeatureEnabled(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }

    override fun getIfFeatureEnabled(key: String): Single<Boolean> =
        configuration.map {
            it.getBoolean(key)
        }

    override fun getABVariant(key: String): Single<String> =
        configuration.map { it.getString(key) }

    override fun getFeatureCount(key: String): Single<Long> =
        configuration.map { it.getLong(key) }

    companion object {
        private const val cacheExpirationForStaleValues = 0L
        private const val cacheExpirationForUpdatedValues = 14400L
    }
}

fun RemoteConfig.featureFlag(key: String, name: String): FeatureFlag = object : FeatureFlag {
    override val key: String = key
    override val readableName: String = name
    override val enabled: Single<Boolean> get() = getIfFeatureEnabled(key)
    override val isEnabled: Boolean by lazy {
        isFeatureEnabled(key)
    }
    override suspend fun coEnabled(): Boolean = enabled.await()
}
