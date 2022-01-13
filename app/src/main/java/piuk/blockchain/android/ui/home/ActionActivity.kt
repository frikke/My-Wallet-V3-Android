package piuk.blockchain.android.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import com.blockchain.coincore.AssetAction
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityActionBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.swap.SwapFragment
import piuk.blockchain.android.ui.transfer.receive.ReceiveFragment
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment
import piuk.blockchain.android.ui.upsell.UpsellHost
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class ActionActivity : BlockchainActivity(), SlidingModalBottomDialog.Host, UpsellHost, SwapFragment.Host {

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val binding: ActivityActionBinding by lazy {
        ActivityActionBinding.inflate(layoutInflater)
    }

    private val action: AssetAction by lazy {
        intent.getSerializableExtra(ACTION) as AssetAction
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbarBackAction {
            onBackPressed()
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
                ReceiveFragment.newInstance()
            }
            else -> {
                throw IllegalStateException("$action is not supported")
            }
        }
    }

    override fun showLoading() {
        binding.progress.visible()
        binding.progress.playAnimation()
    }

    override fun hideLoading() {
        binding.progress.gone()
        binding.progress.pauseAnimation()
    }

    override fun startUpsellKyc() {
        finishWithResult(ActivityResult.StartKyc)
    }

    override fun navigateToReceive() {
        finishWithResult(ActivityResult.StartReceive)
    }

    override fun navigateToBuy() {
        finishWithResult(ActivityResult.StartBuyIntro)
    }

    override fun onSheetClosed() {
        // do nothing
    }

    private fun finishWithResult(result: ActivityResult) {
        val intent = Intent()
        when (result) {
            ActivityResult.StartKyc -> intent.putExtra(RESULT_START_KYC, true)
            ActivityResult.StartReceive -> intent.putExtra(RESULT_START_RECEIVE, true)
            ActivityResult.StartBuyIntro -> intent.putExtra(RESULT_START_BUY_INTRO, true)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        private const val ACTION = "action"
        private const val RESULT_START_KYC = "RESULT_START_KYC"
        private const val RESULT_START_RECEIVE = "RESULT_START_RECEIVE"
        private const val RESULT_START_BUY_INTRO = "RESULT_START_BUY_INTRO"

        private fun newIntent(context: Context, action: AssetAction): Intent =
            Intent(context, ActionActivity::class.java).apply {
                putExtra(ACTION, action)
            }
    }

    data class ActivityArgs(val action: AssetAction)
    sealed class ActivityResult {
        object StartKyc : ActivityResult()
        object StartReceive : ActivityResult()
        object StartBuyIntro : ActivityResult()
    }
    class BlockchainActivityResultContract : ActivityResultContract<ActivityArgs, ActivityResult?>() {
        override fun createIntent(context: Context, input: ActivityArgs): Intent =
            newIntent(context, input.action)

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult? {
            val startKyc = intent?.getBooleanExtra(RESULT_START_KYC, false) ?: false
            val startReceive = intent?.getBooleanExtra(RESULT_START_RECEIVE, false) ?: false
            val startBuyIntro = intent?.getBooleanExtra(RESULT_START_BUY_INTRO, false) ?: false

            return when {
                resultCode != Activity.RESULT_OK -> null
                startKyc -> ActivityResult.StartKyc
                startReceive -> ActivityResult.StartReceive
                startBuyIntro -> ActivityResult.StartBuyIntro
                else -> null
            }
        }
    }
}
