package piuk.blockchain.android.rating.data.repository

import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.service.AppRatingService

internal class AppRatingRepository(
    private val appRatingRemoteConfig: AppRatingRemoteConfig
) : AppRatingService {

    override suspend fun getThreshold(): Int {
        return appRatingRemoteConfig.getThreshold()
    }
}