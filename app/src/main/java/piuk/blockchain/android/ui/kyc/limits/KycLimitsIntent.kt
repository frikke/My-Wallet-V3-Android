package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.core.limits.FeatureWithLimit
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class KycLimitsIntent : MviIntent<KycLimitsState> {
    object FetchLimitsAndTiers : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState = oldState.copy(
            isLoading = true
        )
    }

    data class LimitsAndTiersFetched(
        private val limits: List<FeatureWithLimit>,
        private val header: Header,
        private val currentKycTierRow: CurrentKycTierRow
    ) : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState =
            oldState.copy(
                isLoading = false,
                featuresWithLimits = limits,
                errorState = KycLimitsError.None,
                header = header,
                currentKycTierRow = currentKycTierRow
            )
    }

    data class FetchLimitsAndTiersFailed(private val error: Throwable) : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState = oldState.copy(
            isLoading = false,
            errorState = KycLimitsError.FullscreenError(error)
        )
    }

    data class FetchTiersFailed(private val error: Throwable) : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState = oldState.copy(
            errorState = KycLimitsError.SheetError(error)
        )
    }

    object NewKycHeaderCtaClicked : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState =
            oldState.copy(activeSheet = KycLimitsSheet.UpgradeNow(isGoldPending = false))
    }

    object UpgradeToGoldHeaderCtaClicked : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState = oldState
    }

    data class OpenUpgradeNowSheet(
        private val isGoldPending: Boolean
    ) : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState =
            oldState.copy(activeSheet = KycLimitsSheet.UpgradeNow(isGoldPending))
    }

    object CloseSheet : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState =
            oldState.copy(activeSheet = KycLimitsSheet.None)
    }

    object NavigateToKyc : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState =
            oldState.copy(navigationAction = KycLimitsNavigationAction.StartKyc)
    }

    object ClearNavigation : KycLimitsIntent() {
        override fun reduce(oldState: KycLimitsState): KycLimitsState =
            oldState.copy(navigationAction = KycLimitsNavigationAction.None)
    }
}
