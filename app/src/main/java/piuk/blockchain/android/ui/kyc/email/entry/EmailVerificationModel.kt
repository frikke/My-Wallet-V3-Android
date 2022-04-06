package piuk.blockchain.android.ui.kyc.email.entry

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class EmailVerificationModel(
    private val interactor: EmailVerificationInteractor,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<EmailVerificationState, EmailVerificationIntent>(
    EmailVerificationState(), uiScheduler, environmentConfig, remoteLogger
) {

    override fun performAction(previousState: EmailVerificationState, intent: EmailVerificationIntent): Disposable? =
        when (intent) {
            EmailVerificationIntent.FetchEmail -> interactor.fetchEmail().subscribeBy(
                onSuccess = {
                    process(EmailVerificationIntent.EmailUpdated(it))
                }, onError = {
                process(EmailVerificationIntent.ErrorEmailVerification)
            }
            )
            EmailVerificationIntent.CancelEmailVerification -> interactor.cancelPolling().subscribeBy(
                onComplete = {},
                onError = {}
            )
            EmailVerificationIntent.StartEmailVerification ->
                interactor.fetchEmail().flatMapObservable { email ->
                    if (!email.isVerified) {
                        interactor.pollForEmailStatus().toObservable().startWithItem(email)
                    } else {
                        Observable.just(email)
                    }
                }.subscribeBy(
                    onNext = {
                        process(EmailVerificationIntent.EmailUpdated(it))
                    }, onError = {
                    process(EmailVerificationIntent.ErrorEmailVerification)
                }
                )

            EmailVerificationIntent.ResendEmail -> interactor.fetchEmail()
                .flatMap { interactor.resendEmail(it.address) }
                .subscribeBy(onSuccess = {
                    process(EmailVerificationIntent.EmailUpdated(it))
                }, onError = {})

            EmailVerificationIntent.UpdateEmail -> {
                check(previousState.emailInput != null)
                interactor.updateEmail(previousState.emailInput).subscribeBy(onSuccess = {
                    process(EmailVerificationIntent.EmailUpdated(it))
                }, onError = {
                    process(EmailVerificationIntent.EmailUpdateFailed)
                })
            }
            else -> null
        }
}
