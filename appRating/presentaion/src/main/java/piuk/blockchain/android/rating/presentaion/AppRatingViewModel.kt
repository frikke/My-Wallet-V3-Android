package piuk.blockchain.android.rating.presentaion

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import com.blockchain.preferences.AuthPrefs
import kotlinx.coroutines.launch
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService

class AppRatingViewModel(
    val appRatingService: AppRatingService,
    val authPrefs: AuthPrefs
) : MviViewModel<AppRatingIntents,
    AppRatingViewState,
    AppRatingModelState,
    AppRatingNavigationEvent,
    AppRatingTriggerSource>(
    initialState = AppRatingModelState()
) {

    override fun viewCreated(args: AppRatingTriggerSource) {
        updateState {
            it.copy(
                walletId = authPrefs.walletGuid,
                screenName = args.value
            )
        }
    }

    override fun reduce(state: AppRatingModelState): AppRatingViewState = state.run {
        AppRatingViewState(
            dismiss = dismiss,
            promptInAppReview = promptInAppReview,
            isLoading = isLoading
        )
    }

    override suspend fun handleIntent(modelState: AppRatingModelState, intent: AppRatingIntents) {
        when (intent) {
            is AppRatingIntents.StarsSubmitted -> {
                submitStars(intent.stars)
            }

            is AppRatingIntents.FeedbackSubmitted -> {
                submitFeedback(intent.feedback)
            }

            is AppRatingIntents.InAppReviewCompleted -> {
                inAppReviewCompleted(intent.successful)
            }

            AppRatingIntents.RatingCanceled -> {
                saveRatingDateAndDismiss()
            }

            AppRatingIntents.RatingCompleted -> {
                ratingCompleted()
            }
        }.exhaustive
    }

    private fun submitStars(stars: Int) {
        updateState { it.copy(stars = stars) }

        // get threshold to navigate to the right screen
        viewModelScope.launch {
            appRatingService.getThreshold().let { threshold ->
                if (stars > threshold) {
                    updateState { it.copy(promptInAppReview = true) }
                } else {
                    navigate(AppRatingNavigationEvent.Feedback)
                }
            }
        }
    }

    private fun submitFeedback(feedback: String) {
        if (feedback.isBlank().not()) {
            updateState {
                it.copy(feedback = feedback)
            }
        }

        navigate(AppRatingNavigationEvent.Completed(withFeedback = true))
    }

    private fun inAppReviewCompleted(successful: Boolean) {
        // remove prompt
        // forceRetrigger will be used in postRatingData response
        updateState { it.copy(promptInAppReview = false, forceRetrigger = successful.not()) }

        navigate(AppRatingNavigationEvent.Completed(withFeedback = false))
    }

    private fun saveRatingDateAndDismiss() {
        appRatingService.saveRatingDateForLater()
        updateState { it.copy(dismiss = true) }
    }

    private fun ratingCompleted() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            postRatingData()

            updateState { it.copy(dismiss = true) }
        }
    }

    private suspend fun postRatingData() {
        appRatingService.postRatingData(
            appRating = AppRating(
                rating = modelState.stars,
                feedback = modelState.feedbackFormatted()
            )
        ).let { successful ->
            if (successful && modelState.forceRetrigger.not()) {
                appRatingService.markRatingCompleted()
            } else {
                appRatingService.saveRatingDateForLater()
            }
        }
    }
}
