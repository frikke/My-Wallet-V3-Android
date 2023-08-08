package piuk.blockchain.android.ui.kyc.tiersplash

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BasePresenter
import timber.log.Timber

class KycTierSplashPresenter(
    private val kycService: KycService,
    private val analytics: Analytics
) : BasePresenter<KycTierSplashView>() {

    override fun onViewReady() {}

    override fun onViewResumed() {
        super.onViewResumed()
        compositeDisposable +=
            kycService.getTiersLegacy()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Timber::e)
                .subscribeBy(
                    onSuccess = { tiers ->
                        reportState(
                            tiers.tierForLevel(KycTier.SILVER).state,
                            tiers.tierForLevel(KycTier.GOLD).state
                        )
                    },
                    onError = {
                        view!!.showError(com.blockchain.stringResources.R.string.kyc_non_specific_server_error)
                    }
                )
    }

    private fun reportState(
        silverKycState: KycTierState,
        goldKycState: KycTierState
    ) {
        val pendingOrApproved = listOf(KycTierState.Pending, KycTierState.Verified)
        when {
            goldKycState in pendingOrApproved -> analytics.logEvent(AnalyticsEvents.KycTier2Complete)
            silverKycState in pendingOrApproved -> analytics.logEvent(AnalyticsEvents.KycTier1Complete)
            silverKycState == KycTierState.None -> analytics.logEvent(AnalyticsEvents.KycTiersLocked)
        }
    }

    override fun onViewPaused() {
        compositeDisposable.clear()
        super.onViewPaused()
    }
}
