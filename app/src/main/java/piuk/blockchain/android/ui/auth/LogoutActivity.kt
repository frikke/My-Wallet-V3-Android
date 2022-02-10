package piuk.blockchain.android.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.commonarch.presentation.base.BlockchainActivity.Companion.LOGOUT_ACTION
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.wiper.DataWiper

class LogoutActivity : AppCompatActivity() {

    private val osUtil: OSUtil by inject()
    private val analytics: Analytics by inject()
    private val dataWiper: DataWiper by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == LOGOUT_ACTION) {
            val intent = Intent(this, CoinsWebSocketService::class.java)

            if (osUtil.isServiceRunning(CoinsWebSocketService::class.java)) {
                stopService(intent)
            }
            clearData()
            analytics.logEvent(LogOutAnalyticsEvent)
        }
    }

    private fun clearData() {
        dataWiper.clearData()
        finishAffinity()
    }
}

object LogOutAnalyticsEvent : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SIGNED_OUT.eventName
    override val params: Map<String, Serializable>
        get() = mapOf()
}
