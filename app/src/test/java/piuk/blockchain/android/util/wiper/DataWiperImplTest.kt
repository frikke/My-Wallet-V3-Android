package piuk.blockchain.android.util.wiper

import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.payload.PayloadScopeWiper
import org.amshove.kluent.internal.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState

class DataWiperImplTest {

    private val ethDataManager: EthDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val walletOptionsState: WalletOptionsState = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val walletConnectServiceAPI: WalletConnectServiceAPI = mock()
    private val assetActivityRepository: AssetActivityRepository = mock()
    private val walletPrefs: WalletStatusPrefs = mock()
    private val payloadScopeWiper: PayloadScopeWiper = mock()
    private val remoteLogger: RemoteLogger = mock()

    private lateinit var subject: DataWiper

    @Before
    fun setUp() {
        subject = DataWiperImpl(
            ethDataManager,
            bchDataManager,
            walletOptionsState,
            nabuDataManager,
            walletConnectServiceAPI,
            assetActivityRepository,
            walletPrefs,
            payloadScopeWiper,
            remoteLogger
        )
    }

    @Test
    fun `clearData() should wipe all data and close scope`() {
        // Act
        subject.clearData()

        // Assert
        verify(remoteLogger).logEvent(anyString())
        verify(ethDataManager).clearAccountDetails()
        verify(bchDataManager).clearAccountDetails()
        verify(assetActivityRepository).clear()
        verify(nabuDataManager).clearAccessToken()
        verify(walletConnectServiceAPI).clear()
        verify(walletOptionsState).wipe()
        verify(payloadScopeWiper).wipe()
        assertFalse(walletPrefs.isAppUnlocked)
    }
}
