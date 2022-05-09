package piuk.blockchain.android.data.coinswebsocket.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.blockchain.analytics.Analytics
import com.blockchain.koin.scopedInject
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.websocket.MessagesSocketHandler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.then

class CoinsWebSocketService(
    private val applicationContext: Context
) : MessagesSocketHandler, KoinComponent {

    private val compositeDisposable = CompositeDisposable()
    private val notificationManager: NotificationManager by inject()
    private val coinsWebSocketStrategy: CoinsWebSocketStrategy by scopedInject()
    private val lifecycleInterestedComponent: LifecycleInterestedComponent by inject()
    private val rxBus: RxBus by inject()
    private val analytics: Analytics by inject()

    fun start() {
        compositeDisposable.clear()
        compositeDisposable += coinsWebSocketStrategy.close().then {
            coinsWebSocketStrategy.setMessagesHandler(this)
            coinsWebSocketStrategy.open()
        }.onErrorComplete().emptySubscribe()

        compositeDisposable += lifecycleInterestedComponent
            .appStateUpdated
            .flatMapCompletable {
                if (it == AppState.FOREGROUNDED) {
                    coinsWebSocketStrategy.open()
                } else {
                    coinsWebSocketStrategy.close()
                }
            }.onErrorComplete().emptySubscribe()
    }

    override fun triggerNotification(title: String, marquee: String, text: String) {
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            MainActivity.newIntent(applicationContext, intentFromNotification = true, null),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        NotificationsUtil(
            context = applicationContext,
            notificationManager = notificationManager,
            analytics = analytics
        ).triggerNotification(
            title = title,
            marquee = marquee,
            text = text,
            pendingIntent = pendingIntent,
            id = 1000,
            appName = R.string.app_name,
            colorRes = R.color.primary_navy_medium
        )
    }

    override fun sendBroadcast(event: ActionEvent) {
        rxBus.emitEvent(ActionEvent::class.java, event)
    }

    fun release() {
        compositeDisposable += coinsWebSocketStrategy.close().onErrorComplete().doOnTerminate {
            compositeDisposable.clear()
        }.emptySubscribe()
    }
}
