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

    suspend fun getThreshold(): Outcome<Exception, Int> {
        return try {
            remoteConfigService.getRawJson(THRESHOLD_KEY).await().let { threshold ->
                Outcome.Success(threshold.toInt())
            }
        } catch (e: Exception) {
            Outcome.Failure(e)
        }
    }
}
