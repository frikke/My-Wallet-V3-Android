package piuk.blockchain.android.ui.home

import androidx.core.content.ContextCompat.startActivity
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.nfts.navigation.NftNavigation
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailActivity

class NftNavigationImpl(private val activity: BlockchainActivity?) : NftNavigation {

    override fun showReceiveSheet(account: CryptoAccount) {
        activity?.let {
            activity.startActivity(ReceiveDetailActivity.newIntent(activity, account))
        }
    }
}
