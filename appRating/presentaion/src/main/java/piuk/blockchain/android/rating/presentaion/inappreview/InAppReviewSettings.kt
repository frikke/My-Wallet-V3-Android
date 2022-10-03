package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo

interface InAppReviewSettings {
    /**
     * Recommended to initialize [ReviewInfo] beforehand to prompt inapp view asap when needed
     */
    suspend fun init(context: Context)

    /**
     * @return false if an error prevented inapp review from showing - true otherwise
     */
    suspend fun triggerAppReview(activity: Activity): Boolean
}
