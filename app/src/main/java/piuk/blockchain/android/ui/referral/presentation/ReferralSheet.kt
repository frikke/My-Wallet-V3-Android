package piuk.blockchain.android.ui.referral.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.blockchain.home.presentation.referral.composable.ReferralCode

class ReferralSheet : ThemedBottomSheetFragment(
    cancelableOnTouchOutside = false
) {
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

    companion object {
        fun newInstance() = ReferralSheet()
    }
}
