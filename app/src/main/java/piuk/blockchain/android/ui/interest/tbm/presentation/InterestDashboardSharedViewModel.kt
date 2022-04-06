package piuk.blockchain.android.ui.interest.tbm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class InterestDashboardSharedViewModel : ViewModel() {
    private val _refreshBalancesFlow = MutableSharedFlow<Unit>(replay = 0)
    val refreshBalancesFlow: SharedFlow<Unit> = _refreshBalancesFlow

    fun requestBalanceRefresh() {
        viewModelScope.launch {
            _refreshBalancesFlow.emit(Unit)
        }
    }
}