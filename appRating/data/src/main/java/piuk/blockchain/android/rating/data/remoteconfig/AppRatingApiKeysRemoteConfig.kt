package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.outcome.Outcome
import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys

internal class AppRatingApiKeysRemoteConfig(
    private val remoteConfig: RemoteConfig,
    private val json: Json
) {
    companion object {
        private const val API_KEYS_KEY = "android_app_rating_api_keys"
    }

    suspend fun getApiKeys(): Outcome<Throwable, AppRatingApiKeys> {
        return remoteConfig.getRawJson(API_KEYS_KEY).await().let { apiKeysJson ->
            if (apiKeysJson.isEmpty()) Outcome.Failure(Throwable("remote config json not found"))
            else Outcome.Success(json.decodeFromString(apiKeysJson))
        }
    }
}
