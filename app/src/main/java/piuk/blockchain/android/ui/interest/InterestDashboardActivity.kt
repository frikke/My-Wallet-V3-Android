package piuk.blockchain.android.ui.interest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.extensions.exhaustive
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.orderRewardsFeatureFlag
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityInterestDashboardBinding
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardFragment
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardNavigationEvent
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardSharedViewModel
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class InterestDashboardActivity :
    BlockchainActivity(),
    InterestSummarySheet.Host,
    InterestDashboardFragmentLegacy.InterestDashboardHost,
    NavigationRouter<InterestDashboardNavigationEvent> {

    private val binding: ActivityInterestDashboardBinding by lazy {
        ActivityInterestDashboardBinding.inflate(layoutInflater)
    }

    private val sharedViewModel: InterestDashboardSharedViewModel by viewModel()

    private val compositeDisposable = CompositeDisposable()

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val fragmentLegacy: InterestDashboardFragmentLegacy by lazy {
        InterestDashboardFragmentLegacy.newInstance()
    }

    private val orderRewardsFF: FeatureFlag by inject(orderRewardsFeatureFlag)

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.rewards_dashboard_title),
            backAction = { onSupportNavigateUp() }
        )
        analytics.logEvent(InterestAnalytics.InterestViewed)

        orderRewardsFF.enabled.subscribe { enabled ->
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.content_frame,
                    if (enabled) InterestDashboardFragment.newInstance() else fragmentLegacy,
                    InterestDashboardFragment::class.simpleName
                )
                .commitAllowingStateLoss()
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun route(navigationEvent: InterestDashboardNavigationEvent) {
        when (navigationEvent) {
            is InterestDashboardNavigationEvent.InterestSummary -> {
                showInterestSummarySheet(navigationEvent.account)
            }

            is InterestDashboardNavigationEvent.InterestDeposit -> {
                goToInterestDeposit(navigationEvent.account)
            }

            InterestDashboardNavigationEvent.StartKyc -> {
                startKyc()
            }
        }.exhaustive
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

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        clearBottomSheet()
        require(toAccount is CryptoAccount)
        startActivityForResult(
            TransactionFlowActivity.newIntent(
                context = this,
                target = toAccount,
                action = AssetAction.InterestDeposit
            ),
            TX_FLOW_REQUEST
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        clearBottomSheet()
        require(fromAccount is CryptoAccount)
        startActivityForResult(
            TransactionFlowActivity.newIntent(
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

    override fun showInterestSummarySheet(account: CryptoAccount) {
        showBottomSheet(InterestSummarySheet.newInstance(account))
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
                                currency = toAccount.currency.networkTicker,
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
        TransactionFlowActivity.newIntent(
            context = this,
            sourceAccount = fromAccount as CryptoAccount,
            target = toAccount,
            action = AssetAction.InterestDeposit
        ),
        TX_FLOW_REQUEST
    )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == TX_FLOW_REQUEST) {
            orderRewardsFF.enabled.subscribe { enabled ->
                if (enabled) {
                    sharedViewModel.requestBalanceRefresh()
                } else {
                    fragmentLegacy.refreshBalances()
                }
            }
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
