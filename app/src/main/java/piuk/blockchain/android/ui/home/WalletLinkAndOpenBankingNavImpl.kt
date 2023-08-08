package piuk.blockchain.android.ui.home

import com.blockchain.chrome.navigation.WalletLinkAndOpenBankingNavigation
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.BankLinkingInfo
import com.blockchain.domain.paymentmethods.model.FiatTransactionState
import com.blockchain.extensions.exhaustive
import com.blockchain.home.presentation.navigation.HomeLaunch
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.auth.AccountWalletLinkAlertSheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.yapily.FiatTransactionBottomSheet

class WalletLinkAndOpenBankingNavImpl(private val activity: BlockchainActivity?) : WalletLinkAndOpenBankingNavigation {
    override fun walletLinkError(walletIdHint: String) {
        activity?.showBottomSheet(
            AccountWalletLinkAlertSheet.newInstance(walletIdHint)
        )
    }

    override fun depositComplete(amount: Money, estimationTime: String) {
        activity?.replaceBottomSheet(
            FiatTransactionBottomSheet.newInstance(
                amount.currencyCode,
                activity.getString(
                    com.blockchain.stringResources.R.string.deposit_confirmation_success_title,
                    amount.toStringWithSymbol()
                ),
                activity.getString(
                    com.blockchain.stringResources.R.string.yapily_fiat_deposit_success_subtitle,
                    amount.toStringWithSymbol(),
                    amount.currencyCode,
                    estimationTime
                ),
                FiatTransactionState.SUCCESS
            )
        )
    }

    override fun depositInProgress(orderValue: Money) {
        activity?.replaceBottomSheet(
            FiatTransactionBottomSheet.newInstance(
                orderValue.currencyCode,
                activity.getString(com.blockchain.stringResources.R.string.deposit_confirmation_pending_title),
                activity.getString(
                    com.blockchain.stringResources.R.string.deposit_confirmation_pending_subtitle
                ),
                FiatTransactionState.PENDING
            )
        )
    }

    override fun openBankingTimeout(currency: FiatCurrency) {
        activity?.replaceBottomSheet(
            FiatTransactionBottomSheet.newInstance(
                currency.displayTicker,
                activity.getString(com.blockchain.stringResources.R.string.deposit_confirmation_pending_title),
                activity.getString(
                    com.blockchain.stringResources.R.string.deposit_confirmation_pending_subtitle
                ),
                FiatTransactionState.ERROR
            )
        )
    }

    override fun approvalError() {
        BlockchainSnackbar.make(
            activity!!.findViewById(android.R.id.content),
            activity.getString(com.blockchain.stringResources.R.string.simple_buy_confirmation_error),
            type = SnackbarType.Error
        ).show()
    }

    override fun openBankingError() {
        BlockchainSnackbar.make(
            activity!!.findViewById(android.R.id.content),
            activity.getString(com.blockchain.stringResources.R.string.simple_buy_confirmation_error),
            type = SnackbarType.Error
        ).show()
    }

    override fun openBankingError(currency: FiatCurrency) {
        activity?.replaceBottomSheet(
            FiatTransactionBottomSheet.newInstance(
                currency.displayTicker,
                activity.getString(com.blockchain.stringResources.R.string.deposit_confirmation_error_title),
                activity.getString(
                    com.blockchain.stringResources.R.string.deposit_confirmation_error_subtitle
                ),
                FiatTransactionState.ERROR
            )
        )
    }

    override fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo) {
        activity?.startActivityForResult(
            BankAuthActivity.newInstance(bankLinkingInfo.linkingId, bankLinkingInfo.bankAuthSource, activity),
            when (bankLinkingInfo.bankAuthSource) {
                BankAuthSource.SIMPLE_BUY -> HomeLaunch.BANK_DEEP_LINK_SIMPLE_BUY
                BankAuthSource.SETTINGS -> HomeLaunch.BANK_DEEP_LINK_SETTINGS
                BankAuthSource.DEPOSIT -> HomeLaunch.BANK_DEEP_LINK_DEPOSIT
                BankAuthSource.WITHDRAW -> HomeLaunch.BANK_DEEP_LINK_WITHDRAW
            }.exhaustive
        )
    }

    override fun paymentForCancelledOrder(currency: FiatCurrency) {
        activity?.replaceBottomSheet(
            FiatTransactionBottomSheet.newInstance(
                currency.displayTicker,
                activity.getString(
                    com.blockchain.stringResources.R.string.yapily_payment_to_fiat_wallet_title,
                    currency.displayTicker
                ),
                activity.getString(
                    com.blockchain.stringResources.R.string.yapily_payment_to_fiat_wallet_subtitle,
                    currency.displayTicker,
                    currency.displayTicker
                ),
                FiatTransactionState.SUCCESS
            )
        )
    }

    override fun launchSimpleBuyFromLinkApproval() {
        activity?.startActivity(SimpleBuyActivity.newIntent(activity, launchFromApprovalDeepLink = true))
    }
}
