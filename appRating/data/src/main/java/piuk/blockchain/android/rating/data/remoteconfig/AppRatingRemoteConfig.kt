package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await

internal class AppRatingRemoteConfig(
    private val remoteConfig: RemoteConfig
) {
    companion object {
        private const val DEFAULT_THRESHOLD = 4
        private const val THRESHOLD_KEY = "android_app_rating_threshold"
    }

    suspend fun getThreshold(): Int {
        return remoteConfig.getRawJson(THRESHOLD_KEY).await().let { threshold ->
            try {
                threshold.toInt()
            } catch (e: NumberFormatException) {
                DEFAULT_THRESHOLD
            }
        }
    }
}
