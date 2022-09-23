package com.blockchain.nfts.detail

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.koin.payloadScope
import com.blockchain.nfts.R
import com.blockchain.nfts.detail.screen.NftDetail
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.compose.getViewModel

class NftDetailFragment : BottomSheetDialogFragment() {

    val args: NftDetailNavArgs by lazy {
        arguments?.getParcelable<NftDetailNavArgs>(NftDetailNavArgs.ARGS_KEY)
            ?: error("missing NftDetailNavArg")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            forceExpanded()
        }
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
