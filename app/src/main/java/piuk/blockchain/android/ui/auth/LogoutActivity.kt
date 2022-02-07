package piuk.blockchain.android.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.commonarch.presentation.base.BlockchainActivity.Companion.LOGOUT_ACTION
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.preferences.WalletStatus
import java.io.Serializable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState

class LogoutActivity : AppCompatActivity() {

    private val ethDataManager: EthDataManager by scopedInject()
    private val bchDataManager: BchDataManager by scopedInject()
    private val walletOptionsState: WalletOptionsState by scopedInject()
    private val nabuDataManager: NabuDataManager by scopedInject()
    private val assetActivityRepository: AssetActivityRepository by scopedInject()
    private val osUtil: OSUtil by inject()
    private val analytics: Analytics by inject()
    private val walletPrefs: WalletStatus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == LOGOUT_ACTION) {
            val intent = Intent(this, CoinsWebSocketService::class.java)

            if (osUtil.isServiceRunning(CoinsWebSocketService::class.java)) {
                stopService(intent)
            }

            // TODO: 30/06/20 We shouldn't need this any more now we have koin scopes
            // TODO: see Jira AND-3312
            clearData()
            analytics.logEvent(LogOutAnalyticsEvent)
        }
    }

    private fun clearData() {
        ethDataManager.clearAccountDetails()
        bchDataManager.clearAccountDetails()
        assetActivityRepository.clear()
        nabuDataManager.clearAccessToken()

        walletOptionsState.wipe()

        walletPrefs.isAppUnlocked = false
        finishAffinity()
    }
}

object LogOutAnalyticsEvent : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SIGNED_OUT.eventName
    override val params: Map<String, Serializable>
        get() = mapOf()
}
