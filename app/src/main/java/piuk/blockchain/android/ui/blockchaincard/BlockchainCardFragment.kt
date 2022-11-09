package piuk.blockchain.android.ui.blockchaincard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.ui.BlockchainCardHostFragment
import com.blockchain.blockchaincard.ui.composables.BlockchainCardNavHost
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.componentlib.theme.AppTheme
import org.koin.androidx.compose.get
import piuk.blockchain.android.R

class BlockchainCardFragment : BlockchainCardHostFragment() {

    val viewModel: BlockchainCardViewModel by lazy {
        if (modelArgs is BlockchainCardArgs.CardArgs) manageCardViewModel
        else orderCardViewModel
    }

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(R.string.blockchain_card),
            menuItems = emptyList(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.viewCreated(modelArgs)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val lifecycleOwner = LocalLifecycleOwner.current

                    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
                        viewModel.viewState.flowWithLifecycle(
                            lifecycle = lifecycleOwner.lifecycle, minActiveState = Lifecycle.State.STARTED
                        )
                    }

                    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
                        viewModel.navigationEventFlow.flowWithLifecycle(
                            lifecycle = lifecycleOwner.lifecycle, minActiveState = Lifecycle.State.STARTED
                        )
                    }

                    BlockchainCardNavHost(
                        viewModel = viewModel,
                        modelArgs = modelArgs,
                        stateFlowLifecycleAware = stateFlowLifecycleAware,
                        navigationRouter = BlockchainCardNavigationRouter(rememberNavController()),
                        navEventsFlowLifecycleAware = navEventsFlowLifecycleAware
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(
            blockchainCards: List<BlockchainCard>,
            preselectedCard: BlockchainCard?,
            blockchainCardProducts: List<BlockchainCardProduct>
        ):
            BlockchainCardHostFragment =
            BlockchainCardFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(BLOCKCHAIN_CARD_LIST, blockchainCards.toTypedArray())
                    putParcelableArray(BLOCKCHAIN_CARD_PRODUCT_LIST, blockchainCardProducts.toTypedArray())
                    preselectedCard?.let { putParcelable(PRESELECTED_BLOCKCHAIN_CARD, preselectedCard) }
                }
            }

        fun newInstance(blockchainCardProducts: List<BlockchainCardProduct>):
            BlockchainCardHostFragment =
            BlockchainCardFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(BLOCKCHAIN_CARD_PRODUCT_LIST, blockchainCardProducts.toTypedArray())
                }
            }
    }
}
