package piuk.blockchain.android.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.koin.scopedInject
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityActionBinding
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.brokerage.BuySellFragment
import piuk.blockchain.android.ui.swap.SwapFragment
import piuk.blockchain.android.ui.transfer.receive.ReceiveFragment
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment
import piuk.blockchain.android.ui.upsell.UpsellHost

class ActionActivity :
    BlockchainActivity(),
    SlidingModalBottomDialog.Host,
    UpsellHost,
    SwapFragment.Host,
    BuyPendingOrdersBottomSheet.Host,
    KycUpgradeNowSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val applyModeStatusbarColors: Boolean = true

    private val assetCatalogue: AssetCatalogue by scopedInject()

    private val binding: ActivityActionBinding by lazy {
        ActivityActionBinding.inflate(layoutInflater)
    }

    private val action: AssetAction by lazy {
        intent.getSerializableExtra(ACTION) as AssetAction
    }

    private val cryptoTicker: String? by lazy {
        intent.getStringExtra(CRYPTO_TICKER)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbarBackground(applyModeColors = true, mutedBackground = true)

        updateToolbarBackAction {
            onBackPressedDispatcher.onBackPressed()
        }

        supportFragmentManager.showFragment(
            fragment = loadFragment()
        )
    }

    private fun loadFragment(): Fragment {
        showLoading()
        return when (action) {
            AssetAction.Send -> {
                updateToolbarTitle(getString(R.string.toolbar_send))
                TransferSendFragment.newInstance()
            }
            AssetAction.Swap -> {
                updateToolbarTitle(getString(R.string.toolbar_swap))
                SwapFragment.newInstance()
            }
            AssetAction.Receive -> {
                updateToolbarTitle(getString(R.string.toolbar_receive))
                ReceiveFragment.newInstance(cryptoTicker = cryptoTicker)
            }
            AssetAction.Sell,
            AssetAction.Buy -> {
                BuySellFragment.newInstance(
                    viewType = when (action) {
                        AssetAction.Sell -> {
                            updateToolbarTitle(getString(R.string.common_sell))
                            BuySellViewType.TYPE_SELL
                        }
                        else -> {
                            updateToolbarTitle(getString(R.string.common_buy))
                            BuySellViewType.TYPE_BUY
                        }
                    },
                    asset = cryptoTicker?.let { assetCatalogue.fromNetworkTicker(it) as? AssetInfo }
                )
            }
            else -> {
                throw IllegalStateException("$action is not supported")
            }
        }
    }

    override fun showLoading() {
        with(binding.progress) {
            bringToFront()
            visible()
            playAnimation()
        }
    }

    override fun hideLoading() {
        with(binding.progress) {
            gone()
            pauseAnimation()
        }
    }

    override fun startUpsellKyc() {
        finishWithResult(ActivityResult.StartKyc)
    }

    override fun navigateBack() {
        finish()
    }

    override fun navigateToReceive() {
        finishWithResult(ActivityResult.StartReceive(cryptoTicker))
    }

    override fun navigateToBuy() {
        finishWithResult(ActivityResult.StartBuyIntro)
    }

    override fun startKycClicked() {
        finishWithResult(ActivityResult.StartKyc)
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun startActivityRequested() {
        finishWithResult(ActivityResult.ViewActivity)
    }

    private fun finishWithResult(result: ActivityResult) {
        val intent = Intent()
        when (result) {
            ActivityResult.StartKyc -> intent.putExtra(RESULT_START_KYC, true)
            is ActivityResult.StartReceive -> {
                intent.putExtra(RESULT_START_RECEIVE, true)
                intent.putExtra(CRYPTO_TICKER, cryptoTicker)
            }
            ActivityResult.StartBuyIntro -> intent.putExtra(RESULT_START_BUY_INTRO, true)
            ActivityResult.ViewActivity -> intent.putExtra(RESULT_VIEW_ACTIVITY, true)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        private const val ACTION = "action"
        private const val RESULT_START_KYC = "RESULT_START_KYC"
        private const val RESULT_START_RECEIVE = "RESULT_START_RECEIVE"
        private const val RESULT_START_BUY_INTRO = "RESULT_START_BUY_INTRO"
        private const val RESULT_VIEW_ACTIVITY = "RESULT_VIEW_ACTIVITY"
        private const val CRYPTO_TICKER = "CRYPTO_TICKER"

        private fun newIntent(context: Context, action: AssetAction, cryptoTicker: String? = null): Intent =
            Intent(context, ActionActivity::class.java).apply {
                putExtra(ACTION, action)
                cryptoTicker?.let {
                    putExtra(CRYPTO_TICKER, it)
                }
            }
    }

    data class ActivityArgs(val action: AssetAction, val cryptoTicker: String? = null)

    sealed class ActivityResult {
        object StartKyc : ActivityResult()
        class StartReceive(val cryptoTicker: String? = null) : ActivityResult()
        object StartBuyIntro : ActivityResult()
        object ViewActivity : ActivityResult()
    }

    class BlockchainActivityResultContract : ActivityResultContract<ActivityArgs, ActivityResult?>() {
        override fun createIntent(context: Context, input: ActivityArgs): Intent =
            newIntent(context, input.action, input.cryptoTicker)

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult? {
            val startKyc = intent?.getBooleanExtra(RESULT_START_KYC, false) ?: false
            val startReceive = intent?.getBooleanExtra(RESULT_START_RECEIVE, false) ?: false
            val startBuyIntro = intent?.getBooleanExtra(RESULT_START_BUY_INTRO, false) ?: false

            return when {
                resultCode != Activity.RESULT_OK -> null
                startKyc -> ActivityResult.StartKyc
                startReceive -> ActivityResult.StartReceive(intent?.getStringExtra(CRYPTO_TICKER))
                startBuyIntro -> ActivityResult.StartBuyIntro
                else -> null
            }
        }
    }
}
