package piuk.blockchain.android.internalnotifications

import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationReceiver
import com.blockchain.internalnotifications.NotificationTransmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

internal class NotificationsCenter(private val scope: CoroutineScope) : NotificationReceiver, NotificationTransmitter {
    private val _events = MutableSharedFlow<NotificationEvent>()

    override val events: Flow<NotificationEvent>
        get() = _events.asSharedFlow().shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 0
        )

    override fun postEvent(event: NotificationEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    override fun postEvents(events: List<NotificationEvent>) {
        scope.launch {
            events.forEach {
                _events.emit(it)
            }
        }
    }
}
