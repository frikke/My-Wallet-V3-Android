package com.blockchain.blockchaincard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.compose.rememberNavController
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.ui.composables.BlockchainCardNavHost
import com.blockchain.blockchaincard.ui.composables.BlockchainCardScreen
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.getViewModel

class BlockchainCardFragment : MVIFragment<BlockchainCardViewState>(), FlowFragment {

    private val viewModel: BlockchainCardViewModel by lazy {
        payloadScope.getViewModel(owner = { ViewModelOwner.from(this) })
    }

    private val navigator: BlockchainCardNavigationRouter by scopedInject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        bindViewModel(viewModel, navigator, ModelConfigArgs.NoArgs) // TODO pass card info here

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val navController = rememberNavController()
                BlockchainCardNavHost(navController = navController, viewModel = viewModel)
            }
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun onStateUpdated(state: BlockchainCardViewState) {
        TODO("Not yet implemented")
    }
}