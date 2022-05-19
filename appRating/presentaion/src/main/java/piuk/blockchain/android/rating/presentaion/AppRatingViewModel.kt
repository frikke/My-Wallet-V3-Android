package piuk.blockchain.android.rating.presentaion

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
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
    ModelConfigArgs.NoArgs>(
    initialState = AppRatingModelState()
) {

    private var stars: Int = 0
    private var feedback = StringBuilder("from: ${authPrefs.walletGuid}")

    /**
     * [inAppReviewCompleted] could return an error because showing in-app could've failed
     */
    private var forceRetrigger: Boolean = false

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
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
        this.stars = stars

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
            this.feedback.insert(
                0,
                // to separate feedback from wallet id
                // apparently new lines don't register as such,
                // even when doing it on web, the result is in one line
                "$feedback ------ "
            )
        }

        navigate(AppRatingNavigationEvent.Completed(withFeedback = true))
    }

    private fun inAppReviewCompleted(successful: Boolean) {
        // remove prompt
        updateState { it.copy(promptInAppReview = false) }

        // will be used in postRatingData response
        forceRetrigger = successful.not()

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
        appRatingService.postRatingData(AppRating(rating = stars, feedback = feedback.toString())).let { successful ->
            if (successful && forceRetrigger.not()) {
                appRatingService.markRatingCompleted()
            } else {
                appRatingService.saveRatingDateForLater()
            }
        }
    }
}
