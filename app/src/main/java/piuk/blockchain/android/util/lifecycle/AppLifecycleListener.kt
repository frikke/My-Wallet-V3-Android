package piuk.blockchain.android.util.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.logging.RemoteLogger
import piuk.blockchain.android.fraud.domain.service.FraudService

class AppLifecycleListener(
    private val lifecycleInterestedComponent: LifecycleInterestedComponent,
    private val remoteLogger: RemoteLogger,
    private val fraudService: FraudService
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        remoteLogger.logEvent("App to foreground")
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.FOREGROUNDED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        remoteLogger.logEvent("App to background")
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.BACKGROUNDED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        fraudService.endFlow()
    }
}
