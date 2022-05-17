package piuk.blockchain.android.rating.domain.service

interface AppRatingService {
    suspend fun getThreshold(): Int
}
