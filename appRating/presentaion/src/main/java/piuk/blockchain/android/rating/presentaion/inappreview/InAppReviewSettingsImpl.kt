package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal class InAppReviewSettingsImpl(
    private val reviewManager: ReviewManager
) : InAppReviewSettings {

    private var initialized = false
    private var reviewInfo: ReviewInfo? = null

    override suspend fun init(context: Context) {
        reviewInfo = reviewManager.getReviewInfo()
        initialized = true
    }

    override suspend fun triggerAppReview(activity: Activity, onComplete: (successful: Boolean) -> Unit) {
        if (initialized) {
            reviewInfo?.let {
                reviewManager
                    .launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener {
                        onComplete(true)
                    }
            } ?: onComplete(false)
        } else { // internet too slow and/or user was so fast
            // retry init
            init(activity)

            // retrigger
            triggerAppReview(activity, onComplete)
        }
    }

    private suspend fun ReviewManager.getReviewInfo(): ReviewInfo? = suspendCancellableCoroutine { continuation ->
        requestReviewFlow().addOnCompleteListener { task ->
            continuation.resume(
                if (task.isSuccessful) task.result
                else null
            )
        }
    }
}
