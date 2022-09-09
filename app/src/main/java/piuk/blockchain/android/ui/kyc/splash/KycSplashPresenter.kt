package piuk.blockchain.android.ui.kyc.splash

import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import timber.log.Timber

class KycSplashPresenter(
    private val kycNavigator: KycNavigator
) : BasePresenter<KycSplashView>() {

    override fun onViewReady() {}

    fun onCTATapped() {
        goToNextKycStep()
    }

    private fun goToNextKycStep() {
        compositeDisposable += kycNavigator.findNextStep()
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = { view.goToNextKycStep(it) }
            )
    }
}
