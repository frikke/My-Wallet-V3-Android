package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.eth.L2NonCustodialAccount
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.preferences.OnboardingPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.ui.customviews.account.AccountLocks
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.home.ActionActivity
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.locks.LocksDetailsActivity
import piuk.blockchain.android.ui.transactionflow.analytics.SendAnalyticsEvent
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.ui.transactionflow.flow.send.SendNetworkWarningSheet
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent

class TransferSendFragment : AccountSelectorFragment(), SendNetworkWarningSheet.Host {

    private val analytics: Analytics by inject()
    private val compositeDisposable = CompositeDisposable()

    override val fragmentAction: AssetAction
        get() = AssetAction.Send

    private lateinit var selectedSource: CryptoAccount

    private val onboardingPrefs: OnboardingPrefs by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderList()
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun renderList() {
        setEmptyStateDetails(
            com.blockchain.stringResources.R.string.transfer_wallets_empty_title,
            com.blockchain.stringResources.R.string.transfer_wallets_empty_details,
            com.blockchain.stringResources.R.string.transfer_wallet_buy_crypto
        ) {
            analytics.logEvent(TransferAnalyticsEvent.NoBalanceCtaClicked)
            analytics.logEvent(
                BuySellClicked(
                    origin = LaunchOrigin.SEND,
                    type = BuySellViewType.TYPE_BUY
                )
            )
            (activity as? HomeNavigator)?.launchBuySell() ?: (activity as? ActionActivity)?.navigateToBuy()
        }

        initialiseAccountSelectorWithHeader(
            statusDecorator = ::statusDecorator,
            onAccountSelected = ::doOnAccountSelected,
            onExtraAccountInfoClicked = ::onExtraAccountInfoClicked
        )
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        if (account is CryptoAccount) {
            SendCellDecorator(account)
        } else {
            DefaultCellDecorator()
        }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        require(account is CryptoAccount)

        val shouldShowNetworkWarningSheet = !onboardingPrefs.isSendNetworkWarningDismissed &&
            account is L2NonCustodialAccount

        if (shouldShowNetworkWarningSheet) {
            require(account is L2NonCustodialAccount)
            selectedSource = account
            showBottomSheet(
                SendNetworkWarningSheet.newInstance(account.currency.displayTicker, account.l1Network.shortName)
            )
        } else {
            startTransactionFlow(account)
        }
    }

    private fun onExtraAccountInfoClicked(accountLocks: AccountLocks) {
        require(accountLocks.fundsLocks != null) { "fundsLocks are null" }
        LocksDetailsActivity.start(requireContext(), accountLocks.fundsLocks)
    }

    private fun startTransactionFlow(fromAccount: CryptoAccount) {
        analytics.logEvent(TransferAnalyticsEvent.SourceWalletSelected(fromAccount))
        analytics.logEvent(
            SendAnalyticsEvent.SendSourceAccountSelected(
                currency = fromAccount.currency.networkTicker,
                fromAccountType = TxFlowAnalyticsAccountType.fromAccount(
                    fromAccount
                )
            )
        )
        startActivity(
            TransactionFlowActivity.newIntent(
                context = requireActivity(),
                sourceAccount = fromAccount,
                action = AssetAction.Send
            )
        )
    }

    override fun doOnEmptyList() {
        super.doOnEmptyList()
        analytics.logEvent(TransferAnalyticsEvent.NoBalanceViewDisplayed)
    }

    override fun onSheetClosed() {
        onboardingPrefs.isSendNetworkWarningDismissed = true
        startTransactionFlow(selectedSource)
    }

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment?) =
        bottomSheet?.show(childFragmentManager, BOTTOM_SHEET)

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"

        fun newInstance() = TransferSendFragment()
    }
}
