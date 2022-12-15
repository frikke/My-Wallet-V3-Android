package piuk.blockchain.android.ui.referral.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.home.presentation.referral.composable.ReferralCode
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReferralSheet : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        disableDragging()

        return ComposeView(requireContext()).apply {
            setContent {
                ReferralCode(
                    onBackPressed = ::dismiss
                )
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
        fun newInstance() = ReferralSheet()
    }
}
