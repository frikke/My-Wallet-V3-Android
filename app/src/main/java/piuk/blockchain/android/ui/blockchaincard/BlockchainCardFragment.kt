package piuk.blockchain.android.ui.blockchaincard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.ui.BlockchainCardHostFragment
import com.blockchain.blockchaincard.ui.composables.BlockchainCardNavHost
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.commonarch.presentation.base.updateToolbar
import piuk.blockchain.android.R

class BlockchainCardFragment : BlockchainCardHostFragment() {

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(R.string.blockchain_card),
            menuItems = emptyList(),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val viewModel =
                if (modelArgs is BlockchainCardArgs.CardArgs) manageCardViewModel
                else orderCardViewModel

            setContent {
                viewModel.viewCreated(modelArgs)
                BlockchainCardNavHost(viewModel = viewModel, modelArgs = modelArgs)
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
