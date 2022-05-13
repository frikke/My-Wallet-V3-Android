package piuk.blockchain.android.rating.data.repository

import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.rating.domain.service.AppRatingService

class AppRatingRepository(
    private val remoteConfig: RemoteConfig
) : AppRatingService {

    companion object {
        private const val DEFAULT_THRESHOLD = 4
        private const val THRESHOLD_KEY = "android_app_rating_threshold"
    }

    override suspend fun getThreshold(): Int {
        return remoteConfig.getRawJson(THRESHOLD_KEY).await().let { threshold ->
            try {
                threshold.toInt()
            } catch (e: NumberFormatException) {
                DEFAULT_THRESHOLD
            }
        }
    }
}