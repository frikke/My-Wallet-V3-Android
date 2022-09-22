package piuk.blockchain.android.ui.settings.notifications

import com.blockchain.android.testutils.rxInit
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.preferences.NotificationPrefs
import info.blockchain.wallet.api.data.Settings
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.notifications.EmailNotVerifiedException
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsInteractor
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class NotificationsInteractorTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    private lateinit var interactor: NotificationsInteractor

    private val notificationPrefs: NotificationPrefs = mockk(relaxed = true)
    private val notificationTokenManager: NotificationTokenManager = mockk(relaxed = true)
    private val settingsDataManager: SettingsDataManager = mockk(relaxed = true)
    private val payloadDataManager: PayloadDataManager = mockk(relaxed = true)

    @Before
    fun setup() {
        interactor = NotificationsInteractor(
            notificationPrefs = notificationPrefs,
            notificationTokenManager = notificationTokenManager,
            settingsDataManager = settingsDataManager,
            payloadDataManager = payloadDataManager
        )
    }

    @Test
    fun getNotificationInfo_pushOn_emailOn() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { notificationsType } returns listOf(Settings.NOTIFICATION_TYPE_EMAIL)
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)
        coEvery { notificationPrefs.arePushNotificationsEnabled() } returns true

        interactor.getNotificationsEnabled().test()
            .assertValue {
                it.first && it.second
            }

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
            notificationPrefs.arePushNotificationsEnabled()
        }
    }

    @Test
    fun getNotificationInfo_pushOff_emailOn() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { notificationsType } returns listOf(Settings.NOTIFICATION_TYPE_ALL)
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)
        coEvery { notificationPrefs.arePushNotificationsEnabled() } returns false

        interactor.getNotificationsEnabled().test()
            .assertValue {
                it.first && !it.second
            }

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
            notificationPrefs.arePushNotificationsEnabled()
        }
    }

    @Test
    fun getNotificationInfo_pushOn_emailOff() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { notificationsType } returns emptyList()
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)
        coEvery { notificationPrefs.arePushNotificationsEnabled() } returns true

        interactor.getNotificationsEnabled().test()
            .assertValue {
                !it.first && it.second
            }

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
            notificationPrefs.arePushNotificationsEnabled()
        }
    }

    @Test
    fun getNotificationInfo_pushOff_emailOff() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { notificationsType } returns emptyList()
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)
        coEvery { notificationPrefs.arePushNotificationsEnabled() } returns false

        interactor.getNotificationsEnabled().test()
            .assertValue {
                !it.first && !it.second
            }

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
            notificationPrefs.arePushNotificationsEnabled()
        }
    }

    @Test
    fun enablePushNotifications() {
        coEvery { notificationTokenManager.enableNotifications() } returns Completable.complete()

        interactor.enablePushNotifications().test()
            .assertComplete()

        coVerify(exactly = 1) {
            notificationTokenManager.enableNotifications()
        }
    }

    @Test
    fun disablePushNotifications() {
        coEvery { notificationTokenManager.disableNotifications() } returns Completable.complete()

        interactor.disablePushNotifications().test()
            .assertComplete()

        coVerify(exactly = 1) {
            notificationTokenManager.disableNotifications()
        }
    }

    @Test
    fun checkPushNotificationState() {
        coEvery { notificationPrefs.arePushNotificationsEnabled() } returns true

        interactor.arePushNotificationsEnabled().test()
            .assertValue(true)

        coVerify(exactly = 1) {
            notificationPrefs.arePushNotificationsEnabled()
        }
    }

    @Test
    fun toggleEmailNotifications_enabledToDisabled() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { isEmailVerified } returns true
            coEvery { notificationsType } returns listOf(Settings.NOTIFICATION_TYPE_EMAIL)
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)
        coEvery {
            settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType)
        } returns Observable.just(settingsMock)
        coEvery { payloadDataManager.syncPayloadAndPublicKeys() } returns Completable.complete()

        interactor.toggleEmailNotifications(true).test()
            .assertComplete()

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
            settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType)
            payloadDataManager.syncPayloadAndPublicKeys()
        }
    }

    @Test
    fun toggleEmailNotifications_disabledToEnabled() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { isEmailVerified } returns true
            coEvery { notificationsType } returns listOf(Settings.NOTIFICATION_TYPE_EMAIL)
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)
        coEvery {
            settingsDataManager.enableNotification(Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType)
        } returns Observable.just(settingsMock)

        interactor.toggleEmailNotifications(false).test()
            .assertComplete()

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
            settingsDataManager.enableNotification(Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType)
        }
        coVerify(exactly = 0) {
            payloadDataManager.syncPayloadAndPublicKeys()
        }
    }

    @Test
    fun toggleEmailNotifications_unverifiedEmail() {
        val settingsMock: Settings = mockk<Settings>(relaxed = true).apply {
            coEvery { isEmailVerified } returns false
        }
        coEvery { settingsDataManager.getSettings() } returns Observable.just(settingsMock)

        interactor.toggleEmailNotifications(true).test()
            .assertError {
                it is EmailNotVerifiedException
            }

        coVerify(exactly = 1) {
            settingsDataManager.getSettings()
        }
        coVerify { payloadDataManager wasNot Called }
    }
}
