package piuk.blockchain.android.ui.home.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

class ActionActivity : BlockchainActivity(), SlidingModalBottomDialog.Host, UpsellHost {

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val binding: ActivityActionBinding by lazy {
        ActivityActionBinding.inflate(layoutInflater)
    }

    private val action: AssetAction by lazy {
        intent.getSerializableExtra(ACTION) as AssetAction
    }

    override val toolbarBinding: ToolbarGeneralBinding?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()

        supportFragmentManager.showFragment(
            fragment = loadFragment(),
            loadingView = binding.progress
        )
    }

    private fun setupToolbar() {
        binding.toolbar.onBackButtonClick = { onBackPressed() }
    }

    private fun updateToolbarTitle(title: String) {
        binding.toolbar.title = title
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
        setResult(RESULT_OK)
        finish()
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        private const val ACTION = "action"
        fun newIntent(context: Context, action: AssetAction): Intent =
            Intent(context, ActionActivity::class.java).apply {
                putExtra(ACTION, action)
            }
    }
}
