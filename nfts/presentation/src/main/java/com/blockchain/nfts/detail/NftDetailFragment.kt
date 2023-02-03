package com.blockchain.nfts.detail

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.detail.navigation.NftDetailNavigationEvent
import com.blockchain.nfts.detail.screen.NftDetail
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.compose.getViewModel

class NftDetailFragment :
    MVIBottomSheet<NftDetailViewState>(),
    NavigationRouter<NftDetailNavigationEvent> {

    interface Host : MVIBottomSheet.Host {
        fun openExternalUrl(url: String)
    }

    override val host: Host by lazy {
        activity as? Host ?: error("Host activity is not a NftDetailFragment.Host")
    }

    val args: NftDetailNavArgs by lazy {
        arguments?.getParcelable<NftDetailNavArgs>(NftDetailNavArgs.ARGS_KEY)
            ?: error("missing NftDetailNavArg")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            forceExpanded()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val viewModel: NftDetailViewModel = getViewModel(scope = payloadScope)

//                bindViewModel(viewModel = viewModel, navigator = this@NftDetailFragment, args = args)

                Surface(
                    modifier = Modifier
                        .nestedScroll(rememberNestedScrollInteropConnection()),
                    color = AppTheme.colors.background,
                    shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing)
                ) {
//                    NftDetail(viewModel)
                }
            }
        }
    }

    override fun onStateUpdated(state: NftDetailViewState) {
    }

    override fun route(navigationEvent: NftDetailNavigationEvent) {
        when (navigationEvent) {
            is NftDetailNavigationEvent.ExternalView -> {
                host.openExternalUrl(navigationEvent.url)
            }
        }
    }

    companion object {
        fun newInstance(
            nftId: String,
            pageKey: String?,
            address: String
        ): NftDetailFragment {
            val bundle = Bundle()
            bundle.putParcelable(
                NftDetailNavArgs.ARGS_KEY,
                NftDetailNavArgs(nftId = nftId, pageKey = pageKey, address = address)
            )
            return NftDetailFragment().apply {
                arguments = bundle
            }
        }
    }
}
