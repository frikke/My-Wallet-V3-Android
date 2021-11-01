package piuk.blockchain.android.ui.dashboard.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.nabu.models.data.LinkBankTransfer
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails

sealed class DashboardNavigationAction {
    object StxAirdropComplete : DashboardNavigationAction()
    data class BackUpBeforeSend(val backupSheetDetails: BackupDetails) : DashboardNavigationAction()
    object SimpleBuyCancelOrder : DashboardNavigationAction()
    data class FiatFundsDetails(val fiatAccount: FiatAccount) : DashboardNavigationAction()
    data class LinkOrDeposit(val fiatAccount: FiatAccount? = null) : DashboardNavigationAction()
    object FiatFundsNoKyc : DashboardNavigationAction()
    data class InterestSummary(val account: SingleAccount, val asset: AssetInfo) : DashboardNavigationAction()
    data class PaymentMethods(
        val paymentMethodsForAction: LinkablePaymentMethodsForAction
    ) : DashboardNavigationAction()
    class LinkBankWithPartner(
        override val linkBankTransfer: LinkBankTransfer,
        override val fiatAccount: FiatAccount,
        override val assetAction: AssetAction
    ) : DashboardNavigationAction(), LinkBankNavigationAction

    fun isBottomSheet() =
        this !is LinkBankNavigationAction
}

interface LinkBankNavigationAction {
    val linkBankTransfer: LinkBankTransfer
    val fiatAccount: FiatAccount
    val assetAction: AssetAction
}