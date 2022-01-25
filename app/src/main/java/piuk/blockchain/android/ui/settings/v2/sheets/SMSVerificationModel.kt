package piuk.blockchain.android.ui.settings.v2.sheets

import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class SMSVerificationModel(
    initialState: SMSVerificationState,
    mainScheduler: Scheduler,
    private val interactor: SMSVerificationInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<SMSVerificationState, SMSVerificationIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(
        previousState: SMSVerificationState,
        intent: SMSVerificationIntent
    ): Disposable? =
        when (intent) {
            is SMSVerificationIntent.ResendSMS -> {
                interactor.resendCodeSMS(
                    mobileWithPrefix = intent.phoneNumber
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(SMSVerificationIntent.ResendCodeSMSSucceeded)
                        },
                        onError = {
                            Timber.e("ResendSMS failure $it")
                            process(SMSVerificationIntent.ResendCodeSMSFailed)
                        }
                    )
            }
            is SMSVerificationIntent.VerifySMSCode -> {
                interactor.verifyPhoneNumber(
                    code = intent.code
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onComplete = {
                            process(SMSVerificationIntent.VerifyPhoneNumberSucceeded)
                        },
                        onError = {
                            Timber.e("VerifySMSCode failure $it")
                            process(SMSVerificationIntent.VerifyPhoneNumberFailed)
                        }
                    )
            }
            is SMSVerificationIntent.ResendCodeSMSSucceeded,
            is SMSVerificationIntent.ResendCodeSMSFailed,
            is SMSVerificationIntent.ResetCodeSentVerification,
            is SMSVerificationIntent.VerifyPhoneNumberFailed,
            is SMSVerificationIntent.VerifyPhoneNumberSucceeded,
            is SMSVerificationIntent.ClearErrors -> null
        }
}
