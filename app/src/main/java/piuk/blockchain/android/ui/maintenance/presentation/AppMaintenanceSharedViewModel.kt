package piuk.blockchain.android.ui.maintenance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.launcher.LauncherActivity

class AppMaintenanceSharedViewModel : ViewModel() {
    private val _resumeAppFlow = MutableSharedFlow<Unit>(replay = 0)

    /**
     * The screen responsible for showing the app maintenance screens
     * Should be responsible for observing when the flow should proceed.
     *
     * @see [LauncherActivity.observeResumeAppFlow]
     */
    val resumeAppFlow: SharedFlow<Unit> = _resumeAppFlow

    fun resumeAppFlow() {
        viewModelScope.launch {
            _resumeAppFlow.emit(Unit)
        }
    }
}