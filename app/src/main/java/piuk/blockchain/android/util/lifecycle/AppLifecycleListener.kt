package piuk.blockchain.android.util.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.logging.RemoteLogger

class AppLifecycleListener(
    private val lifecycleInterestedComponent: LifecycleInterestedComponent,
    private val remoteLogger: RemoteLogger
) : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                remoteLogger.logEvent("App to foreground")
                lifecycleInterestedComponent.appStateUpdated.onNext(AppState.FOREGROUNDED)
            }

            Lifecycle.Event.ON_STOP -> {
                remoteLogger.logEvent("App to background")
                lifecycleInterestedComponent.appStateUpdated.onNext(AppState.BACKGROUNDED)
            }

            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_RESUME,
            Lifecycle.Event.ON_PAUSE -> {
            }

            Lifecycle.Event.ON_DESTROY,
            Lifecycle.Event.ON_ANY -> {
            }
        }
    }
}
