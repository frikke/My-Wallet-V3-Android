package piuk.blockchain.android.ui.kyc.email.entry

import androidx.lifecycle.viewModelScope
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.core.settings.EmailSyncUpdater
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.utils.awaitOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

sealed class EmailVerificationIntent : Intent<EmailVerificationModelState> {
    object StartPollingForVerification : EmailVerificationIntent()
    object StopPollingForVerification : EmailVerificationIntent()
    object ResendEmailClicked : EmailVerificationIntent()
    object EditEmailClicked : EmailVerificationIntent()
    data class OnEmailChanged(val email: String) : EmailVerificationIntent()
    object ShowResendEmailConfirmationHandled : EmailVerificationIntent()
}

sealed class Navigation : NavigationEvent {
    data class EditEmailSheet(val currentEmail: String) : Navigation()
}

@Parcelize
data class Args(val emailMustBeValidated: Boolean) : ModelConfigArgs.ParcelableArgs

class EmailVerificationModel(
    private val emailUpdater: EmailSyncUpdater,
    private val getUserStore: GetUserStore,
) : MviViewModel<
    EmailVerificationIntent,
    EmailVerificationViewState,
    EmailVerificationModelState,
    Navigation,
    Args,
    >(EmailVerificationModelState()) {

    private var jobPolling: Job? = null

    override fun viewCreated(args: Args) {
        viewModelScope.launch {
            emailUpdater.email().awaitOutcome()
                .doOnSuccess { email ->
                    updateState { it.copy(email = email.address, isVerified = email.isVerified) }
                    if (!email.isVerified) {
                        if (args.emailMustBeValidated) {
                            viewModelScope.launch { emailUpdater.resendEmail(email.address).awaitOutcome() }
                        }
                        startPollingForVerification()
                    }
                }
                .doOnFailure { error ->
                    updateState { it.copy(error = EmailVerificationError.Generic(error.message)) }
                }
        }
    }

    override fun reduce(state: EmailVerificationModelState): EmailVerificationViewState =
        EmailVerificationViewState(
            email = state.email,
            isVerified = state.isVerified,
            showResendEmailConfirmation = state.showResendEmailConfirmation,
            error = state.error,
        )

    override suspend fun handleIntent(
        modelState: EmailVerificationModelState,
        intent: EmailVerificationIntent
    ) {
        when (intent) {
            EmailVerificationIntent.StartPollingForVerification -> startPollingForVerification()
            EmailVerificationIntent.StopPollingForVerification -> stopPollingForVerification()
            EmailVerificationIntent.EditEmailClicked -> {
                if (modelState.email != null) navigate(Navigation.EditEmailSheet(modelState.email))
            }
            EmailVerificationIntent.ResendEmailClicked -> {
                if (modelState.email != null) {
                    emailUpdater.resendEmail(modelState.email).awaitOutcome()
                        .doOnSuccess {
                            updateState { it.copy(showResendEmailConfirmation = true) }
                        }
                        .doOnFailure { error ->
                            updateState { it.copy(error = EmailVerificationError.Generic(error.message)) }
                        }
                }
            }
            is EmailVerificationIntent.OnEmailChanged -> {
                updateAndResendEmail(intent.email)
            }
            EmailVerificationIntent.ShowResendEmailConfirmationHandled -> updateState {
                it.copy(showResendEmailConfirmation = false)
            }
        }
    }

    private fun startPollingForVerification() {
        jobPolling?.cancel()
        jobPolling = viewModelScope.launch {
            emailUpdater.pollForEmailVerification(timerInSec = 1, retries = Int.MAX_VALUE)
                .doOnSuccess { email ->
                    updateState {
                        it.copy(email = email.address, isVerified = email.isVerified)
                    }
                    getUserStore.markAsStale()
                }
                .doOnFailure { error ->
                    updateState { it.copy(error = EmailVerificationError.Generic(error.message)) }
                }
        }
    }

    private fun stopPollingForVerification() {
        jobPolling?.cancel()
    }

    private suspend fun updateAndResendEmail(email: String) {
        emailUpdater.updateEmailAndSync(email).awaitOutcome()
            .doOnSuccess {
                updateState { it.copy(email = email, showResendEmailConfirmation = true) }
            }
            .doOnFailure { error ->
                val message = error.asError()
                updateState { it.copy(error = message) }
            }
    }

    private fun Exception.asError(): EmailVerificationError = if (
        this is NabuApiException &&
        this.getErrorStatusCode() == NabuErrorStatusCodes.TooManyRequests
    ) {
        EmailVerificationError.TooManyResendAttempts
    } else {
        EmailVerificationError.Generic(message)
    }
}
