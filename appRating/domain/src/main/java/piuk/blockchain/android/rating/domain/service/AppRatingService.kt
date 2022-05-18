package piuk.blockchain.android.rating.domain.service

import piuk.blockchain.android.rating.domain.model.AppRating

interface AppRatingService {
    suspend fun getThreshold(): Int
    suspend fun postRatingData(appRating: AppRating): Boolean
}
