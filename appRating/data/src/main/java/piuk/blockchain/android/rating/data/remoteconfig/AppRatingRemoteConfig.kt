package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.outcome.Outcome
import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await

internal class AppRatingRemoteConfig(
    private val remoteConfig: RemoteConfig
) {
    companion object {
        private const val THRESHOLD_KEY = "android_app_rating_threshold"
    }

    suspend fun getThreshold(): Outcome<NumberFormatException, Int> {
        return remoteConfig.getRawJson(THRESHOLD_KEY).await().let { threshold ->
            try {
                Outcome.Success(threshold.toInt())
            } catch (e: NumberFormatException) {
                Outcome.Failure(e)
            }
        }
    }
}
