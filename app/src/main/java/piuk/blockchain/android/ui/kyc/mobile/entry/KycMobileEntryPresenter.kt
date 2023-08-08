package piuk.blockchain.android.ui.kyc.mobile.entry

import com.blockchain.core.settings.PhoneNumberUpdater
import com.blockchain.nabu.NabuUserSync
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.mobile.entry.models.PhoneDisplayModel
import timber.log.Timber

class KycMobileEntryPresenter(
    private val phoneNumberUpdater: PhoneNumberUpdater,
    private val nabuUserSync: NabuUserSync
) : BasePresenter<KycMobileEntryView>() {

    override fun onViewReady() {
        preFillPhoneNumber()
        subscribeToClickEvents()
    }

    private fun subscribeToClickEvents() {
        compositeDisposable +=
            view.uiStateObservable
                .map { it.first }
                .flatMapCompletable { number ->
                    phoneNumberUpdater.updateSms(number)
                        .flatMapCompletable {
                            nabuUserSync.syncUser()
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            view.showErrorSnackbar(
                                com.blockchain.stringResources.R.string.kyc_phone_number_error_saving_number
                            )
                        }
                        .doOnComplete {
                            view.continueSignUp(PhoneDisplayModel(number.raw, number.sanitized))
                        }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    private fun preFillPhoneNumber() {
        compositeDisposable +=
            phoneNumberUpdater.smsNumber()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        if (!it.isEmpty() && it.first() == '+') {
                            view.preFillPhoneNumber(it)
                        }
                    },
                    // Ignore error
                    onError = { Timber.e(it) }
                )
    }

    internal fun onProgressCancelled() {
        // Cancel outbound
        compositeDisposable.clear()
        // Resubscribe to everything
        subscribeToClickEvents()
    }
}
