package piuk.blockchain.android.rating.domain.service

import com.blockchain.preferences.AppRatingPrefs
import piuk.blockchain.android.rating.domain.model.AppRating

interface AppRatingService {
    suspend fun getThreshold(): Int
    suspend fun postRatingData(appRating: AppRating): Boolean

    /**
     * * has not rated before
     * * must be GOLD
     * * no withdraw locks
     * * last try was more than a month ago
     **/
    suspend fun shouldShowRating(): Boolean

    /**
     * Saves the current date/time if needed in the future [AppRatingPrefs.promptDateMillis]
     * And set [AppRatingPrefs.completed] true
     */
    fun markRatingCompleted()

    /**
     * Saves the current date/time [AppRatingPrefs.promptDateMillis] to check against
     * to show the rating after one month
     */
    fun saveRatingDateForLater()
}
