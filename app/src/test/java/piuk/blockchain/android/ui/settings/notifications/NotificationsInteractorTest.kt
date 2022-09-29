package piuk.blockchain.android.ui.settings.notifications

import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.preferences.NotificationPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.notifications.EmailNotVerifiedException
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsInteractor
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class NotificationsInteractorTest {

    private lateinit var interactor: NotificationsInteractor

    private val notificationPrefs: NotificationPrefs = mock()
    private val notificationTokenManager: NotificationTokenManager = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()

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
        val settingsMock: Settings = mock {
            on { notificationsType }.thenReturn(listOf(Settings.NOTIFICATION_TYPE_EMAIL))
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(notificationPrefs.arePushNotificationsEnabled).thenReturn(true)

        val test = interactor.getNotificationsEnabled().test()
        test.assertValue {
            it.first && it.second
        }

        verify(settingsDataManager).getSettings()
        verify(notificationPrefs).arePushNotificationsEnabled

        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(notificationPrefs)
    }

    @Test
    fun getNotificationInfo_pushOff_emailOn() {
        val settingsMock: Settings = mock {
            on { notificationsType }.thenReturn(listOf(Settings.NOTIFICATION_TYPE_ALL))
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(notificationPrefs.arePushNotificationsEnabled).thenReturn(false)

        val test = interactor.getNotificationsEnabled().test()
        test.assertValue {
            it.first && !it.second
        }

        verify(settingsDataManager).getSettings()
        verify(notificationPrefs).arePushNotificationsEnabled

        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(notificationPrefs)
    }

    @Test
    fun getNotificationInfo_pushOn_emailOff() {
        val settingsMock: Settings = mock {
            on { notificationsType }.thenReturn(emptyList())
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(notificationPrefs.arePushNotificationsEnabled).thenReturn(true)

        val test = interactor.getNotificationsEnabled().test()
        test.assertValue {
            !it.first && it.second
        }

        verify(settingsDataManager).getSettings()
        verify(notificationPrefs).arePushNotificationsEnabled

        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(notificationPrefs)
    }

    @Test
    fun getNotificationInfo_pushOff_emailOff() {
        val settingsMock: Settings = mock {
            on { notificationsType }.thenReturn(emptyList())
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(notificationPrefs.arePushNotificationsEnabled).thenReturn(false)

        val test = interactor.getNotificationsEnabled().test()
        test.assertValue {
            !it.first && !it.second
        }

        verify(settingsDataManager).getSettings()
        verify(notificationPrefs).arePushNotificationsEnabled

        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(notificationPrefs)
    }

    @Test
    fun enablePushNotifications() {
        whenever(notificationTokenManager.enableNotifications()).thenReturn(Completable.complete())

        val test = interactor.enablePushNotifications().test()
        test.assertComplete()

        verify(notificationTokenManager).enableNotifications()
        verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun disablePushNotifications() {
        whenever(notificationTokenManager.disableNotifications()).thenReturn(Completable.complete())

        val test = interactor.disablePushNotifications().test()
        test.assertComplete()

        verify(notificationTokenManager).disableNotifications()
        verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun checkPushNotificationState() {
        whenever(notificationPrefs.arePushNotificationsEnabled).thenReturn(true)

        val test = interactor.arePushNotificationsEnabled()
        Assert.assertTrue(test)

        verify(notificationPrefs).arePushNotificationsEnabled
        verifyNoMoreInteractions(notificationPrefs)
    }

    @Test
    fun toggleEmailNotifications_enabledToDisabled() {
        val settingsMock: Settings = mock {
            on { isEmailVerified }.thenReturn(true)
            on { notificationsType }.thenReturn(listOf(Settings.NOTIFICATION_TYPE_EMAIL))
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(
            settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType)
        ).thenReturn(
            Observable.just(settingsMock)
        )
        whenever(payloadDataManager.syncPayloadAndPublicKeys()).thenReturn(Completable.complete())

        val test = interactor.toggleEmailNotifications(true).test()
        test.assertComplete()

        verify(settingsDataManager).getSettings()
        verify(settingsDataManager).disableNotification(
            Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType
        )
        verify(payloadDataManager).syncPayloadAndPublicKeys()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun toggleEmailNotifications_disabledToEnabled() {
        val settingsMock: Settings = mock {
            on { isEmailVerified }.thenReturn(true)
            on { notificationsType }.thenReturn(listOf(Settings.NOTIFICATION_TYPE_EMAIL))
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(
            settingsDataManager.enableNotification(Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType)
        ).thenReturn(
            Observable.just(settingsMock)
        )

        val test = interactor.toggleEmailNotifications(false).test()
        test.assertComplete()

        verify(settingsDataManager).getSettings()
        verify(settingsDataManager).enableNotification(
            Settings.NOTIFICATION_TYPE_EMAIL, settingsMock.notificationsType
        )
        verify(payloadDataManager, never()).syncPayloadAndPublicKeys()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(payloadDataManager)
    }

    @Test
    fun toggleEmailNotifications_unverifiedEmail() {
        val settingsMock: Settings = mock {
            on { isEmailVerified }.thenReturn(false)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))

        val test = interactor.toggleEmailNotifications(true).test()
        test.assertError {
            it is EmailNotVerifiedException
        }

        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(payloadDataManager)
    }
}
