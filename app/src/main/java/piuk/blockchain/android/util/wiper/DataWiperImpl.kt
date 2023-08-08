package piuk.blockchain.android.util.wiper

import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.api.services.ActivityWebSocketService
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import info.blockchain.wallet.payload.PayloadScopeWiper
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.android.ui.launcher.GlobalEventHandler

class DataWiperImpl constructor(
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val nabuDataManager: NabuDataManager,
    private val activityWebSocketService: ActivityWebSocketService,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val assetActivityRepository: AssetActivityRepository,
    private val walletPrefs: WalletStatusPrefs,
    private val payloadScopeWiper: PayloadScopeWiper,
    private val sessionInfo: SessionInfo,
    private val remoteLogger: RemoteLogger,
    private val globalEventHandler: GlobalEventHandler
) : DataWiper {
    /***
     * App locked
     */
    override fun clearData() {
        remoteLogger.logEvent("Clearing data")
        activityWebSocketService.close()
        ethDataManager.clearAccountDetails()
        bchDataManager.clearAccountDetails()
        assetActivityRepository.clear()
        nabuDataManager.clearAccessToken()
        walletConnectServiceAPI.clear()
        payloadScopeWiper.wipe()
        sessionInfo.clearUserId()
        walletPrefs.isAppUnlocked = false
        globalEventHandler.clear()
    }
}
