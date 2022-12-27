package piuk.blockchain.android.ui.interest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.chrome.navigation.TransactionFlowNavigation
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.earn.EarnAnalytics
import com.blockchain.earn.dashboard.EarnDashboardFragment
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.HomeLaunch
import com.blockchain.koin.payloadScope
import com.blockchain.utils.consume
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.core.parameter.parametersOf
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityInterestDashboardBinding
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class EarnDashboardActivity :
    BlockchainActivity(), EarnDashboardFragment.Host {

    private val binding: ActivityInterestDashboardBinding by lazy {
        ActivityInterestDashboardBinding.inflate(layoutInflater)
    }

    override val applyModeBackground: Boolean = true

    private val compositeDisposable = CompositeDisposable()

    override val alwaysDisableScreenshots: Boolean = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbarBackground(applyModeBackground = true, mutedBackground = false)
        updateToolbar(
            toolbarTitle = getString(R.string.earn_dashboard_title),
            backAction = { onSupportNavigateUp() }
        )
        analytics.logEvent(EarnAnalytics.InterestViewed)

        goToInterestDashboardFragment()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private val transactionFlowNavigation: TransactionFlowNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private val assetsActionsNavigation: AssetActionsNavigation = payloadScope.get {
        parametersOf(
            this
        )
    }

    private fun goToInterestDashboardFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                EarnDashboardFragment.newInstance(),
                EarnDashboardFragment::class.simpleName
            )
            .commitAllowingStateLoss()
    }

    companion object {
        const val ACTIVITY_ACCOUNT = "ACTIVITY_ACCOUNT"
        fun newInstance(context: Context) =
            Intent(context, EarnDashboardActivity::class.java)
    }

    override fun goToInterestDeposit(toAccount: BlockchainAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.InterestDeposit,
            target = toAccount as TransactionTarget
        )
    }

    override fun goToInterestWithdraw(fromAccount: BlockchainAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.InterestWithdraw,
            sourceAccount = fromAccount
        )
    }

    override fun launchStakingWithdrawal(account: StakingAccount) {
    }

    override fun launchStakingDeposit(account: StakingAccount) {
        transactionFlowNavigation.startTransactionFlow(
            action = AssetAction.StakingDeposit,
            target = account as TransactionTarget
        )
    }

    override fun startKycClicked() {
        KycNavHostActivity.startForResult(this, CampaignType.None, HomeLaunch.KYC_STARTED)
    }

    override fun launchReceive(cryptoTicker: String?) {
        assetsActionsNavigation.navigate(AssetAction.Receive)
    }

    override fun launchBuySell(viewType: BuySellViewType, asset: AssetInfo?, reload: Boolean) {
        assetsActionsNavigation.buyCrypto(
            currency = asset!!,
            amount = null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HomeLaunch.KYC_STARTED) {
            finish()
        }
    }
}
