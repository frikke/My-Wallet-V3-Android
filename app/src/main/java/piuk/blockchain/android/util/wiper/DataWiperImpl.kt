package piuk.blockchain.android.util.wiper

import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import info.blockchain.wallet.payload.PayloadScopeWiper
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import timber.log.Timber

class DataWiperImpl constructor(
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val walletOptionsState: WalletOptionsState,
    private val nabuDataManager: NabuDataManager,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val assetActivityRepository: AssetActivityRepository,
    private val walletPrefs: WalletStatus,
    private val payloadScopeWiper: PayloadScopeWiper
) : DataWiper {

    override fun clearData() {
        ethDataManager.clearAccountDetails()
        bchDataManager.clearAccountDetails()
        assetActivityRepository.clear()
        nabuDataManager.clearAccessToken()
        walletConnectServiceAPI.clear()
        walletOptionsState.wipe()
        payloadScopeWiper.wipe()
        walletPrefs.isAppUnlocked = false
        Timber.d("skipPinAndProcessDeeplink: isAppUnlocked = false")
    }
}
