package piuk.blockchain.android.ui.interest.tbm.presentation

import com.blockchain.coincore.CryptoAccount

interface InterestDashboardHost {
    fun startKyc()
    fun showInterestSummarySheet(account: CryptoAccount)
}