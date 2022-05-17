package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity

interface InAppReviewSettings {
    fun triggerAppReview(activity: Activity, onComplete: (successful: Boolean) -> Unit)
}
