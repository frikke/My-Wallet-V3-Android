package piuk.blockchain.android.ui.interest.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
}
