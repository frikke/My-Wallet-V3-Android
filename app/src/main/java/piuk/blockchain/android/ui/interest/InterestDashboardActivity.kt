package piuk.blockchain.android.ui.interest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityInterestDashboardBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class InterestDashboardActivity :
    BlockchainActivity(),
    InterestSummarySheet.Host,
    InterestDashboardFragment.InterestDashboardHost {

    private val binding: ActivityInterestDashboardBinding by lazy {
        ActivityInterestDashboardBinding.inflate(layoutInflater)
    }

    private val compositeDisposable = CompositeDisposable()

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val fragment: InterestDashboardFragment by lazy { InterestDashboardFragment.newInstance() }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        loadToolbar(
            titleToolbar = getString(R.string.rewards_dashboard_title),
            backAction = { onSupportNavigateUp() }
        )
        analytics.logEvent(InterestAnalytics.InterestViewed)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment, InterestDashboardFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun goToActivityFor(account: BlockchainAccount) {
        val b = Bundle()
        b.putAccount(ACTIVITY_ACCOUNT, account)
        setResult(
            RESULT_FIRST_USER,
            Intent().apply {
                putExtras(b)
            }
        )
        finish()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun goToInterestDeposit(toAccount: InterestAccount) {
        clearBottomSheet()
        require(toAccount is CryptoAccount)
        startActivityForResult(
            TransactionFlowActivity.newInstance(
                context = this,
                target = toAccount,
                action = AssetAction.InterestDeposit
            ),
            TX_FLOW_REQUEST
        )
    }

    override fun goToInterestWithdraw(fromAccount: InterestAccount) {
        clearBottomSheet()
        require(fromAccount is CryptoAccount)
        startActivityForResult(
            TransactionFlowActivity.newInstance(
                context = this,
                sourceAccount = fromAccount,
                action = AssetAction.InterestWithdraw
            ),
            TX_FLOW_REQUEST
        )
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun startKyc() {
        analytics.logEvent(InterestAnalytics.InterestDashboardKyc)
        KycNavHostActivity.start(this, CampaignType.Interest)
    }

    override fun showInterestSummarySheet(account: SingleAccount, asset: AssetInfo) {
        showBottomSheet(InterestSummarySheet.newInstance(account, asset))
    }

    override fun startAccountSelection(
        filter: Single<List<BlockchainAccount>>,
        toAccount: SingleAccount
    ) {
        showBottomSheet(
            AccountSelectSheet.newInstance(
                object : AccountSelectSheet.SelectionHost {
                    override fun onAccountSelected(account: BlockchainAccount) {
                        startDeposit(account as SingleAccount, toAccount)
                        analytics.logEvent(
                            InterestAnalytics.InterestDepositClicked(
                                currency = (toAccount as CryptoAccount).asset.networkTicker,
                                origin = LaunchOrigin.SAVINGS_PAGE
                            )
                        )
                    }

                    override fun onSheetClosed() {
                        // do nothing
                    }
                },
                filter, R.string.select_deposit_source_title
            )
        )
    }

    private fun startDeposit(
        fromAccount: SingleAccount,
        toAccount: SingleAccount
    ) = startActivityForResult(
        TransactionFlowActivity.newInstance(
            context = this,
            sourceAccount = fromAccount as CryptoAccount,
            target = toAccount,
            action = AssetAction.InterestDeposit
        ),
        TX_FLOW_REQUEST
    )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == TX_FLOW_REQUEST) {
            fragment.refreshBalances()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val TX_FLOW_REQUEST = 321
        const val ACTIVITY_ACCOUNT = "ACTIVITY_ACCOUNT"
        fun newInstance(context: Context) =
            Intent(context, InterestDashboardActivity::class.java)
    }
}
