package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.core.limits.FeatureWithLimit
import piuk.blockchain.android.ui.base.mvi.MviState

data class KycLimitsState(
    val isLoading: Boolean = false,
    val errorState: KycLimitsError = KycLimitsError.None,
    val activeSheet: KycLimitsSheet = KycLimitsSheet.None,
    val navigationAction: KycLimitsNavigationAction = KycLimitsNavigationAction.None,
    val header: Header = Header.HIDDEN,
    val currentKycTierRow: CurrentKycTierRow = CurrentKycTierRow.HIDDEN,
    val featuresWithLimits: List<FeatureWithLimit> = emptyList()
) : MviState

sealed class KycLimitsSheet {
    object None : KycLimitsSheet()
    data class UpgradeNow(val isGoldPending: Boolean) : KycLimitsSheet()
}

sealed class KycLimitsNavigationAction {
    object None : KycLimitsNavigationAction()
    object StartKyc : KycLimitsNavigationAction()
}

sealed class KycLimitsError {
    object None : KycLimitsError()
    data class FullscreenError(val exception: Throwable) : KycLimitsError()
    data class SheetError(val exception: Throwable) : KycLimitsError()
}

enum class Header {
    NEW_KYC,
    UPGRADE_TO_GOLD,
    MAX_TIER_REACHED,
    HIDDEN
}

enum class CurrentKycTierRow {
    SILVER,
    GOLD,
    HIDDEN
}
