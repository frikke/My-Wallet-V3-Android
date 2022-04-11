package piuk.blockchain.android.ui.maintenance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class AppMaintenanceSharedViewModel : ViewModel() {
    private val _resumeAppFlow = MutableSharedFlow<Unit>(replay = 0)
    val resumeAppFlow: SharedFlow<Unit> = _resumeAppFlow

    fun resumeAppFlow() {
        viewModelScope.launch {
            _resumeAppFlow.emit(Unit)
        }
    }
}

