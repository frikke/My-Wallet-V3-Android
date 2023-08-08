package piuk.blockchain.android.ui.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.swapdexoption.SwapDexOptionScreen

class SwapSelectorSheet private constructor() : ThemedBottomSheetFragment() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onSwapSelectorOpenSwap(account: CryptoAccount)
        fun onSwapSelectorOpenDex()
    }

    private val host: Host by lazy {
        activity as? Host ?: throw IllegalStateException("Host fragment is not a RecurringBuyDetailsSheet.Host")
    }

    private val account: CryptoAccount by lazy {
        (arguments?.getSerializable(ACCOUNT) as? CryptoAccount) ?: error("missing account arg")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Surface(
                    shape = AppTheme.shapes.veryLarge
                ) {
                    SwapDexOptionScreen(
                        onBackPressed = this@SwapSelectorSheet::dismiss,
                        openSwap = { host.onSwapSelectorOpenSwap(account) },
                        openDex = host::onSwapSelectorOpenDex
                    )
                }
            }
        }
    }

    companion object {
        const val ACCOUNT = "account"
        fun newInstance(account: CryptoAccount): SwapSelectorSheet = SwapSelectorSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ACCOUNT, account)
            }
        }
    }
}
