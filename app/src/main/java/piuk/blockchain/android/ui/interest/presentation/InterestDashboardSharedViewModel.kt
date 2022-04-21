package piuk.blockchain.android.ui.interest.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.CryptoAccount
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class InterestDashboardSharedViewModel : ViewModel() {
    private val _refreshBalancesFlow = MutableSharedFlow<Unit>(replay = 0)
    val refreshBalancesFlow: SharedFlow<Unit> = _refreshBalancesFlow
    internal fun requestBalanceRefresh() {
        viewModelScope.launch {
            _refreshBalancesFlow.emit(Unit)
        }
    }

    private val _startKycFlow = MutableSharedFlow<Unit>(replay = 0)
    val startKycFlow: SharedFlow<Unit> = _startKycFlow
    internal fun startKyc() {
        viewModelScope.launch {
            _startKycFlow.emit(Unit)
        }
    }

    private val _showInterestSummaryFlow = MutableSharedFlow<CryptoAccount>(replay = 0)
    val showInterestSummaryFlow: SharedFlow<CryptoAccount> = _showInterestSummaryFlow
    internal fun showInterestSummary(account: CryptoAccount) {
        viewModelScope.launch {
            _showInterestSummaryFlow.emit(account)
        }
    }
}
