package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.subscribeBy

class KycLimitsModel(
    private val interactor: KycLimitsInteractor,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<KycLimitsState, KycLimitsIntent>(KycLimitsState(), uiScheduler, environmentConfig, remoteLogger) {

    override fun performAction(previousState: KycLimitsState, intent: KycLimitsIntent): Disposable? = when (intent) {
        KycLimitsIntent.FetchLimitsAndTiers -> fetchLimitsAndTiers()
        KycLimitsIntent.UpgradeToGoldHeaderCtaClicked -> fetchIsGoldPendingAndNavigate()
        KycLimitsIntent.NewKycHeaderCtaClicked,
        is KycLimitsIntent.LimitsAndTiersFetched,
        is KycLimitsIntent.FetchLimitsAndTiersFailed,
        is KycLimitsIntent.OpenUpgradeNowSheet,
        KycLimitsIntent.CloseSheet,
        is KycLimitsIntent.FetchTiersFailed,
        KycLimitsIntent.ClearNavigation,
        KycLimitsIntent.NavigateToKyc -> null
    }

    private fun fetchLimitsAndTiers() = Singles.zip(
        interactor.fetchLimits(),
        interactor.fetchHighestApprovedTier(),
        interactor.fetchIsKycRejected()
    ).subscribeBy(
        onSuccess = { (limits, highestApprovedTier, isKycDenied) ->
            val header =
                if (isKycDenied) Header.MAX_TIER_REACHED
                else when (highestApprovedTier) {
                    KycTier.BRONZE -> Header.NEW_KYC
                    KycTier.SILVER -> Header.UPGRADE_TO_GOLD
                    KycTier.GOLD -> Header.MAX_TIER_REACHED
                }

            val currentKycTierRow =
                if (isKycDenied) CurrentKycTierRow.HIDDEN
                else when (highestApprovedTier) {
                    KycTier.BRONZE -> CurrentKycTierRow.HIDDEN
                    KycTier.SILVER -> CurrentKycTierRow.SILVER
                    KycTier.GOLD -> CurrentKycTierRow.GOLD
                }
            process(KycLimitsIntent.LimitsAndTiersFetched(limits, header, currentKycTierRow))
        },
        onError = {
            process(KycLimitsIntent.FetchLimitsAndTiersFailed(it))
        }
    )

    private fun fetchIsGoldPendingAndNavigate() =
        interactor.fetchIsGoldKycPending().subscribeBy(
            onSuccess = { isGoldPending ->
                if (isGoldPending) process(KycLimitsIntent.OpenUpgradeNowSheet(isGoldPending))
                else process(KycLimitsIntent.NavigateToKyc)
            },
            onError = {
                process(KycLimitsIntent.FetchTiersFailed(it))
            }
        )
}
