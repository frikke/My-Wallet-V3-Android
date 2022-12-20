package com.blockchain.nfts.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.collection.navigation.NftCollectionNavigationEvent
import com.blockchain.nfts.collection.screen.NftCollection
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

class NftCollectionFragment :
    MVIFragment<NftCollectionViewState>(),
    KoinScopeComponent,
    NavigationRouter<NftCollectionNavigationEvent> {

    interface Host {
        fun showReceiveSheet(account: BlockchainAccount)
        fun showNftDetail(nftId: String, pageKey: String?, address: String)
        fun showNftHelp()
        fun openExternalUrl(url: String)
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel by viewModel<NftCollectionViewModel>()

    private val host: Host by lazy {
        activity as? Host ?: error("Host activity is not a NftCollectionFragment.Host")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                bindViewModel(
                    viewModel = viewModel, navigator = this@NftCollectionFragment, args = ModelConfigArgs.NoArgs
                )

                NftCollection(viewModel)
            }
        }
    }

    override fun onStateUpdated(state: NftCollectionViewState) {
    }

    override fun route(navigationEvent: NftCollectionNavigationEvent) {
        when (navigationEvent) {
            is NftCollectionNavigationEvent.ShopExternal -> {
                host.openExternalUrl(navigationEvent.url)
            }

            is NftCollectionNavigationEvent.ShowReceiveAddress -> {
                host.showReceiveSheet(navigationEvent.account)
            }

            NftCollectionNavigationEvent.ShowHelp -> {
                host.showNftHelp()
            }

            is NftCollectionNavigationEvent.ShowDetail -> {
                host.showNftDetail(
                    nftId = navigationEvent.nftId,
                    pageKey = navigationEvent.pageKey,
                    address = navigationEvent.address
                )
            }
        }
    }
}
