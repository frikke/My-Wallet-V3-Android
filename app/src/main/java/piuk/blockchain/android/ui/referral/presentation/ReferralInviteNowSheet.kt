package piuk.blockchain.android.ui.referral.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment

class ReferralInviteNowSheet : ThemedBottomSheetFragment(
    cancelableOnTouchOutside = false
) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                ReferralInviteNowScreen(
                    "Invite Friends, get $30",
                    "Increase your earnings on each successful invite",
                    { dismiss() },
                    { dismiss() }
                )
            }
        }
    }
}
