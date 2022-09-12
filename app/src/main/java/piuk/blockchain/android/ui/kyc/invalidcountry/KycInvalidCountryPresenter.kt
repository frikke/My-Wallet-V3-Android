package piuk.blockchain.android.ui.kyc.invalidcountry

import com.blockchain.nabu.datamanagers.NabuDataManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.ui.base.BasePresenter
import timber.log.Timber

class KycInvalidCountryPresenter(
    private val nabuDataManager: NabuDataManager,
) : BasePresenter<KycInvalidCountryView>() {

    override fun onViewReady() = Unit

    internal fun onNoThanks() {
        compositeDisposable +=
            recordCountryCode(false)
                .subscribe()
    }

    internal fun onNotifyMe() {
        compositeDisposable +=
            recordCountryCode(true)
                .subscribe()
    }

    private fun recordCountryCode(notifyMe: Boolean): Completable =
        createUserAndStoreInMetadata()
            .flatMapCompletable { jwt ->
                nabuDataManager.recordCountrySelection(
                    jwt,
                    view.displayModel.countryCode,
                    view.displayModel.state,
                    notifyMe
                ).subscribeOn(Schedulers.io())
            }
            .doOnError { Timber.e(it) }
            // No need to notify users that this has failed
            .onErrorComplete()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { view.finishPage() }
            .doOnSubscribe { view.showProgressDialog() }
            .doOnTerminate { view.dismissProgressDialog() }

    private fun createUserAndStoreInMetadata(): Single<String> =
        nabuDataManager.requestJwt()
            .subscribeOn(Schedulers.io())

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
    }
}
