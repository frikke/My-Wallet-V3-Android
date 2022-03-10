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
    MviComposeFragment<OrderCardModel, OrderCardIntent, OrderCardState>(), FlowFragment {

    override val model: OrderCardModel by scopedInject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // model.process(DappsListIntent.LoadDapps)
    }

    companion object {
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
                BlockchainCardScreen()
            }
        }
    }

    override fun onBackPressed(): Boolean = false

}