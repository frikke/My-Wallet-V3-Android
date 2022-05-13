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
    initialState = AppRatingModelState
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: AppRatingModelState): AppRatingViewState {
        return AppRatingViewState
    }

    override suspend fun handleIntent(modelState: AppRatingModelState, intent: AppRatingIntents) {
        when (intent) {
            is AppRatingIntents.StarsSubmitted -> {
                submitStars(intent.stars)
            }

            is AppRatingIntents.FeedbackSubmitted -> {
                submitFeedback()
            }
        }.exhaustive
    }

    private fun submitStars(stars: Int) {
        viewModelScope.launch {
            appRatingService.getThreshold().let { threshold ->
                val navigationEvent = if (stars > threshold) {
                    AppRatingNavigationEvent.Completed(withFeedback = false)
                } else {
                    AppRatingNavigationEvent.Feedback
                }

                navigate(navigationEvent)
            }
        }
    }

    private fun submitFeedback() {
        navigate(AppRatingNavigationEvent.Completed(withFeedback = true))
    }
}
