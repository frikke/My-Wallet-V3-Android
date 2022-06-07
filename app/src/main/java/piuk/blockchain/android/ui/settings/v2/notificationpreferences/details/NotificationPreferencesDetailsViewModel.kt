package piuk.blockchain.android.ui.settings.v2.notificationpreferences.details

import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import piuk.blockchain.android.ui.settings.v2.notificationpreferences.NotificationPreferencesAnalyticsEvents
import piuk.blockchain.android.ui.settings.v2.notificationpreferences.NotificationPreferencesAnalyticsEvents.Companion.createChannelSetUpEvent
import timber.log.Timber

object NotificationPreferenceDetailsNavigation : NavigationEvent

data class NotificationPreferenceDetailsModelState(
    val title: String,
    val description: String,
    val channel: String,
    val methods: List<ContactMethod>
) : ModelState

sealed class NotificationPreferenceDetailsViewState(val title: String, val description: String) : ViewState {
    object Loading : NotificationPreferenceDetailsViewState("", "")
    class Data(
        title: String,
        description: String,
        val methods: List<ContactMethod>
    ) : NotificationPreferenceDetailsViewState(title, description)
    class Error(
        title: String,
        description: String
    ) : NotificationPreferenceDetailsViewState(title, description)
}

data class ContactMethod(
    val title: String,
    val method: String,
    val required: Boolean,
    val isMethodEnabled: Boolean
)

sealed class NotificationPreferenceDetailsIntent : Intent<NotificationPreferenceDetailsModelState> {
    data class ContactMethodChanged(
        val allMethods: List<ContactMethod>,
        val changed: ContactMethod
    ) : NotificationPreferenceDetailsIntent()
}

class NotificationPreferencesDetailsViewModel(
    private val interactor: NotificationPreferencesDetailsInteractor,
    private val analytics: Analytics
) : MviViewModel<
    NotificationPreferenceDetailsIntent,
    NotificationPreferenceDetailsViewState,
    NotificationPreferenceDetailsModelState,
    NotificationPreferenceDetailsNavigation,
    NotificationPreferenceDetailsArguments
    >(NotificationPreferenceDetailsModelState("", "", "", emptyList())) {

    override fun viewCreated(args: NotificationPreferenceDetailsArguments) {
        updateState {
            NotificationPreferenceDetailsModelState(
                args.title,
                args.description,
                args.channel,
                args.methods.map { it.mapToContactMethod() }
            )
        }
    }

    override fun reduce(state: NotificationPreferenceDetailsModelState): NotificationPreferenceDetailsViewState {
        return NotificationPreferenceDetailsViewState.Data(state.title, state.description, state.methods)
    }

    override suspend fun handleIntent(
        modelState: NotificationPreferenceDetailsModelState,
        intent: NotificationPreferenceDetailsIntent
    ) {
        when (intent) {
            is NotificationPreferenceDetailsIntent.ContactMethodChanged -> {
                val contactMethods = intent.allMethods.map {
                    if (it == intent.changed) {
                        intent.changed.copy(isMethodEnabled = !intent.changed.isMethodEnabled)
                    } else {
                        it
                    }
                }
                updateState {
                    modelState.copy(methods = contactMethods)
                }
                interactor.updateContactPreferences(modelState.channel, contactMethods)
                    .doOnSuccess {
                        analytics.logEvent(createChannelSetUpEvent(modelState.channel, contactMethods))
                    }
                    .doOnFailure {
                        Timber.e(it)
                        analytics.logEvent(
                            NotificationPreferencesAnalyticsEvents.StatusChangeError(modelState.channel)
                        )
                    }
            }
        }
    }
}

private fun NotificationContactMethod.mapToContactMethod() = ContactMethod(
    title = title,
    method = method,
    required = required,
    isMethodEnabled = enabled
)
