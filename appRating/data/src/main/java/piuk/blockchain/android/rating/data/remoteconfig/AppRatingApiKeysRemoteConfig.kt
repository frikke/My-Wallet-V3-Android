package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.outcome.Outcome
import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import timber.log.Timber

internal class AppRatingApiKeysRemoteConfig(
    private val remoteConfig: RemoteConfig,
    private val json: Json
) {
    companion object {
        private const val API_KEYS_KEY = "android_app_rating_api_keys"
    }

    suspend fun getApiKeys(): Outcome<Throwable, AppRatingApiKeys> {
        return remoteConfig.getRawJson(API_KEYS_KEY).await().let { apiKeysJson ->
            if (apiKeysJson.isEmpty()) {
                Timber.e("remote config json not found")
                Outcome.Failure(Throwable("remote config json not found"))
            } else {
                try {
                    Outcome.Success(json.decodeFromString(apiKeysJson))
                } catch (e: SerializationException) {
                    Timber.e(e)
                    Outcome.Failure(e)
                }
            }
        }
    }
}
