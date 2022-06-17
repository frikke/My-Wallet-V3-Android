package piuk.blockchain.android.ui.settings.v2.notificationpreferences

import androidx.lifecycle.viewModelScope
import com.blockchain.analytics.Analytics
import com.blockchain.api.services.ContactPreference
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class NotificationPreferencesNavigation : NavigationEvent {
    object Back : NotificationPreferencesNavigation()
    data class Details(val contactPreference: ContactPreference) : NotificationPreferencesNavigation()
}

sealed class NotificationPreferencesModelState : ModelState {
    object Loading : NotificationPreferencesModelState()
    data class Data(val categories: List<ContactPreference>) : NotificationPreferencesModelState()
    object Error : NotificationPreferencesModelState()
}

sealed class NotificationPreferencesViewState : ViewState {
    object Loading : NotificationPreferencesViewState()
    data class Data(val categories: List<NotificationCategory>) : NotificationPreferencesViewState()
    object Error : NotificationPreferencesViewState()
}

data class NotificationCategory(val title: String, val notificationTypes: String)

class NotificationPreferencesViewModel(
    private val interactor: NotificationPreferencesInteractor,
    private val ioDispatcher: CoroutineDispatcher,
    private val analytics: Analytics
) : MviViewModel<
    NotificationPreferencesIntent,
    NotificationPreferencesViewState,
    NotificationPreferencesModelState,
    NotificationPreferencesNavigation,
    ModelConfigArgs.NoArgs
    >(NotificationPreferencesModelState.Loading) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) { }

    override fun reduce(state: NotificationPreferencesModelState): NotificationPreferencesViewState {
        return when (state) {
            is NotificationPreferencesModelState.Loading -> NotificationPreferencesViewState.Loading
            is NotificationPreferencesModelState.Data -> NotificationPreferencesViewState.Data(
                state.categories.map { it.mapToNotificationCategory() }
            )
            is NotificationPreferencesModelState.Error -> NotificationPreferencesViewState.Error
        }
    }

    override suspend fun handleIntent(
        modelState: NotificationPreferencesModelState,
        intent: NotificationPreferencesIntent
    ) {
        when (intent) {
            is NotificationPreferencesIntent.Fetch -> fetchPreferences()
            is NotificationPreferencesIntent.Retry -> fetchPreferences()
            is NotificationPreferencesIntent.NavigateToDetails -> navigateToDetails(intent.preferenceId, modelState)
            is NotificationPreferencesIntent.NavigateBack -> navigate(NotificationPreferencesNavigation.Back)
        }
    }

    private fun navigateToDetails(preferenceId: Int, modelState: NotificationPreferencesModelState) {
        if (modelState is NotificationPreferencesModelState.Data) {
            val category = modelState.categories[preferenceId]
            analytics.logEvent(NotificationPreferencesAnalyticsEvents.NotificationPreferencesTapped(category.channel))
            navigate(NotificationPreferencesNavigation.Details(category))
        }
    }

    private suspend fun fetchPreferences() {
        viewModelScope.launch(ioDispatcher) {
            interactor.getNotificationPreferences()
                .map { NotificationPreferencesModelState.Data(it) }
                .doOnFailure { Timber.e("Error fetching preference", NotificationPreferencesModelState.Error) }
                .getOrDefault(NotificationPreferencesModelState.Error)
                .also { state -> updateState { state } }
        }
    }
}

fun ContactPreference.mapToNotificationCategory() = NotificationCategory(title, subtitle)
