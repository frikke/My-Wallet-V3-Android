package com.blockchain.core.settings

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.core.settings.datastore.SettingsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.RxTest
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.util.Arrays
import kotlinx.coroutines.flow.flowOf
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class SettingsDataManagerTest : RxTest() {

    private lateinit var subject: SettingsDataManager
    private val settingsService: SettingsService = mock()
    private val settingsStore: SettingsStore = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val walletSettingsService: WalletSettingsService = mock()
    private val assetCatalogue: AssetCatalogue = mock()

    @Before
    fun setUp() {
        subject = SettingsDataManager(
            settingsService,
            settingsStore,
            currencyPrefs,
            walletSettingsService,
            assetCatalogue
        )
    }

    @Test
    fun initSettings() {
        // Arrange
        val mockSettings = mock(Settings::class.java)
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.initSettings(guid, sharedKey).test()
        // Assert
        verify(settingsService).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun getSettings() {
        val mockSettings = mock(Settings::class.java)
        whenever(settingsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))).thenReturn(
            flowOf(DataResource.Data(mockSettings))
        )
        // Act
        val testObserver = subject.getSettings().test()
        // Assert
        verify(settingsStore).stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun updateEmail() {
        // Arrange
        val email = "EMAIL"
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateEmail(email)).thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.updateEmail(email).test()
        // Assert
        verify(settingsService).updateEmail(email)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun updateSms() {
        // Arrange
        val phoneNumber = "PHONE_NUMBER"
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateSms(phoneNumber)).thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.updateSms(phoneNumber).test()
        // Assert
        verify(settingsService).updateSms(phoneNumber)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun verifySms() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.verifySms(verificationCode))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.verifySms(verificationCode).test()
        // Assert
        verify(settingsService).verifySms(verificationCode)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun updateTor() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateTor(true)).thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.updateTor(true).test()
        // Assert
        verify(settingsService).updateTor(true)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun updateTwoFactor() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        val authType = 1337
        whenever(settingsService.updateTwoFactor(authType))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.updateTwoFactor(authType).test()
        // Assert
        verify(settingsService).updateTwoFactor(authType)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun enableNotificationNoneRegistered() {
        // Arrange
        val notifications = emptyList<Int>()
        val notificationType = SettingsManager.NOTIFICATION_TYPE_SMS
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(notificationType))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsService).updateNotifications(notificationType)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun enableNotificationEmailRegistered() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_SMS
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun enableNotificationSmsRegistered() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun enableNotificationAllEnabled() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_ALL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_EMAIL))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        verifyNoMoreInteractions(settingsService)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun disableNotificationNoneRegistered() {
        // Arrange
        val notifications = emptyList<Int>()
        val notificationType = SettingsManager.NOTIFICATION_TYPE_SMS
        val mockSettings = mock(Settings::class.java)
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun disableNotificationAllRegistered() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_ALL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun disableNotificationBothRegistered() {
        // Arrange
        val notifications = listOf(
            SettingsManager.NOTIFICATION_TYPE_EMAIL,
            SettingsManager.NOTIFICATION_TYPE_SMS
        )
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun disableNotificationOneRegisteredMatchesPassed() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(false))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(false)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE)
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun disableNotificationOneRegisteredDoesNotMatchPassed() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockSettings = mock(Settings::class.java)
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun disableNotificationEdgeCase() {
        // Arrange
        val notifications = Arrays.asList(1337, 1338)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockSettings = mock(Settings::class.java)
        whenever(settingsStore.stream(FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(mockSettings)))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verifyNoMoreInteractions(settingsService)
        verify(settingsStore).stream(FreshnessStrategy.Fresh)
        verifyNoMoreInteractions(settingsStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun triggerEmailAlert() {
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        whenever(walletSettingsService.triggerAlert(guid, sharedKey)).thenReturn(Completable.complete())

        val testObserver = subject.triggerEmailAlertLegacy(guid, sharedKey).test()

        verifyNoMoreInteractions(settingsService)
        verify(walletSettingsService).triggerAlert(guid, sharedKey)
        verifyNoMoreInteractions(walletSettingsService)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }
}
