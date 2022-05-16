package piuk.blockchain.android.rating.data.repository

import com.blockchain.outcome.fold
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.service.AppRatingService

internal class AppRatingRepository(
    private val appRatingRemoteConfig: AppRatingRemoteConfig,
    private val defaultThreshold: Int
) : AppRatingService {

    override suspend fun getThreshold(): Int {
        return appRatingRemoteConfig.getThreshold().fold(
            onSuccess = { it },
            onFailure = { defaultThreshold }
        )
    }
}
