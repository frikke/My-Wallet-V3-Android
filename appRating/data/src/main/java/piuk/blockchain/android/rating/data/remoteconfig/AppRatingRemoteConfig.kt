package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.outcome.Outcome
import kotlinx.coroutines.rx3.await

internal class AppRatingRemoteConfig(
    private val remoteConfigService: RemoteConfigService
) {
    companion object {
        private const val THRESHOLD_KEY = "android_app_rating_threshold"
    }

    suspend fun getThreshold(): Outcome<NumberFormatException, Int> {
        return remoteConfigService.getRawJson(THRESHOLD_KEY).await().let { threshold ->
            try {
                Outcome.Success(threshold.toInt())
            } catch (e: NumberFormatException) {
                Outcome.Failure(e)
            }
        }
    }
}
