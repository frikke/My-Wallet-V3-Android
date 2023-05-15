package piuk.blockchain.android.ui.kyc.mobile.validation

import com.blockchain.core.settings.PhoneNumberUpdater
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.nabu.NabuUserSync
import com.blockchain.utils.rxMaybeOutcome
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import timber.log.Timber

class KycMobileValidationPresenter(
    private val nabuUserSync: NabuUserSync,
    private val phoneNumberUpdater: PhoneNumberUpdater,
    private val dataRemediationService: DataRemediationService
) : BasePresenter<KycMobileValidationView>() {

    override fun onViewReady() {
        setupRxEvents()
    }

    private fun setupRxEvents() {
        compositeDisposable +=
            view.uiStateObservable
                .flatMapMaybe { (verificationModel, _) ->
                    phoneNumberUpdater.verifySms(verificationModel.verificationCode.code)
                        .flatMapCompletable { nabuUserSync.syncUser() }
                        .andThen(
                            rxMaybeOutcome(Schedulers.io().asCoroutineDispatcher()) {
                                dataRemediationService.getQuestionnaire(QuestionnaireContext.TIER_TWO_VERIFICATION)
                            }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate {
                            view.dismissProgressDialog()
                        }
                        .doOnError {
                            Timber.e(it)
                            view.displayErrorDialog(
                                com.blockchain.stringResources.R.string.kyc_phone_number_validation_error_incorrect
                            )
                        }
                        .doOnSuccess { questionnaire ->
                            view.navigateToQuestionnaire(questionnaire)
                        }
                        .doOnComplete {
                            view.navigateToVeriff()
                        }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
        compositeDisposable +=
            view.resendObservable
                .flatMapCompletable { (phoneNumber, _) ->
                    phoneNumberUpdater.updateSms(phoneNumber)
                        .flatMapCompletable {
                            nabuUserSync.syncUser()
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            Timber.e(it)
                            view.displayErrorDialog(
                                com.blockchain.stringResources.R.string.kyc_phone_number_error_resending
                            )
                        }
                        .doOnComplete {
                            view.theCodeWasResent()
                        }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
        // Resubscribe
        setupRxEvents()
    }
}
