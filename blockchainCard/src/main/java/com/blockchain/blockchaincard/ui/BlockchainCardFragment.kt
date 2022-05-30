package com.blockchain.blockchaincard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.ui.composables.BlockchainCardNavHost
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.koin.payloadScope
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

class BlockchainCardFragment : Fragment(), FlowFragment, AndroidScopeComponent {

    override val scope: Scope = payloadScope

    private val orderCardViewModel: OrderCardViewModel by viewModel()

    private val manageCardViewModel: ManageCardViewModel by viewModel()

    private val modelArgs: ModelConfigArgs by lazy {
        (arguments?.getParcelable(BLOCKCHAIN_CARD) as? BlockchainCard)?.let { card ->
            BlockchainCardArgs.CardArgs(card)
        } ?: (arguments?.getParcelable(BLOCKCHAIN_PRODUCT) as? BlockchainCardProduct)?.let { product ->
            BlockchainCardArgs.ProductArgs(product)
        } ?: throw IllegalStateException("Missing card or product data")
    }

    companion object {
        private const val BLOCKCHAIN_CARD = "BLOCKCHAIN_CARD"
        private const val BLOCKCHAIN_PRODUCT = "BLOCKCHAIN_PRODUCT"

        fun newInstance(blockchainCard: BlockchainCard) =
            BlockchainCardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(BLOCKCHAIN_CARD, blockchainCard)
                }
            }

        fun newInstance(blockchainCardProduct: BlockchainCardProduct) =
            BlockchainCardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(BLOCKCHAIN_PRODUCT, blockchainCardProduct)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(R.string.blockchain_card),
            menuItems = emptyList()
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
                BlockchainCardNavHost(viewModel = viewModel, modelArgs = modelArgs)
            }
        }
    }

    override fun onBackPressed(): Boolean = false
}
