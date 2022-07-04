package piuk.blockchain.android.ui.launcher

import android.content.Intent
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.SecurityPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.EncryptedPrefs
import piuk.blockchain.androidcore.utils.SessionPrefs

@RunWith(MockitoJUnitRunner::class)
class LauncherPresenterTest {

    private val launcherActivity: LauncherView = mock()
    private val deepLinkPersistence: DeepLinkPersistence = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val appUtil: AppUtil = mock()
    private val viewIntentData: ViewIntentData = mock()
    private val sessionPrefs: SessionPrefs = mock()
    private val authPrefs: AuthPrefs = mock()
    private val securityPrefs: SecurityPrefs = mock()
    private val referralPrefs: ReferralPrefs = mock()
    private val encryptedPrefs: EncryptedPrefs = mock()

    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase = mock()
    private val appMaintenanceFF: FeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(true))
    }

    private val subject = LauncherPresenter(
        appUtil,
        deepLinkPersistence,
        environmentConfig,
        authPrefs,
        getAppMaintenanceConfigUseCase,
        appMaintenanceFF,
        sessionPrefs,
        securityPrefs,
        referralPrefs,
        encryptedPrefs
    )

    @Test
    fun onViewAttached_setsBitcoinUri() {
        // Arrange
        val bitcoinUriData: ViewIntentData = mock {
            on { action }.thenReturn(Intent.ACTION_VIEW)
            on { scheme }.thenReturn("bitcoin")
            on { data }.thenReturn("bitcoin uri")
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(bitcoinUriData)
        whenever(authPrefs.walletGuid).thenReturn(WALLET_GUID)
        whenever(authPrefs.pinId).thenReturn(PIN_ID)
        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(sessionPrefs).keySchemeUrl = "bitcoin uri"
    }

    @Test
    fun onViewAttached_setsMetadataUri() {
        // Arrange
        val metadata: ViewIntentData = mock {
            on { action }.thenReturn(Intent.ACTION_VIEW)
            on { dataString }.thenReturn("blockchain")
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(metadata)
        whenever(authPrefs.walletGuid).thenReturn("")
        whenever(authPrefs.pinId).thenReturn("")

        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(sessionPrefs).metadataUri = "blockchain"
    }

    @Test
    fun onViewAttached_notValidGuid_callsOnCorruptPayload() {
        // Arrange
        whenever(launcherActivity.getViewIntentData()).thenReturn(viewIntentData)
        whenever(authPrefs.pinId).thenReturn(PIN_ID)
        whenever(authPrefs.walletGuid).thenReturn(INVALID_WALLET_GUID)

        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(launcherActivity).onCorruptPayload()
    }

    @Test
    fun onViewAttached_noGuidAndNoPinId_callsOnRequestPin() {
        // Arrange
        whenever(launcherActivity.getViewIntentData()).thenReturn(viewIntentData)
        whenever(encryptedPrefs.hasBackup()).thenReturn(false)
        whenever(authPrefs.walletGuid).thenReturn(WALLET_GUID)
        whenever(authPrefs.pinId).thenReturn(PIN_ID)
        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(launcherActivity).onRequestPin()
    }

    @Test
    fun onViewAttached_isLoggedOut_callsOnReenterPassword() {
        // Arrange
        whenever(launcherActivity.getViewIntentData()).thenReturn(viewIntentData)
        whenever(encryptedPrefs.hasBackup()).thenReturn(false)
        whenever(authPrefs.walletGuid).thenReturn(WALLET_GUID)
        whenever(authPrefs.pinId).thenReturn("")
        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(launcherActivity).onReenterPassword()
    }

    @Test
    fun onViewAttached_noGuidAndNoBackup_callsOnNoGuid() {
        // Arrange
        whenever(launcherActivity.getViewIntentData()).thenReturn(viewIntentData)
        whenever(encryptedPrefs.hasBackup()).thenReturn(false)
        whenever(authPrefs.walletGuid).thenReturn("")
        whenever(authPrefs.pinId).thenReturn("")
        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(launcherActivity).onNoGuid()
    }

    @Test
    fun onViewAttached_noGuidAndBackup_callsOnRequestPin() = runBlocking {
        // Arrange
        whenever(launcherActivity.getViewIntentData()).thenReturn(viewIntentData)
        whenever(encryptedPrefs.hasBackup()).thenReturn(true)
        whenever(authPrefs.walletGuid).thenReturn("")
        whenever(authPrefs.pinId).thenReturn("")

        // Act
        subject.attachView(launcherActivity)
        subject.resumeAppFlow()

        // Assert
        verify(launcherActivity).onRequestPin()
    }

    @Test
    fun clearCredentialsAndRestart() {
        // Arrange

        // Act
        subject.clearCredentialsAndRestart()
        // Assert
        verify(appUtil).clearCredentialsAndRestart()
    }

    companion object {
        private const val WALLET_GUID = "d5f7c5db-072c-4178-b563-393259ec173a"
        private const val INVALID_WALLET_GUID = "0000-0000-0000-0000-00231231223400"
        private const val PIN_ID = "1234"
    }
}
