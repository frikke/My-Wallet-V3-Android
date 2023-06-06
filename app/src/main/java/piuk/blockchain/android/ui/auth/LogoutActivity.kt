package piuk.blockchain.android.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.commonarch.presentation.base.BlockchainActivity.Companion.LOGOUT_ACTION
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.presentation.koin.scopedInject
import java.io.Serializable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.util.wiper.DataWiper

class LogoutActivity : AppCompatActivity() {

    private val analytics: Analytics by inject()
    private val notificationTransmitter: NotificationTransmitter by inject()
    private val dataWiper: DataWiper by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == LOGOUT_ACTION) {
            clearData()
            analytics.logEvent(LogOutAnalyticsEvent)
        }
    }

    private fun clearData() {
        dataWiper.clearData()
        finishAffinity()
        notificationTransmitter.postEvent(NotificationEvent.Logout)
    }
}

object LogOutAnalyticsEvent : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SIGNED_OUT.eventName
    override val params: Map<String, Serializable>
        get() = mapOf()
}
