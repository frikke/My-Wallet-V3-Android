package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import com.google.android.play.core.review.ReviewManager

internal class InAppReviewSettingsImpl(
    private val reviewManager: ReviewManager
) : InAppReviewSettings {

    override fun triggerAppReview(activity: Activity, onComplete: (successful: Boolean) -> Unit) {
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener {
                        onComplete(true)
                    }
            } else {
                onComplete(false)
            }
        }
    }
}
