package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.settings.v2.security.pin.PinInteractor
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

// TODO coming
class PinInteractorTest {
    private lateinit var interactor: PinInteractor
    private val mobileNoticeRemoteConfig = mock<MobileNoticeRemoteConfig>()
    private val authDataManager = mock<AuthDataManager>()
    private val apiStatus = mock<ApiStatus>()
    private val authPrefs = mock<AuthPrefs>()
    private val persistentPrefs = mock<PersistentPrefs>()
    private val walletStatus = mock<WalletStatus>()
    private val walletOptionsDataManager = mock<WalletOptionsDataManager>()
    private val credentialsWiper = mock<CredentialsWiper>()
    private val payloadDataManager = mock<PayloadDataManager>()
    private val pinRepository = mock<PinRepository>()
    private val biometricsController = mock<BiometricsController>()
    private val defaultLabels = mock<DefaultLabels>()

    @Before
    fun setup() {
        whenever(authPrefs.sharedKey).thenReturn("1234")
        whenever(authPrefs.walletGuid).thenReturn("4321")

        interactor = PinInteractor(
            walletOptionsDataManager = walletOptionsDataManager,
            credentialsWiper = credentialsWiper,
            payloadManager = payloadDataManager,
            pinRepository = pinRepository,
            biometricsController = biometricsController,
            mobileNoticeRemoteConfig = mobileNoticeRemoteConfig,
            authDataManager = authDataManager,
            apiStatus = apiStatus,
            authPrefs = authPrefs,
            persistentPrefs = persistentPrefs,
            walletStatus = walletStatus,
            defaultLabels = defaultLabels
        )
    }
}
