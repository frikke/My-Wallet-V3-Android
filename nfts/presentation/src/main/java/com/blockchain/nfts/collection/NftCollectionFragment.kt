package com.blockchain.nfts.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.collection.screen.NftCollection
import com.blockchain.nfts.detail.NftDetailFragment
import org.koin.androidx.compose.getViewModel

class NftCollectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val viewModel: NftCollectionViewModel = getViewModel(scope = payloadScope)
                viewModel.viewCreated(ModelConfigArgs.NoArgs)

                NftCollection(viewModel, {(requireActivity() as BlockchainActivity).showBottomSheet(NftDetailFragment.newInstance(it.id))})
            }
        }
    }
}
