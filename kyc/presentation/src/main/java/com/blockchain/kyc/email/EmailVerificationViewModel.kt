package com.blockchain.kyc.email

import androidx.lifecycle.viewModelScope
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.settings.EmailSyncUpdater
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.utils.awaitOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EmailVerificationViewModel(
    private val verificationRequired: Boolean,
    private val emailUpdater: EmailSyncUpdater,
    private val notificationTransmitter: NotificationTransmitter
) : MviViewModel<
    EmailVerificationIntent,
    EmailVerificationViewState,
    EmailVerificationModelState,
    Navigation,
    ModelConfigArgs.NoArgs
    >(EmailVerificationModelState()) {

    private var jobPolling: Job? = null

    init {
        viewModelScope.launch {
            emailUpdater.email().awaitOutcome()
                .doOnSuccess { email ->
                    updateState {
                        copy(
                            email = email.address,
                            status = if (email.isVerified) {
                                EmailVerificationStatus.Success
                            } else {
                                EmailVerificationStatus.Default
                            }
                        )
                    }

                    if (!email.isVerified) {
                        if (verificationRequired) {
                            viewModelScope.launch {
                                emailUpdater.resendEmail(email.address).awaitOutcome()
                            }
                        }

                        startPollingForVerification()
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        copy(
                            status = EmailVerificationStatus.Error,
                            notification = EmailVerificationNotification.Error(error.asError())
                        )
                    }
                }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun EmailVerificationModelState.reduce() = EmailVerificationViewState(
        email = email.orEmpty(),
        status = status,
        showResendingEmailInProgress = isResendingEmailInProgress,
        snackbarMessage = notification
    )

    override suspend fun handleIntent(
        modelState: EmailVerificationModelState,
        intent: EmailVerificationIntent
    ) {
        when (intent) {
            EmailVerificationIntent.ResendEmailClicked -> {
                check(modelState.email != null)

                updateState {
                    copy(
                        notification = null,
                        isResendingEmailInProgress = true
                    )
                }

                emailUpdater.resendEmail(modelState.email).awaitOutcome()
                    .doOnSuccess {
                        updateState {
                            copy(
                                status = EmailVerificationStatus.Default,
                                notification = EmailVerificationNotification.EmailSent,
                                isResendingEmailInProgress = false
                            )
                        }
                    }
                    .doOnFailure { error ->
                        updateState {
                            copy(
                                status = EmailVerificationStatus.Error,
                                notification = EmailVerificationNotification.Error(error.asError()),
                                isResendingEmailInProgress = false
                            )
                        }
                    }
            }
        }
    }

    private fun startPollingForVerification() {
        jobPolling?.cancel()
        jobPolling = viewModelScope.launch {
            emailUpdater.pollForEmailVerification(timerInSec = 1, retries = Int.MAX_VALUE)
                .doOnSuccess { email ->
                    updateState {
                        copy(
                            email = email.address,
                            status = if (email.isVerified) {
                                EmailVerificationStatus.Success
                            } else {
                                EmailVerificationStatus.Default
                            }
                        )
                    }

                    if (email.isVerified) {
                        jobPolling?.cancel()
                        notificationTransmitter.postEvent(NotificationEvent.UserUpdated)
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        copy(
                            status = EmailVerificationStatus.Error,
                            notification = EmailVerificationNotification.Error(error.asError()),
                        )
                    }
                }
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
