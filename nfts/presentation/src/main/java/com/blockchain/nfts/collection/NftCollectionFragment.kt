package com.blockchain.nfts.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.collection.screen.NftCollection
import com.blockchain.nfts.detail.NftDetailFragment
import com.blockchain.presentation.openUrl
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class NftCollectionFragment :
    MVIFragment<NftCollectionViewState>(),
    KoinScopeComponent,
    NavigationRouter<NftCollectionNavigationEvent> {

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<NftCollectionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //        showBottomSheet(ReceiveDetailSheet.newInstance(navigationEvent.cvAccount.account as CryptoAccount))

        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(
                    viewModel = viewModel, navigator = this@NftCollectionFragment, args = ModelConfigArgs.NoArgs
                )

                NftCollection(
                    viewModel,
                    { (requireActivity() as BlockchainActivity).showBottomSheet(NftDetailFragment.newInstance(it.id)) })
            }
        }
    }

    override fun onStateUpdated(state: NftCollectionViewState) {
    }

    override fun route(navigationEvent: NftCollectionNavigationEvent) {
        when (navigationEvent) {
            is NftCollectionNavigationEvent.ShopExternal -> {
                requireContext().openUrl(navigationEvent.url)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(NftCollectionIntent.LoadData)
    }
}
