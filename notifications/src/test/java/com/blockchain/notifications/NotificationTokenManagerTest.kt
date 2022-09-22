package com.blockchain.notifications

import com.blockchain.android.testutils.rxInit
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.NotificationPrefs
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class NotificationTokenManagerTest {
    private lateinit var subject: NotificationTokenManager

    private val notificationService: NotificationService = mockk(relaxed = true)
    private val payloadDataManager: PayloadDataManager = mockk(relaxed = true)
    private val prefs: NotificationPrefs = mockk(relaxed = true)
    private val authPrefs: AuthPrefs = mockk<AuthPrefs>(relaxed = true).apply {
        coEvery { walletGuid } returns GUID
        coEvery { sharedKey } returns SHARED_KEY
    }
    private val notificationTokenProvider: NotificationTokenProvider = mockk(relaxed = true)
    private val remoteLogger: RemoteLogger = mockk(relaxed = true)

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
        mainTrampoline()
    }

    @Before
    fun setUp() {
        subject = NotificationTokenManager(
            notificationService,
            payloadDataManager,
            prefs,
            authPrefs,
            notificationTokenProvider,
            remoteLogger
        )
    }

    @Test
    fun storeAndUpdateToken_disabledNotifications() {
        // Arrange
        coEvery { prefs.arePushNotificationsEnabled() } returns false
        coEvery { payloadDataManager.initialised } returns false

        // Act
        subject.storeAndUpdateToken("token")

        // Assert
        coVerify(exactly = 1) {
            prefs.arePushNotificationsEnabled()
            prefs.setFirebaseToken("token")
        }
    }

    @Test
    fun storeAndUpdateToken_enabledNotifications_notSignedIn() {
        // Arrange
        coEvery { prefs.arePushNotificationsEnabled() } returns true
        coEvery { payloadDataManager.initialised } returns false

        // Act
        subject.storeAndUpdateToken("token")

        // Assert
        coVerify(exactly = 1) {
            prefs.arePushNotificationsEnabled()
            prefs.setFirebaseToken("token")
        }
    }

    @Test
    fun storeAndUpdateToken_enabledNotifications_signedIn() {
        // Arrange
        coEvery { prefs.arePushNotificationsEnabled() } returns true
        coEvery { payloadDataManager.initialised } returns true
        coEvery { payloadDataManager.guid } returns "guid"
        coEvery { payloadDataManager.sharedKey } returns "sharedKey"
        coEvery { notificationService.sendNotificationToken(any(), any(), any()) } returns Completable.complete()

        // Act
        subject.storeAndUpdateToken("token")

        // Assert
        coVerify(exactly = 1) {
            prefs.arePushNotificationsEnabled()
            prefs.setFirebaseToken("token")
            notificationService.sendNotificationToken("token", "guid", "sharedKey")
        }
    }

    @Test
    fun enableNotifications_requestToken() {
        // Arrange
        coEvery { prefs.getFirebaseToken() } returns ""
        coEvery { notificationTokenProvider.notificationToken() } returns Single.just("token")

        // Act
        subject.enableNotifications().test()
            .assertComplete()

        // Assert
        coVerify(exactly = 1) {
            prefs.setPushNotificationsEnabled(true)
        }
        coVerify {
            notificationService wasNot Called
        }
    }

    @Test
    fun enableNotifications_storedToken() {
        val guid = "guid"
        val key = "sharedKey"
        val token = "token"

        coEvery { prefs.getFirebaseToken() } returns token
        coEvery { prefs.arePushNotificationsEnabled() } returns true
        coEvery { payloadDataManager.guid } returns guid
        coEvery { payloadDataManager.initialised } returns true
        coEvery { payloadDataManager.sharedKey } returns key
        coEvery { notificationService.sendNotificationToken(token, guid, key) } returns Completable.complete()

        subject.enableNotifications().test()
            .assertComplete()
            .assertNoErrors()

        coVerify(exactly = 1) {
            prefs.setPushNotificationsEnabled(true)
            notificationService.sendNotificationToken(token, guid, key)
        }
    }

    @Test
    fun disableNotifications() {
        // Arrange
        coEvery { prefs.getFirebaseToken() } returns ""
        coEvery { notificationTokenProvider.deleteToken() } returns Completable.complete()

        // Act
        subject.disableNotifications().test()
            .assertComplete()
            .assertNoErrors()

        // Assert
        coVerify {
            notificationTokenProvider.deleteToken()
            prefs.setPushNotificationsEnabled(false)
        }
        coVerify {
            notificationService wasNot Called
        }
    }

    @Test
    fun removeNotificationToken() {
        coEvery { prefs.getFirebaseToken() } returns "1234"
        coEvery { notificationTokenProvider.deleteToken() } returns Completable.complete()
        coEvery { notificationService.removeNotificationToken(GUID, SHARED_KEY) } returns Completable.complete()

        subject.disableNotifications().test()
            .assertComplete()
            .assertNoErrors()

        coVerify(exactly = 1) {
            prefs.setPushNotificationsEnabled(false)
            notificationTokenProvider.deleteToken()
            notificationService.removeNotificationToken(GUID, SHARED_KEY)
            prefs.getFirebaseToken()
            prefs.setFirebaseToken("")
        }
    }

    @Test
    fun removeNotificationToken_noToken() {
        coEvery { prefs.getFirebaseToken() } returns ""
        coEvery { notificationTokenProvider.deleteToken() } returns Completable.complete()

        subject.disableNotifications().test()
            .assertComplete()
            .assertNoErrors()

        coVerify(exactly = 1) {
            prefs.setPushNotificationsEnabled(false)
            notificationTokenProvider.deleteToken()
            prefs.getFirebaseToken()
            prefs.setFirebaseToken("")
        }
        coVerify {
            notificationService wasNot Called
        }
    }

    @Test
    fun removeNotificationToken_noPayload() {
        coEvery { prefs.getFirebaseToken() } returns "1234"
        coEvery { notificationTokenProvider.deleteToken() } returns Completable.complete()
        coEvery { notificationService.removeNotificationToken(GUID, SHARED_KEY) } returns Completable.complete()

        subject.disableNotifications().test()
            .assertComplete()
            .assertNoErrors()

        coVerify(exactly = 1) {
            notificationService.removeNotificationToken(GUID, SHARED_KEY)
            prefs.setPushNotificationsEnabled(false)
            notificationTokenProvider.deleteToken()
            prefs.getFirebaseToken()
            prefs.setFirebaseToken("")
        }
    }
}

private const val GUID = "GUID"
private const val SHARED_KEY = "SHARED_KEY"
