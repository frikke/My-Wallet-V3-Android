package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.auth.MobileNoticeDialog
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.settings.v2.security.pin.PinInteractor
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.SessionPrefs

class PinInteractorTest {
    private lateinit var interactor: PinInteractor
    private val mobileNoticeRemoteConfig = mock<MobileNoticeRemoteConfig>()
    private val authDataManager = mock<AuthDataManager>()
    private val apiStatus = mock<ApiStatus>()
    private val authPrefs = mock<AuthPrefs>()
    private val sessionPrefs = mock<SessionPrefs>()
    private val walletStatusPrefs = mock<WalletStatusPrefs>()
    private val walletOptionsDataManager = mock<WalletOptionsDataManager>()
    private val credentialsWiper = mock<CredentialsWiper>()
    private val payloadDataManager = mock<PayloadDataManager>()
    private val pinRepository = mock<PinRepository>()
    private val biometricsController = mock<BiometricsController>()
    private val defaultLabels = mock<DefaultLabels>()
    private val isIntercomEnabledFlag = mock<FeatureFlag>()

    @Before
    fun setup() {
        whenever(authPrefs.sharedKey).thenReturn("1234")
        whenever(authPrefs.walletGuid).thenReturn("4321")

        interactor = spy(
            PinInteractor(
                walletOptionsDataManager = walletOptionsDataManager,
                credentialsWiper = credentialsWiper,
                payloadManager = payloadDataManager,
                pinRepository = pinRepository,
                biometricsController = biometricsController,
                mobileNoticeRemoteConfig = mobileNoticeRemoteConfig,
                authDataManager = authDataManager,
                apiStatus = apiStatus,
                authPrefs = authPrefs,
                persistentPrefs = sessionPrefs,
                walletStatus = walletStatusPrefs,
                defaultLabels = defaultLabels,
                isIntercomEnabledFlag = isIntercomEnabledFlag
            )
        )
    }

    @Test
    fun `validatePIN then call validatePin`() {
        val pin = "1234"
        whenever(authDataManager.validatePin(pin)).thenReturn(Observable.just(pin))
        whenever(isIntercomEnabledFlag.enabled).thenReturn(Single.just(false))
        doNothing().whenever(interactor).registerIntercomUser()
        val test = interactor.validatePIN(pin).test()
        test.assertValue {
            it == pin
        }

        verify(authDataManager).validatePin(pin)

        verifyNoMoreInteractions(authDataManager)
    }

    @Test
    fun `validatePassword then call initializeAndDecrypt`() {
        val password = "Test1234!"
        val sharedKey = "1234"
        val walletGuid = "4321"
        whenever(authPrefs.sharedKey).thenReturn(sharedKey)
        whenever(authPrefs.walletGuid).thenReturn(walletGuid)
        whenever(payloadDataManager.initializeAndDecrypt(sharedKey, walletGuid, password))
            .thenReturn(Completable.complete())

        interactor.validatePassword(password).test()

        verify(payloadDataManager).initializeAndDecrypt(sharedKey, walletGuid, password)

        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `doUpgradeWallet then call upgradeWalletPayload`() {
        val secondPassword = "Test1234!"
        val sharedKey = "1234"
        val defaultLabelName = "label"
        whenever(defaultLabels.getDefaultNonCustodialWalletLabel()).thenReturn(defaultLabelName)
        whenever(
            payloadDataManager.upgradeWalletPayload(
                secondPassword,
                defaultLabels.getDefaultNonCustodialWalletLabel()
            )
        ).thenReturn(Completable.complete())

        interactor.doUpgradeWallet(secondPassword, false).test()

        verify(payloadDataManager).upgradeWalletPayload(
            secondPassword, defaultLabels.getDefaultNonCustodialWalletLabel()
        )

        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `checkForceUpgradeStatus then call checkForceUpgrade`() {
        val versionName = "1234"
        whenever(walletOptionsDataManager.checkForceUpgrade(versionName))
            .thenReturn(Observable.just(mock()))

        interactor.checkForceUpgradeStatus(versionName).test()

        verify(walletOptionsDataManager).checkForceUpgrade(versionName)

        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun `createPin then call createPin`() {
        val tempPassword = "Test1234!"
        val pin = "1234"
        whenever(authDataManager.createPin(tempPassword, pin))
            .thenReturn(Completable.complete())
        whenever(authDataManager.verifyCloudBackup())
            .thenReturn(Completable.complete())

        interactor.createPin(tempPassword, pin).test()

        verify(authDataManager).createPin(tempPassword, pin)
        verify(authDataManager).verifyCloudBackup()

        verifyNoMoreInteractions(authDataManager)
    }

    @Test
    fun `fetchInfoMessage then call mobileNoticeDialog`() {
        val mobileNoticeDialog = MobileNoticeDialog(
            title = "Attention",
            body = "This is an important message",
            ctaText = "Understood",
            ctaLink = "www.blockchain.com"
        )
        whenever(mobileNoticeRemoteConfig.mobileNoticeDialog())
            .thenReturn(Single.just(mobileNoticeDialog))

        val test = interactor.fetchInfoMessage().test()
        test.assertValue {
            it.title == mobileNoticeDialog.title &&
                it.body == mobileNoticeDialog.body &&
                it.ctaText == mobileNoticeDialog.ctaText &&
                it.ctaLink == mobileNoticeDialog.ctaLink
        }

        verify(mobileNoticeRemoteConfig).mobileNoticeDialog()

        verifyNoMoreInteractions(mobileNoticeRemoteConfig)
    }

    @Test
    fun `getCurrentPin`() {
        val pin = "1234"
        whenever(pinRepository.pin).thenReturn(pin)

        interactor.getCurrentPin()

        verify(pinRepository).pin

        verifyNoMoreInteractions(pinRepository)
    }

    @Test
    fun `checkApiStatus then call isHealthy`() {
        whenever(apiStatus.isHealthy()).thenReturn(Single.just(true))

        val test = interactor.checkApiStatus().test()
        test.assertValue { it == true }

        verify(apiStatus).isHealthy()

        verifyNoMoreInteractions(apiStatus)
    }

    @Test
    fun `updatePayload then initializeAndDecrypt`() {
        val password = "Test1234!"
        val sharedKey = "1234"
        val walletGuid = "4321"
        whenever(authPrefs.sharedKey).thenReturn(sharedKey)
        whenever(authPrefs.walletGuid).thenReturn(walletGuid)
        whenever(payloadDataManager.initializeAndDecrypt(sharedKey, walletGuid, password))
            .thenReturn(Completable.complete())
        whenever(authDataManager.verifyCloudBackup()).thenReturn(Completable.complete())

        interactor.updatePayload(password).test()

        verify(payloadDataManager).initializeAndDecrypt(sharedKey, walletGuid, password)
        verify(authDataManager).verifyCloudBackup()

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(authDataManager)
    }
}
