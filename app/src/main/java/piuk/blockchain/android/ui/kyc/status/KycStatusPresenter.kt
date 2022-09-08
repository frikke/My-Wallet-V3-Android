package piuk.blockchain.android.ui.kyc.status

import com.blockchain.notifications.NotificationTokenManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import timber.log.Timber

class KycStatusPresenter(
    private val kycStatusHelper: KycStatusHelper,
    private val notificationTokenManager: NotificationTokenManager
) : BasePresenter<KycStatusView>() {

    override fun onViewReady() {
        compositeDisposable +=
            kycStatusHelper.getKycTierStatus()
                .map { it.highestActiveLevelState() }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnEvent { _, _ -> view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribeBy(
                    onSuccess = { view.renderUi(it) },
                    onError = { view.finishPage() }
                )
    }

    internal fun onClickNotifyUser() {
        compositeDisposable +=
            notificationTokenManager.enableNotifications()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnEvent { view.dismissProgressDialog() }
                .subscribeBy(
                    onComplete = {
                        view.showNotificationsEnabledDialog()
                    },
                    onError = {
                        view.showSnackbar(R.string.kyc_status_button_notifications_error)
                        Timber.e(it)
                    }
                )
    }

    internal fun onClickContinue() {
        view.startExchange()
    }

    internal fun onProgressCancelled() {
        // Cancel subscriptions
        compositeDisposable.clear()
        // Exit page as UI won't be rendered
        view.finishPage()
    }
}
