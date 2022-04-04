package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import io.reactivex.rxjava3.core.Single

interface InterestDashboardHost {
    fun startKyc()
    fun showInterestSummarySheet(account: CryptoAccount)
    fun startAccountSelection(filter: Single<List<BlockchainAccount>>, toAccount: SingleAccount)
}