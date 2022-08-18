package piuk.blockchain.android.ui.coinview.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.koin.payloadScope
import info.blockchain.balance.AssetInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.ui.coinview.presentation.composable.Coinview

class CoinviewActivity : BlockchainActivity(),
    KoinScopeComponent {

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val scope: Scope = payloadScope
    private val viewModel: CoinviewViewModel by viewModel()

    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
    val args: CoinviewArgs by lazy {
        intent.getParcelableExtra<CoinviewArgs>(CoinviewArgs.ARGS_KEY)
            ?: error("missing CoinviewArgs")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.viewCreated(args = args)

        setContent {
            Coinview(
                viewModel = viewModel,
                backOnClick = { onBackPressedDispatcher.onBackPressed() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(CoinviewIntents.LoadData)
    }

    companion object {
        fun newIntent(context: Context, asset: AssetInfo): Intent {
            return Intent(context, CoinviewActivity::class.java).apply {
                putExtra(
                    CoinviewArgs.ARGS_KEY,
                    CoinviewArgs(networkTicker = asset.networkTicker)
                )
            }
        }
    }
}
