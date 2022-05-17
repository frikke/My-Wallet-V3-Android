package piuk.blockchain.android.rating.presentaion

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.launch
import piuk.blockchain.android.rating.domain.service.AppRatingService

class AppRatingViewModel(
    val appRatingService: AppRatingService
) : MviViewModel<AppRatingIntents,
    AppRatingViewState,
    AppRatingModelState,
    AppRatingNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = AppRatingModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: AppRatingModelState): AppRatingViewState = state.run {
        AppRatingViewState(
            dismiss = dismiss,
            promptInAppReview = promptInAppReview
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

            AppRatingIntents.InAppReviewCompleted -> {
                inAppReviewCompleted()
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
        // todo(othman): call api here
        // todo(othman): mark rating completed
        navigate(AppRatingNavigationEvent.Completed(withFeedback = true))
    }

    private fun inAppReviewCompleted() {
        updateState { it.copy(promptInAppReview = false) }
        // todo(othman): call api here
        // todo(othman): mark rating completed
        navigate(AppRatingNavigationEvent.Completed(withFeedback = false))
    }

    private fun saveRatingDateAndDismiss() {
        // todo(othman): save rating date - retrigger in 1 month
        updateState { it.copy(dismiss = true) }
    }

    private fun ratingCompleted() {
        updateState { it.copy(dismiss = true) }
    }
}
