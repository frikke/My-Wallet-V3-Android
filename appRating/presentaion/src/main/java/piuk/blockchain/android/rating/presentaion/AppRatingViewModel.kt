package piuk.blockchain.android.rating.presentaion

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import com.blockchain.preferences.AuthPrefs
import kotlinx.coroutines.launch
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService

class AppRatingViewModel(
    private val appRatingService: AppRatingService,
    private val authPrefs: AuthPrefs
) : MviViewModel<
    AppRatingIntents,
    AppRatingViewState,
    AppRatingModelState,
    AppRatingNavigationEvent,
    AppRatingTriggerSource
    >(
    initialState = AppRatingModelState()
) {

    override fun viewCreated(args: AppRatingTriggerSource) {
        updateState {
            copy(
                walletId = authPrefs.walletGuid,
                screenName = args.value
            )
        }
    }

    override fun AppRatingModelState.reduce() = AppRatingViewState(
        dismiss = dismiss,
        promptInAppReview = promptInAppReview,
        isLoading = isLoading
    )

    override suspend fun handleIntent(modelState: AppRatingModelState, intent: AppRatingIntents) {
        when (intent) {
            is AppRatingIntents.StarsSubmitted -> {
                submitStars(intent.stars)
            }

            is AppRatingIntents.FeedbackSubmitted -> {
                submitFeedback(intent.feedback)
            }

            is AppRatingIntents.InAppReviewRequested -> {
                inAppReviewRequested(intent.successful)
            }

            AppRatingIntents.RatingCanceled -> {
                saveRatingDateAndDismiss()
            }
        }.exhaustive
    }

    private fun submitStars(stars: Int) {
        updateState { copy(stars = stars) }

        // get threshold to navigate to the right screen
        viewModelScope.launch {
            appRatingService.getThreshold().let { threshold ->
                if (stars > threshold) {
                    updateState { copy(promptInAppReview = true) }
                } else {
                    navigate(AppRatingNavigationEvent.Feedback)
                }
            }
        }
    }

    private fun submitFeedback(feedback: String) {
        if (feedback.isBlank().not()) {
            updateState {
                copy(feedback = feedback)
            }
        }

        postRatingData()
        ratingCompleted()
    }

    private fun inAppReviewRequested(successful: Boolean) {
        if (successful) {
            postRatingData()
        } else {
            appRatingService.saveRatingDateForLater()
        }

        ratingCompleted()
    }

    private fun saveRatingDateAndDismiss() {
        appRatingService.saveRatingDateForLater()
        ratingCompleted()
    }

    private fun ratingCompleted() {
        updateState { copy(dismiss = true) }
    }

    private fun postRatingData() {
        appRatingService.postRatingData(
            appRating = AppRating(
                rating = modelState.stars,
                feedback = modelState.feedbackFormatted()
            )
        )
    }
}
