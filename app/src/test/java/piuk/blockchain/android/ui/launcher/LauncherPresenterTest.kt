package piuk.blockchain.android.ui.launcher

import android.content.Intent
import com.blockchain.android.testutils.rxInit
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class LauncherPresenterTest {
    private val launcherActivity: LauncherView = mock()
    private val prefsUtil: PersistentPrefs = mock()
    private val appUtil: AppUtil = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val deepLinkPersistence: DeepLinkPersistence = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val wallet: Wallet = mock()
    private val notificationTokenManager: NotificationTokenManager = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val featureFlag: FeatureFlag = mock()
    private val userIdentity: UserIdentity = mock()
    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency }.thenReturn(SELECTED_FIAT)
        on { defaultFiatCurrency }.thenReturn(DEFAULT_FIAT)
    }
    private val analytics: Analytics = mock()
    private val crashLogger: CrashLogger = mock()
    private val prerequisites: Prerequisites = mock()
    private val walletPrefs = mock<WalletStatus>()
    private val nabuUserDataManager = mock<NabuUserDataManager>()

    private val subject = LauncherPresenter(
        appUtil,
        payloadDataManager,
        prefsUtil,
        deepLinkPersistence,
        settingsDataManager,
        notificationTokenManager,
        environmentConfig,
        currencyPrefs,
        analytics,
        prerequisites,
        userIdentity,
        crashLogger,
        walletPrefs,
        nabuUserDataManager
    )

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject.initView(launcherActivity)

        whenever(featureFlag.enabled).thenReturn(Single.just(false))
        val settings: Settings = mock()
        whenever(settingsDataManager.updateFiatUnit(anyString()))
            .thenReturn(Observable.just(settings))
        whenever(walletPrefs.countrySelectedOnSignUp).thenReturn("US")
        whenever(walletPrefs.stateSelectedOnSignUp).thenReturn("US-FL")

        whenever(
            nabuUserDataManager.saveUserInitialLocation(
                walletPrefs.countrySelectedOnSignUp,
                walletPrefs.stateSelectedOnSignUp
            )
        ).thenReturn(Completable.complete())
    }

    @Test
    fun onViewReadyVerifiedEmailVerified() {
        // Arrange
        val pinValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(true)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinValidatedData
        )
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())

        val mockSettings: Settings = mock()
        whenever(prerequisites.initSettings(anyString(), anyString())).thenReturn(Single.just(mockSettings))
        whenever(prerequisites.warmCaches()).thenReturn(Completable.complete())
        whenever(prefsUtil.isLoggedOut).thenReturn(false)

        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)
        whenever(mockSettings.isEmailVerified).thenReturn(true)
        whenever(mockSettings.currency).thenReturn("USD")
        whenever(notificationTokenManager.resendNotificationToken()).thenReturn(Completable.complete())

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onStartMainActivity(null, false)
    }

    /**
     * Everything is good, email not verified and getting [Settings] object failed. Should
     * re-request PIN code.
     */
    @Test
    fun onViewReadyNonVerifiedEmailSettingsFailure() {
        // Arrange
        val pinValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(true)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinValidatedData
        )

        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())
        whenever(prerequisites.initSettings(anyString(), anyString())).thenReturn(Single.error(Throwable()))
        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verify(launcherActivity).onRequestPin()
    }

    /**
     * Bitcoin URI is found, expected to step into Bitcoin branch and call [ ][LauncherActivity.onStartMainActivity]
     */
    @Test
    fun onViewReadyBitcoinUri() {
        // Arrange
        val pinValidatedWithBitcoinUriData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(true)
            on { action }.thenReturn(Intent.ACTION_VIEW)
            on { scheme }.thenReturn("bitcoin")
            on { data }.thenReturn("bitcoin uri")
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinValidatedWithBitcoinUriData
        )
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(prefsUtil.isLoggedOut).thenReturn(false)

        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())

        val mockSettings: Settings = mock()
        whenever(prerequisites.initSettings(anyString(), anyString())).thenReturn(Single.just(mockSettings))
        whenever(prerequisites.warmCaches()).thenReturn(Completable.complete())

        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)
        whenever(mockSettings.isEmailVerified).thenReturn(true)
        whenever(mockSettings.currency).thenReturn("USD")
        whenever(notificationTokenManager.resendNotificationToken()).thenReturn(Completable.complete())

        // Act
        subject.onViewReady()

        // Assert
        verify(prefsUtil).setValue(PersistentPrefs.KEY_SCHEME_URL, "bitcoin uri")
        verify(launcherActivity).onStartMainActivity(null, false)
    }

    /**
     * Everything is fine, but PIN not validated.
     */
    @Test
    fun onViewReadyNotVerified() {
        // Arrange
        val pinUnValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(false)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinUnValidatedData
        )
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onRequestPin()
    }

    /**
     * Everything is fine, but PIN not validated. However, [AccessState] returns logged in.
     */
    @Test
    fun onViewReadyPinValidatedAndLoggedIn() {
        // Arrange
        val pinUnValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(true)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinUnValidatedData
        )
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())
        val mockSettings: Settings = mock()
        whenever(prerequisites.initSettings(WALLET_GUID, SHARED_KEY)).thenReturn(Single.just(mockSettings))
        whenever(prerequisites.warmCaches()).thenReturn(Completable.complete())
        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)
        whenever(mockSettings.isEmailVerified).thenReturn(true)
        whenever(mockSettings.currency).thenReturn("USD")
        whenever(notificationTokenManager.resendNotificationToken()).thenReturn(Completable.complete())

        // Act
        subject.onViewReady()

        // Assert
        verify(prefsUtil).isLoggedOut
        verify(launcherActivity).onStartMainActivity(null, false)
    }

    /**
     * GUID not found
     */
    @Test
    fun onViewReadyNoGuid() {
        // Arrange
        val pinUnValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(false)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinUnValidatedData
        )
        whenever(prefsUtil.walletGuid).thenReturn("")

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onNoGuid()
    }

    /**
     * Pin not found
     */
    @Test
    fun onViewReadyNoPin() {
        // Arrange
        val pinUnValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(false)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinUnValidatedData
        )
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("")

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onRequestPin()
    }

    @Test
    fun onViewReadyNotSane() {
        // Arrange
        val pinUnValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(false)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinUnValidatedData
        )
        whenever(prefsUtil.pinId).thenReturn("1234")
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(appUtil.isSane).thenReturn(false)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onCorruptPayload()
    }

    /**
     * GUID exists, Shared Key exists but user logged out.
     */
    @Test
    fun onViewReadyUserLoggedOut() {
        // Arrange
        val pinUnValidatedData: ViewIntentData = mock {
            on { isPinValidated }.thenReturn(false)
        }
        whenever(launcherActivity.getViewIntentData()).thenReturn(
            pinUnValidatedData
        )
        whenever(prefsUtil.isLoggedOut).thenReturn(true)
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onReEnterPassword()
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
        private const val WALLET_GUID = "0000-0000-0000-0000-0000"
        private const val SHARED_KEY = "123123123"

        private const val SELECTED_FIAT = "USD"
        private const val DEFAULT_FIAT = "USD"
    }
}
