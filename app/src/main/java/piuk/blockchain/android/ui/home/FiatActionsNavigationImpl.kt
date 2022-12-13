package piuk.blockchain.android.ui.home

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavigation
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet

class FiatActionsNavigationImpl(private val activity: BlockchainActivity?) : FiatActionsNavigation {
    override fun wireTransferDetail(account: FiatAccount) {
        activity?.showBottomSheet(
            WireTransferAccountDetailsBottomSheet.newInstance(account)
        )
    }
}
