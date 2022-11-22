package piuk.blockchain.android.ui.interest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.extensions.exhaustive
import com.blockchain.utils.consume
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityInterestDashboardBinding
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardFragment
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardNavigationEvent
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardSharedViewModel
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.analytics.EarnAnalytics
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.util.putAccount

class InterestDashboardActivity :
    BlockchainActivity(),
    InterestSummarySheet.Host,
    NavigationRouter<InterestDashboardNavigationEvent> {

    private val binding: ActivityInterestDashboardBinding by lazy {
        ActivityInterestDashboardBinding.inflate(layoutInflater)
    }

    private val sharedViewModel: InterestDashboardSharedViewModel by viewModel()

    private val compositeDisposable = CompositeDisposable()

    override val alwaysDisableScreenshots: Boolean = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private var transactionFlowActivityLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        sharedViewModel.requestBalanceRefresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.rewards_dashboard_title),
            backAction = { onSupportNavigateUp() }
        )
        analytics.logEvent(EarnAnalytics.InterestViewed)

        goToInterestDashboardFragment()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressedDispatcher.onBackPressed()
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

    private fun goToInterestDashboardFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                InterestDashboardFragment.newInstance(),
                InterestDashboardFragment::class.simpleName
            )
            .commitAllowingStateLoss()
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        clearBottomSheet()
        require(toAccount is CryptoAccount)

        transactionFlowActivityLauncher.launch(
            TransactionFlowActivity.newIntent(
                context = this,
                target = toAccount,
                action = AssetAction.InterestDeposit
            )
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        clearBottomSheet()
        require(fromAccount is CryptoAccount)

        transactionFlowActivityLauncher.launch(
            TransactionFlowActivity.newIntent(
                context = this,
                sourceAccount = fromAccount,
                action = AssetAction.InterestWithdraw
            )
        )
    }

    override fun onSheetClosed() {
        // do nothing
    }

    private fun startKyc() {
        analytics.logEvent(EarnAnalytics.InterestDashboardKyc)
        KycNavHostActivity.start(this, CampaignType.Interest)
    }

    private fun showInterestSummarySheet(account: CryptoAccount) {
        showBottomSheet(InterestSummarySheet.newInstance(account))
    }

    companion object {
        const val ACTIVITY_ACCOUNT = "ACTIVITY_ACCOUNT"
        fun newInstance(context: Context) =
            Intent(context, InterestDashboardActivity::class.java)
    }
}
