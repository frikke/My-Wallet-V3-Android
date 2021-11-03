package piuk.blockchain.android.util.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.logging.CrashLogger

class AppLifecycleListener(
    private val lifecycleInterestedComponent: LifecycleInterestedComponent,
    private val crashLogger: CrashLogger
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        crashLogger.logEvent("App to foreground")
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.FOREGROUNDED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        crashLogger.logEvent("App to background")
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.BACKGROUNDED)
    }
}
