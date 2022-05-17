package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context

interface InAppReviewSettings {
    suspend fun init(context: Context)
    suspend fun triggerAppReview(activity: Activity, onComplete: (successful: Boolean) -> Unit)
}
