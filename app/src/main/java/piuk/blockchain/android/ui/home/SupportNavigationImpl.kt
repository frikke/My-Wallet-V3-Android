package piuk.blockchain.android.ui.home

import com.blockchain.chrome.navigation.SupportNavigation
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import piuk.blockchain.android.support.SupportCentreActivity

class SupportNavigationImpl(private val activity: BlockchainActivity?) : SupportNavigation {

    override fun launchSupportCenter() {
        activity!!.startActivity(SupportCentreActivity.newIntent(activity))
    }

    override fun launchSupportChat() {
        activity!!.startActivity(SupportCentreActivity.newIntent(context = activity, launchChat = true))
    }
}
