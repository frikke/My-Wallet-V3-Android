package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context

interface InAppReviewSettings {
    fun init(context: Context)

    fun triggerAppReview(activity: Activity, onComplete: () -> Unit)
}
