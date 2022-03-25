package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.core.payments.EligiblePaymentMethodType
import com.blockchain.core.payments.LinkedPaymentMethod
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.BankState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.responses.nabu.NabuApiException.Companion.fromResponseBody
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.testutils.EUR
import com.blockchain.testutils.GBP
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.domain.usecases.LinkAccess
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import retrofit2.HttpException
import retrofit2.Response.error
import thepit.PitLinking
import thepit.PitLinkingState

class SettingsPresenterTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var subject: SettingsPresenter

    private val activity: SettingsView = mock()

    private val biometricsController: BiometricsController = mock()
    private val authDataManager: AuthDataManager = mock()

    private val settingsDataManager: SettingsDataManager = mock()

    private val payloadManager: PayloadManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()

    private val prefsUtil: PersistentPrefs = mock()
    private val pinRepository: PinRepository = mock()

    private val notificationTokenManager: NotificationTokenManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val emailSyncUpdater: EmailSyncUpdater = mock()
    private val pitLinking: PitLinking = mock()
    private val pitLinkState: PitLinkingState = mock()
    private val ratingPrefs: RatingPrefs = mock()
    private val qrProcessor: QrScanResultProcessor = mock()
    private val secureChannelManager: SecureChannelManager = mock()

    private val featureFlag: FeatureFlag = mock()

    private val analytics: Analytics = mock()
    private val paymentsDataManager: PaymentsDataManager = mock()
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase = mock()
    private val cardsFeatureFlag: FeatureFlag = mock()
    private val fundsFeatureFlag: FeatureFlag = mock()
    private val securityPrefs: SecurityPrefs = mock()

    @Before
    fun setUp() {
        subject = SettingsPresenter(
            authDataManager = authDataManager,
            settingsDataManager = settingsDataManager,
            emailUpdater = emailSyncUpdater,
            payloadManager = payloadManager,
            payloadDataManager = payloadDataManager,
            prefs = prefsUtil,
            pinRepository = pinRepository,
            paymentsDataManager = paymentsDataManager,
            getAvailablePaymentMethodsTypesUseCase = getAvailablePaymentMethodsTypesUseCase,
            notificationTokenManager = notificationTokenManager,
            exchangeRates = exchangeRates,
            kycStatusHelper = kycStatusHelper,
            pitLinking = pitLinking,
            analytics = analytics,
            biometricsController = biometricsController,
            ratingPrefs = ratingPrefs,
            qrProcessor = qrProcessor,
            secureChannelManager = secureChannelManager,
            securityPrefs = securityPrefs
        )
        subject.initView(activity)
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        whenever(prefsUtil.arePushNotificationsEnabled).thenReturn(false)
        whenever(securityPrefs.areScreenshotsEnabled).thenReturn(false)
        whenever(biometricsController.isHardwareDetected).thenReturn(false)
        whenever(prefsUtil.getValue(any(), any<Boolean>())).thenReturn(false)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadAndPublicKeys()).thenReturn(Completable.complete())
    }

    @Test
    fun onViewReadySuccess() {
        // Arrange
        val mockSettings: Settings = mock {
            on { isNotificationsOn }.thenReturn(true)
            on { notificationsType }.thenReturn(listOf(1, 32))
            on { smsNumber }.thenReturn("sms")
            on { email }.thenReturn("email")
        }

        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings))
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(pitLinkState.isLinked).thenReturn(false)
        whenever(paymentsDataManager.getLinkedCards(any())).thenReturn(Single.just(emptyList()))
        whenever(pitLinking.state).thenReturn(Observable.just(pitLinkState))

        whenever(featureFlag.enabled).thenReturn(Single.just(true))
        whenever(cardsFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(fundsFeatureFlag.enabled).thenReturn(Single.just(true))

        arrangeEligiblePaymentMethodTypes(USD, listOf(EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)))
        whenever(paymentsDataManager.canTransactWithBankMethods(any())).thenReturn(Single.just(false))
        arrangeBanks(emptyList())
        arrangeEligiblePaymentMethodTypes(USD, emptyList())
        // Act
        subject.onViewReady()
        // Assert
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).setUpUi()
        verify(activity).setPitLinkingState(false)
        verify(activity, Mockito.times(2)).updateCards(emptyList())
    }

    @Test
    fun onViewReadyFailed() {
        // Arrange
        whenever(
            settingsDataManager.fetchSettings()
        ).thenReturn(Observable.error(Throwable()))
        whenever(pitLinkState.isLinked).thenReturn(false)
        whenever(pitLinking.state).thenReturn(Observable.just(pitLinkState))
        whenever(featureFlag.enabled).thenReturn(Single.just(false))
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        whenever(cardsFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(fundsFeatureFlag.enabled).thenReturn(Single.just(false))

        whenever(paymentsDataManager.getLinkedCards(any())).thenReturn(Single.just(emptyList()))
        whenever(paymentsDataManager.canTransactWithBankMethods(any())).thenReturn(Single.just(false))
        arrangeEligiblePaymentMethodTypes(USD, listOf(EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)))
        arrangeBanks(emptyList())
        arrangeEligiblePaymentMethodTypes(USD, emptyList())

        // Act
        subject.onViewReady()

        // Assert
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).setUpUi()
        verify(activity, times(2)).updateCards(emptyList())
    }

    @Test
    fun updateEmailSuccess() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)

        val mockSettings = Settings().copy(notificationsType = notifications)

        val email = "EMAIL"
        whenever(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.just(Email(email, false)))
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications))
            .thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateEmail(email)

        // Assert
        verify(emailSyncUpdater).updateEmailAndSync(email)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications)
        verify(activity).showDialogEmailVerification()
    }

    @Test
    fun updateEmailFailed() {
        // Arrange
        val email = "EMAIL"
        whenever(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.error(Throwable()))

        // Act
        subject.updateEmail(email)

        // Assert
        verify(emailSyncUpdater).updateEmailAndSync(email)
        verify(activity).showSnackbar(R.string.update_failed)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateSmsInvalid() {
        // Arrange
        subject.updateSms("")
        // Assert
        verify(activity).setSmsUnknown()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateSmsSuccess() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)

        val mockSettings = Settings().copy(notificationsType = notifications)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete())

        // Act
        subject.updateSms(phoneNumber)

        // Assert
        verify(settingsDataManager).updateSms(phoneNumber)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        verify(activity).showDialogVerifySms()
    }

    @Test
    fun updateSmsSuccess_despiteNumberAlreadyRegistered() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)

        val mockSettings = Settings().copy(notificationsType = notifications)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.updateSms(phoneNumber)
        ).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        )
            .thenReturn(Observable.just(mockSettings))
        val responseBody = ResponseBody.create("application/json".toMediaTypeOrNull(), "{}")
        val error = fromResponseBody(HttpException(error<Any>(409, responseBody)))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.error(error))

        // Act
        subject.updateSms(phoneNumber)

        // Assert
        verify(settingsDataManager).updateSms(phoneNumber)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        verify(activity).showDialogVerifySms()
    }

    @Test
    fun updateSmsFailed() {
        // Arrange
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateSms(phoneNumber)

        // Assert
        verify(settingsDataManager).updateSms(phoneNumber)
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).showSnackbar(R.string.update_failed)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun verifySmsSuccess() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        val mockSettings = Settings()
        whenever(settingsDataManager.verifySms(verificationCode)).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete())

        // Act
        subject.verifySms(verificationCode)

        // Assert
        verify(settingsDataManager).verifySms(verificationCode)
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showDialogSmsVerified()
    }

    @Test
    fun verifySmsFailed() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        whenever(settingsDataManager.verifySms(ArgumentMatchers.anyString())).thenReturn(Observable.error(Throwable()))

        // Act
        subject.verifySms(verificationCode)
        // Assert
        verify(settingsDataManager).verifySms(verificationCode)
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showWarningDialog(R.string.verify_sms_failed)
    }

    @Test
    fun updateTorSuccess() {
        // Arrange
        val mockSettings = Settings().copy(
            blockTorIps = 1
        )
        whenever(settingsDataManager.updateTor(true)).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateTor(true)
        // Assert
        verify(settingsDataManager).updateTor(true)
        verify(activity).setTorBlocked(true)
    }

    @Test
    fun updateTorFailed() {
        // Arrange
        Mockito.`when`(settingsDataManager.updateTor(true)).thenReturn(Observable.error(Throwable()))
        // Act
        subject.updateTor(true)
        // Assert
        verify(settingsDataManager).updateTor(true)
        verify(activity).showSnackbar(R.string.update_failed)
    }

    @Test
    fun update2FaSuccess() {
        // Arrange
        val mockSettings = Settings()
        val authType = SettingsManager.AUTH_TYPE_YUBI_KEY
        Mockito.`when`(
            settingsDataManager.updateTwoFactor(authType)
        ).thenReturn(Observable.just(mockSettings))
        // Act
        subject.updateTwoFa(authType)
        // Assert
        verify(settingsDataManager).updateTwoFactor(authType)
    }

    @Test
    fun update2FaFailed() {
        // Arrange
        val authType = SettingsManager.AUTH_TYPE_YUBI_KEY
        whenever(
            settingsDataManager.updateTwoFactor(authType)
        ).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateTwoFa(authType)
        // Assert
        verify(settingsDataManager).updateTwoFactor(authType)
        verify(activity).showSnackbar(R.string.update_failed)
    }

    @Test
    fun enableNotificationSuccess() {
        // Arrange
        val mockSettingsResponse = Settings()
        val mockSettings = Settings().copy(
            notificationsType = listOf(
                SettingsManager.NOTIFICATION_TYPE_NONE
            )
        )

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        whenever(
            settingsDataManager.enableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(
                    SettingsManager.NOTIFICATION_TYPE_NONE
                )
            )
        )
            .thenReturn(Observable.just(mockSettingsResponse))
        // Act
        subject.updateEmailNotification(true)
        // Assert
        verify(settingsDataManager)
            .enableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(
                    SettingsManager.NOTIFICATION_TYPE_NONE
                )
            )
        verify(payloadDataManager).syncPayloadAndPublicKeys()
        verify(activity).setEmailNotificationPref(true)
    }

    @Test
    fun disableNotificationSuccess() {
        // Arrange

        val mockSettingsResponse = Settings()
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        )

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        whenever(
            settingsDataManager.disableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
            )
        ).thenReturn(Observable.just(mockSettingsResponse))
        // Act
        subject.updateEmailNotification(false)
        // Assert
        verify(settingsDataManager)
            .disableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
            )

        verify(payloadDataManager).syncPayloadWithServer()
        verify(activity).setEmailNotificationPref(ArgumentMatchers.anyBoolean())
    }

    @Test
    fun enableNotificationAlreadyEnabled() {
        // Arrange
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL),
            notificationsOn = 1
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateEmailNotification(true)

        // Assert
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).setEmailNotificationPref(true)
    }

    @Test
    fun disableNotificationAlreadyDisabled() {
        // Assert
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_NONE),
            notificationsOn = 1
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateEmailNotification(false)

        // Assert
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).setEmailNotificationPref(false)
    }

    @Test
    fun enableNotificationFailed() {
        // Arrange
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.enableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
            )
        ).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateEmailNotification(true)

        // Assert
        verify(settingsDataManager).enableNotification(
            SettingsManager.NOTIFICATION_TYPE_EMAIL,
            listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
        )
        verify(activity).showSnackbar(R.string.update_failed)
    }

    @Test
    fun pinCodeValidatedForChange() {
        // Arrange

        // Act
        subject.pinCodeValidatedForChange()
        // Assert
        verify(prefsUtil).pinFails = 0
        verify(prefsUtil).pinId = ""
        verify(activity).goToPinEntryPage()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updatePasswordSuccess() {
        // Arrange
        val newPassword = "NEW_PASSWORD"
        val oldPassword = "OLD_PASSWORD"
        val pin = "PIN"
        whenever(pinRepository.pin).thenReturn(pin)
        whenever(authDataManager.createPin(newPassword, pin)).thenReturn(Completable.complete())
        whenever(authDataManager.verifyCloudBackup()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())

        // Act
        subject.updatePassword(newPassword, oldPassword)

        // Assert
        verify(pinRepository).pin
        verify(authDataManager).createPin(newPassword, pin)
        verify(payloadDataManager).syncPayloadWithServer()
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showSnackbar(R.string.password_changed, SnackbarType.Success)
    }

    @Test
    fun updatePasswordFailed() {
        // Arrange
        val newPassword = "NEW_PASSWORD"
        val oldPassword = "OLD_PASSWORD"
        val pin = "PIN"
        whenever(pinRepository.pin).thenReturn(pin)
        whenever(authDataManager.createPin(newPassword, pin))
            .thenReturn(Completable.error(Throwable()))
        whenever(authDataManager.verifyCloudBackup()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())

        // Act
        subject.updatePassword(newPassword, oldPassword)

        // Assert
        verify(pinRepository).pin
        verify(authDataManager).createPin(newPassword, pin)
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadManager).tempPassword = newPassword
        verify(payloadManager).tempPassword = oldPassword
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showSnackbar(R.string.remote_save_failed)
        verify(activity).showSnackbar(R.string.password_unchanged)
    }

    @Test
    fun enablePushNotifications() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications()).thenReturn(Completable.complete())

        // Act
        subject.enablePushNotifications()

        // Assert
        verify(activity).setPushNotificationPref(true)
        verify(notificationTokenManager).enableNotifications()
        verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun disablePushNotifications() {
        // Arrange
        whenever(notificationTokenManager.disableNotifications()).thenReturn(Completable.complete())

        // Act
        subject.disablePushNotifications()

        // Assert
        verify(activity).setPushNotificationPref(false)
        verify(notificationTokenManager).disableNotifications()
        verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun updateEligibleLinkedBanks() {
        // Arrange
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, USD),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, EUR)
            )
        )
        arrangeBanks(
            listOf(
                LinkedPaymentMethod.Bank("", "", "", "", "", true, BankState.ACTIVE, USD),
                LinkedPaymentMethod.Bank("", "", "", "", "", false, BankState.ACTIVE, USD)
            )
        )

        // Act
        subject.updateBanks()

        // Assert
        argumentCaptor<List<BankItem>>().apply {
            verify(activity).updateLinkedBanks(capture())

            assertTrue(firstValue.first { it.bank.type == PaymentMethodType.BANK_TRANSFER }.canBeUsedToTransact)
            assertFalse(firstValue.first { it.bank.type == PaymentMethodType.BANK_ACCOUNT }.canBeUsedToTransact)
        }
    }

    @Test
    fun `updateEligibleLinkedBanks - no linkable banks`() {
        // Arrange
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, GBP),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, EUR)
            )
        )
        arrangeBanks(
            listOf(
                LinkedPaymentMethod.Bank("", "", "", "", "", true, BankState.ACTIVE, USD),
                LinkedPaymentMethod.Bank("", "", "", "", "", false, BankState.ACTIVE, USD)
            )
        )

        // Act
        subject.updateBanks()

        // Assert
        argumentCaptor<List<BankItem>>().apply {
            verify(activity).updateLinkedBanks(capture())

            assertFalse(firstValue.first { it.bank.type == PaymentMethodType.BANK_TRANSFER }.canBeUsedToTransact)
            assertFalse(firstValue.first { it.bank.type == PaymentMethodType.BANK_ACCOUNT }.canBeUsedToTransact)
        }
    }

    @Test
    fun `when user can add new card the add card view should be enabled`() {
        reset(activity)
        arrangeEligiblePaymentMethodTypes(USD, listOf(EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)), LinkAccess.GRANTED)

        subject.updateCanAddNewCard()

        verify(activity).addCardEnabled(true)
    }

    @Test
    fun `when user cannot add new card the add card view should be disabled`() {
        reset(activity)
        arrangeEligiblePaymentMethodTypes(USD, listOf(EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)), LinkAccess.BLOCKED)

        subject.updateCanAddNewCard()

        verify(activity, times(2)).addCardEnabled(false)
    }

    private fun arrangeEligiblePaymentMethodTypes(
        currency: FiatCurrency,
        eligiblePaymentMethodTypes: List<EligiblePaymentMethodType>,
        linkAccess: LinkAccess = LinkAccess.GRANTED
    ) {
        val limits = PaymentLimits(BigInteger.ZERO, BigInteger.ZERO, currency)
        whenever(
            getAvailablePaymentMethodsTypesUseCase.invoke(
                GetAvailablePaymentMethodsTypesUseCase.Request(
                    currency = currency,
                    onlyEligible = true,
                    fetchSddLimits = false
                )
            )
        ).thenReturn(
            Single.just(
                eligiblePaymentMethodTypes.map {
                    AvailablePaymentMethodType(true, linkAccess, it.currency, it.type, limits)
                }
            )
        )
    }

    private fun arrangeBanks(banks: List<LinkedPaymentMethod.Bank>) {
        whenever(paymentsDataManager.getLinkedBanks()).thenReturn(
            Single.just(banks)
        )
    }
}
