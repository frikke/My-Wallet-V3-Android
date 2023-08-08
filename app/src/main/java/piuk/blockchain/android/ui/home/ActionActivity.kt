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
import com.blockchain.logging.RemoteLogger
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.stringResources.R
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityActionBinding
import piuk.blockchain.android.simplebuy.sheets.BuyPendingOrdersBottomSheet
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.brokerage.BuySellFragment
import piuk.blockchain.android.ui.swap.SwapFragment
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment

private const val receiveErrorMsg = "Old receive tried to open - review any missing places"
private object LegacyReceiveException : Exception(receiveErrorMsg)

class ActionActivity :
    BlockchainActivity(),
    SlidingModalBottomDialog.Host,
    SwapFragment.Host,
    BuyPendingOrdersBottomSheet.Host,
    KycUpgradeNowSheet.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = false

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

    private val remoteLogger: RemoteLogger by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbarBackground(mutedBackground = true)

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
                remoteLogger.logException(LegacyReceiveException, receiveErrorMsg)
                error(receiveErrorMsg)
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
                    asset = cryptoTicker?.let { assetCatalogue.fromNetworkTicker(it) as? AssetInfo },
                    fromRecurringBuy = intent.getBooleanExtra(ARG_FROM_RECURRING_BUY, false)
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

    override fun navigateBack() {
        finish()
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
            ActivityResult.StartBuyIntro -> intent.putExtra(RESULT_START_BUY_INTRO, true)
            ActivityResult.ViewActivity -> intent.putExtra(RESULT_VIEW_ACTIVITY, true)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        private const val ACTION = "action"
        private const val RESULT_START_KYC = "RESULT_START_KYC"
        private const val RESULT_START_BUY_INTRO = "RESULT_START_BUY_INTRO"
        private const val RESULT_VIEW_ACTIVITY = "RESULT_VIEW_ACTIVITY"
        private const val CRYPTO_TICKER = "CRYPTO_TICKER"
        private const val ARG_FROM_RECURRING_BUY = "ARG_FROM_RECURRING_BUY"

        private fun newIntent(
            context: Context,
            action: AssetAction,
            cryptoTicker: String? = null,
            fromRecurringBuy: Boolean = false
        ): Intent =
            Intent(context, ActionActivity::class.java).apply {
                putExtra(ACTION, action)
                cryptoTicker?.let {
                    putExtra(CRYPTO_TICKER, it)
                }
                putExtra(ARG_FROM_RECURRING_BUY, fromRecurringBuy)
            }
    }

    data class ActivityArgs(
        val action: AssetAction,
        val cryptoTicker: String? = null,
        val fromRecurringBuy: Boolean = false
    )

    sealed class ActivityResult {
        object StartKyc : ActivityResult()
        object StartBuyIntro : ActivityResult()
        object ViewActivity : ActivityResult()
    }

    class BlockchainActivityResultContract : ActivityResultContract<ActivityArgs, ActivityResult?>() {
        override fun createIntent(context: Context, input: ActivityArgs): Intent =
            newIntent(context, input.action, input.cryptoTicker, input.fromRecurringBuy)

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult? {
            val startKyc = intent?.getBooleanExtra(RESULT_START_KYC, false) ?: false
            val startBuyIntro = intent?.getBooleanExtra(RESULT_START_BUY_INTRO, false) ?: false

            return when {
                resultCode != Activity.RESULT_OK -> null
                startKyc -> ActivityResult.StartKyc
                startBuyIntro -> ActivityResult.StartBuyIntro
                else -> null
            }
        }
    }
}
