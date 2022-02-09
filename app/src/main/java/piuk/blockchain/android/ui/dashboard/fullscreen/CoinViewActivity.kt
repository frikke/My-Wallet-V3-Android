package piuk.blockchain.android.ui.dashboard.fullscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.databinding.ActivityCoinviewBinding

class CoinViewActivity : BlockchainActivity() {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val binding: ActivityCoinviewBinding by lazy {
        ActivityCoinviewBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar("Coin view")
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, CoinViewActivity::class.java)
    }
}
