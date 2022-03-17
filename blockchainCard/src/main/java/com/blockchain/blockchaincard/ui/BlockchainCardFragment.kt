package com.blockchain.blockchaincard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.ui.composables.BlockchainCardScreen
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi.MviComposeFragment
import com.blockchain.koin.scopedInject

class BlockchainCardFragment :
    MviComposeFragment<BlockchainCardModel, BlockchainCardIntent, BlockchainCardState>(), FlowFragment {

    override val model: BlockchainCardModel by scopedInject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(BlockchainCardIntent.UpdateCardState(CardState.NOT_ORDERED))
    }

    companion object {
        private const val BLOCKCHAIN_CARD = "BLOCKCHAIN_CARD"
        fun newInstance() = BlockchainCardFragment()
    }

    override fun onResume() {
        super.onResume()
        updateToolbar(
            toolbarTitle = getString(R.string.order_card_title),
            menuItems = emptyList()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        analytics.logEvent(
//            WalletConnectAnalytics.ConnectedDappsListViewed
//        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BlockchainCardScreen(model)
            }
        }
    }

    override fun onBackPressed(): Boolean = false

}