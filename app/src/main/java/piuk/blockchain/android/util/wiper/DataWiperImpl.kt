package piuk.blockchain.android.util.wiper

import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import info.blockchain.wallet.payload.PayloadScopeWiper
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState

class DataWiperImpl constructor(
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val walletOptionsState: WalletOptionsState,
    private val nabuDataManager: NabuDataManager,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val assetActivityRepository: AssetActivityRepository,
    private val walletPrefs: WalletStatusPrefs,
    private val payloadScopeWiper: PayloadScopeWiper,
    private val remoteLogger: RemoteLogger
) : DataWiper {

    override fun clearData() {
        remoteLogger.logEvent("Clearing data")

        ethDataManager.clearAccountDetails()
        bchDataManager.clearAccountDetails()
        assetActivityRepository.clear()
        nabuDataManager.clearAccessToken()
        walletConnectServiceAPI.clear()
        walletOptionsState.wipe()
        payloadScopeWiper.wipe()
        walletPrefs.isAppUnlocked = false
    }
}
