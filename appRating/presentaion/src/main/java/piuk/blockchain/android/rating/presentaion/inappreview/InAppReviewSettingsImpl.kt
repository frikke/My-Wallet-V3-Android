package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager

internal class InAppReviewSettingsImpl(
    private val reviewManager: ReviewManager
) : InAppReviewSettings {

    private var initialized = false
    private var reviewInfo: ReviewInfo? = null

    override fun init(context: Context) {
        initialized = true

        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewInfo = task.result
            }
        }
    }

    override fun triggerAppReview(activity: Activity, onComplete: () -> Unit) {
        require(initialized) { "InAppReviewSettings not initialized" }

        reviewInfo?.let {
            reviewManager.launchReviewFlow(activity, reviewInfo)
                .addOnCompleteListener {
                    onComplete()
                }
        } ?: onComplete()
    }
}
