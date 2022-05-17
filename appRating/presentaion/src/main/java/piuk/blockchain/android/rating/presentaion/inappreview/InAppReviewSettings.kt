package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo

interface InAppReviewSettings {
    /**
     * Recommended to initialize [ReviewInfo] beforehand to prompt inapp view asap when needed
     */
    suspend fun init(context: Context)

    suspend fun triggerAppReview(activity: Activity, onComplete: (successful: Boolean) -> Unit)
}
