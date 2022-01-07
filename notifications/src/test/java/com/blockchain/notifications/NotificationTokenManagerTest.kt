package com.blockchain.notifications

import com.blockchain.android.testutils.rxInit
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.NotificationPrefs
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString

class NotificationTokenManagerTest {
    private lateinit var subject: NotificationTokenManager

    private val notificationService: NotificationService = mock()
    private val payloadManager: PayloadManager = mock()
    private val prefs: NotificationPrefs = mock()
    private val authPrefs: AuthPrefs = mock {
        on { walletGuid }.thenReturn(GUID)
        on { sharedKey }.thenReturn(SHARED_KEY)
    }
    private val notificationTokenProvider: NotificationTokenProvider = mock()
    private val crashLogger: CrashLogger = mock()

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
            payloadManager,
            prefs,
            authPrefs,
            notificationTokenProvider,
            crashLogger
        )
    }

    @Test
    fun storeAndUpdateToken_disabledNotifications() {
        // Arrange
        whenever(prefs.arePushNotificationsEnabled).thenReturn(false)
        whenever(payloadManager.payload).thenReturn(null)

        // Act
        subject.storeAndUpdateToken("token")
        // Assert
        verify(prefs).arePushNotificationsEnabled
        verify(prefs).firebaseToken = "token"
        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun storeAndUpdateToken_enabledNotifications_notSignedIn() {
        // Arrange
        whenever(prefs.arePushNotificationsEnabled).thenReturn(true)
        whenever(payloadManager.payload).thenReturn(null)

        // Act
        subject.storeAndUpdateToken("token")
        // Assert
        verify(prefs).arePushNotificationsEnabled
        verify(prefs).firebaseToken = "token"
        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun storeAndUpdateToken_enabledNotifications_signedIn() {
        // Arrange
        whenever(prefs.arePushNotificationsEnabled).thenReturn(true)
        val mockPayload = mock<Wallet>()
        whenever(mockPayload.guid).thenReturn("guid")
        whenever(mockPayload.sharedKey).thenReturn("sharedKey")
        whenever(payloadManager.payload).thenReturn(mockPayload)
        whenever(
            notificationService.sendNotificationToken(
                anyString(), anyString(), anyString()
            )
        ).thenReturn(
            Completable.complete()
        )

        // Act
        subject.storeAndUpdateToken("token")
        // Assert
        verify(prefs).arePushNotificationsEnabled
        verify(prefs).firebaseToken = "token"
        verify(notificationService).sendNotificationToken("token", "guid", "sharedKey")
        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun enableNotifications_requestToken() {
        // Arrange
        whenever(prefs.firebaseToken).thenReturn("")
        whenever(notificationTokenProvider.notificationToken()).thenReturn(Single.just("token"))

        // Act
        subject.enableNotifications()
        // Assert
        verify(prefs).arePushNotificationsEnabled = true
        verifyNoMoreInteractions(notificationService)
    }

    @Test
    fun enableNotifications_storedToken() {
        val guid = "guid"
        val key = "sharedKey"
        val token = "token"

        whenever(prefs.firebaseToken).thenReturn(token)
        whenever(prefs.arePushNotificationsEnabled).thenReturn(true)
        val mockPayload = mock<Wallet> {
            on { this.guid }.thenReturn(guid)
            on { sharedKey }.thenReturn(key)
        }

        whenever(payloadManager.payload).thenReturn(mockPayload)
        whenever(notificationService.sendNotificationToken(token, guid, key)).thenReturn(Completable.complete())

        val testObservable = subject.enableNotifications().test()

        testObservable.assertComplete()
        testObservable.assertNoErrors()
        verify(prefs).arePushNotificationsEnabled = true
        verify(payloadManager, atLeastOnce()).payload
        verify(notificationService).sendNotificationToken(token, guid, key)
        verifyNoMoreInteractions(notificationService)
    }

    @Test
    fun disableNotifications() {
        // Arrange
        whenever(prefs.firebaseToken).thenReturn("")
        whenever(notificationTokenProvider.deleteToken()).thenReturn(Completable.complete())

        // Act
        val testObservable = subject.disableNotifications().test()
        // Assert
        testObservable.assertComplete()
        testObservable.assertNoErrors()
        verify(notificationTokenProvider).deleteToken()
        verifyZeroInteractions(notificationService)
        verify(prefs).arePushNotificationsEnabled = false
    }

    @Test
    fun removeNotificationToken() {
        whenever(prefs.firebaseToken).thenReturn("1234")
        whenever(notificationTokenProvider.deleteToken()).thenReturn(Completable.complete())
        whenever(notificationService.removeNotificationToken(GUID, SHARED_KEY)).thenReturn(Completable.complete())

        val testObservable = subject.disableNotifications().test()

        testObservable.assertComplete()
        testObservable.assertNoErrors()
        verify(prefs).arePushNotificationsEnabled = false
        verify(notificationTokenProvider).deleteToken()
        verify(notificationService).removeNotificationToken(GUID, SHARED_KEY)
        verify(prefs).firebaseToken
        verify(prefs).firebaseToken = ""
        verifyNoMoreInteractions(notificationService)
        verifyNoMoreInteractions(notificationTokenProvider)
        verifyNoMoreInteractions(prefs)
        verifyZeroInteractions(payloadManager)
    }

    @Test
    fun removeNotificationToken_noToken() {
        whenever(prefs.firebaseToken).thenReturn("")
        whenever(notificationTokenProvider.deleteToken()).thenReturn(Completable.complete())

        val testObservable = subject.disableNotifications().test()

        testObservable.assertComplete()
        testObservable.assertNoErrors()
        verify(prefs).arePushNotificationsEnabled = false
        verify(notificationTokenProvider).deleteToken()
        verify(prefs).firebaseToken
        verify(prefs).firebaseToken = ""
        verifyNoMoreInteractions(notificationService)
        verifyNoMoreInteractions(notificationTokenProvider)
        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun removeNotificationToken_noPayload() {

        whenever(prefs.firebaseToken).thenReturn("1234")
        whenever(notificationTokenProvider.deleteToken()).thenReturn(Completable.complete())
        whenever(notificationService.removeNotificationToken(GUID, SHARED_KEY))
            .thenReturn(
                Completable.complete()
            )

        val testObservable = subject.disableNotifications().test()

        testObservable.assertComplete()
        testObservable.assertNoErrors()
        verify(notificationService).removeNotificationToken(GUID, SHARED_KEY)
        verify(prefs).arePushNotificationsEnabled = false
        verify(notificationTokenProvider).deleteToken()
        verify(prefs).firebaseToken
        verify(prefs).firebaseToken = ""
        verifyZeroInteractions(payloadManager)
        verifyNoMoreInteractions(notificationService)
        verifyNoMoreInteractions(notificationTokenProvider)
        verifyNoMoreInteractions(prefs)
    }
}

private const val GUID = "GUID"
private const val SHARED_KEY = "SHARED_KEY"
