package piuk.blockchain.android.ui.educational.walletmodes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import piuk.blockchain.android.ui.educational.walletmodes.screens.EducationalWalletModeNavigationScreen

class EducationalWalletModeNavigationSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = false

        disableDragging()

        return ComposeView(requireContext()).apply {
            setContent {
                EducationalWalletModeNavigationScreen {}
            }
        }
    }

    companion object {
        fun newInstance() = EducationalWalletModeNavigationSheet()
    }
}
