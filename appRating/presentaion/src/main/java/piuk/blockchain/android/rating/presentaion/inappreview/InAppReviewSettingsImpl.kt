package piuk.blockchain.android.rating.presentaion.inappreview

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class InAppReviewSettingsImpl(
    private val reviewManager: ReviewManager
) : InAppReviewSettings {

    private var initialized = false
    private var reviewInfo: ReviewInfo? = null

    override suspend fun init(context: Context) {
        reviewInfo = reviewManager.getReviewInfo()
        initialized = true
    }

    override suspend fun triggerAppReview(activity: Activity): Boolean {
        return if (initialized) {
            reviewInfo?.let {
                reviewManager.launchReviewFlow(activity, reviewInfo)
                true
            } ?: false
        } else { // internet too slow and/or user was so fast
            // retry init
            init(activity)

            // retrigger
            triggerAppReview(activity)
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
