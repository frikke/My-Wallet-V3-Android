package com.blockchain.nfts.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.detail.screen.NftDetail
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.compose.getViewModel

class NftDetailFragment : BottomSheetDialogFragment() {

    val args: NftDetailNavArgs by lazy {
        arguments?.getParcelable<NftDetailNavArgs>(NftDetailNavArgs.ARGS_KEY)
            ?: error("missing NftDetailNavArg")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val viewModel: NftDetailViewModel = getViewModel(scope = payloadScope)
                viewModel.viewCreated(args)

                NftDetail(viewModel)
            }
        }
    }

    companion object {
        fun newInstance(nftId: String): NftDetailFragment {
            val bundle = Bundle()
            bundle.putParcelable(NftDetailNavArgs.ARGS_KEY, NftDetailNavArgs(nftId = nftId))
            return NftDetailFragment().apply {
                arguments = bundle
            }
        }
    }
}
