package piuk.blockchain.android.ui.educational.walletmodes

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setCanceledOnTouchOutside(false)
            forceExpanded()
        }
    }

    companion object {
        fun newInstance() = EducationalWalletModeNavigationSheet()
    }
}
