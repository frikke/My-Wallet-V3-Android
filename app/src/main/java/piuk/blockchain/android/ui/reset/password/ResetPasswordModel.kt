package piuk.blockchain.android.ui.reset.password

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorTypes
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class ResetPasswordModel(
    initialState: ResetPasswordState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val interactor: ResetPasswordInteractor
) : MviModel<ResetPasswordState, ResetPasswordIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(previousState: ResetPasswordState, intent: ResetPasswordIntents): Disposable? {
        return when (intent) {
            is ResetPasswordIntents.SetNewPassword ->
                setNewPassword(
                    password = intent.password,
                    intent.shouldResetKyc
                )
            is ResetPasswordIntents.CreateWalletForAccount ->
                createWalletForAccount(
                    email = intent.email,
                    password = intent.password,
                    userId = intent.userId,
                    recoveryToken = intent.recoveryToken,
                    walletName = intent.walletName,
                    intent.shouldResetKyc
                )
            is ResetPasswordIntents.RecoverAccount ->
                recoverAccount(
                    userId = intent.userId,
                    recoveryToken = intent.recoveryToken,
                    shouldResetKyc = intent.shouldResetKyc
                )
            ResetPasswordIntents.ResetUserKyc -> resetKyc()
            is ResetPasswordIntents.UpdateStatus -> null
        }
    }

    private fun setNewPassword(
        password: String,
        shouldResetKyc: Boolean
    ): Disposable {
        return interactor.setNewPassword(password = password)
            .subscribeBy(
                onComplete = { resetKycOrContinue(shouldResetKyc) },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ERROR))
                }
            )
    }

    private fun createWalletForAccount(
        email: String,
        password: String,
        userId: String,
        recoveryToken: String,
        walletName: String,
        shouldResetKyc: Boolean
    ): Disposable {
        return interactor.createWalletForAccount(email, password, walletName)
            .subscribeBy(
                onComplete = {
                    process(
                        ResetPasswordIntents.RecoverAccount(
                            userId = userId,
                            recoveryToken = recoveryToken,
                            shouldResetKyc = shouldResetKyc
                        )
                    )
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_WALLET_CREATION_FAILED))
                }
            )
    }

    private fun recoverAccount(userId: String, recoveryToken: String, shouldResetKyc: Boolean) =
        interactor.recoverAccount(userId = userId, recoveryToken = recoveryToken)
            .subscribeBy(
                onComplete = { resetKycOrContinue(shouldResetKyc) },
                onError = { throwable ->
                    process(
                        when {
                            shouldResetKyc && isErrorResponseConflict(throwable) ->
                                ResetPasswordIntents.ResetUserKyc
                            isErrorResponseConflict(throwable) ->
                                ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS)
                            else -> {
                                Timber.e(throwable)
                                ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_ACCOUNT_RESET_FAILED)
                            }
                        }
                    )
                }
            )

    private fun resetKyc() = interactor.resetUserKyc()
        .subscribeBy(
            onComplete = {
                process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS))
            },
            onError = { throwable ->
                if (isErrorResponseConflict(throwable)) {
                    // Resetting KYC is already in progress
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS))
                } else {
                    Timber.e(throwable)
                    process(ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_RESET_KYC_FAILED))
                }
            }
        )

    private fun resetKycOrContinue(shouldResetKyc: Boolean) {
        process(
            if (shouldResetKyc) {
                ResetPasswordIntents.ResetUserKyc
            } else {
                ResetPasswordIntents.UpdateStatus(ResetPasswordStatus.SHOW_SUCCESS)
            }
        )
    }

    // Recovery/Reset is already in progress
    private fun isErrorResponseConflict(throwable: Throwable) =
        throwable is NabuApiException && throwable.getErrorType() == NabuErrorTypes.Conflict
}
