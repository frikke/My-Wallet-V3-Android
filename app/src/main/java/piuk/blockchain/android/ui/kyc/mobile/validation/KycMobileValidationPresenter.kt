package piuk.blockchain.android.ui.kyc.mobile.validation

import com.blockchain.analytics.Analytics
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.questionnaire.toMutableNode
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import timber.log.Timber

class KycMobileValidationPresenter(
    private val nabuUserSync: NabuUserSync,
    private val phoneNumberUpdater: PhoneNumberUpdater,
    private val kycDataManager: KycDataManager,
    private val analytics: Analytics
) : BasePresenter<KycMobileValidationView>() {

    override fun onViewReady() {
        setupRxEvents()
    }

    private fun setupRxEvents() {
        compositeDisposable +=
            view.uiStateObservable
                .flatMapSingle { (verificationModel, _) ->
                    phoneNumberUpdater.verifySms(verificationModel.verificationCode.code)
                        .flatMapCompletable { nabuUserSync.syncUser() }
                        .andThen(
                            rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
                                kycDataManager.getQuestionnaire()
                            }
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate {
                            view.dismissProgressDialog()
                        }
                        .doOnError {
                            Timber.e(it)
                            view.displayErrorDialog(R.string.kyc_phone_number_validation_error_incorrect)
                        }
                        .doOnSuccess { questionnaire ->
                            if (questionnaire.isNotEmpty()) {
                                view.navigateToQuestionnaire(questionnaire.toMutableNode())
                            } else {
                                view.navigateToVeriff()
                            }
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
                            view.displayErrorDialog(R.string.kyc_phone_number_error_resending)
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
