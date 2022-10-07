package com.blockchain.nfts.help

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
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.nfts.OPENSEA_URL
import com.blockchain.nfts.help.screen.NftHelpScreen
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.java.KoinJavaComponent

class NftHelpFragment :
    BottomSheetDialogFragment(),
    Analytics by KoinJavaComponent.get(Analytics::class.java) {

    interface Host {
        fun openExternalUrl(url: String)
    }

    private val host: Host by lazy {
        activity as? Host ?: error("Host activity is not a NftHelpFragment.Host")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setCanceledOnTouchOutside(false)
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
                Surface(
                    modifier = Modifier
                        .nestedScroll(rememberNestedScrollInteropConnection()),
                    color = AppTheme.colors.background,
                    shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing)
                ) {
                    NftHelpScreen(
                        onBuyClick = {
                            host.openExternalUrl(OPENSEA_URL)
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(): NftHelpFragment {
            return NftHelpFragment()
        }
    }
}
