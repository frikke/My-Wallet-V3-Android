package piuk.blockchain.android.ui.createwallet

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.android.testutils.rxInit
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Single
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class CreateWalletPresenterTest {

    private lateinit var subject: CreateWalletPresenter
    private val view: CreateWalletView = mock()
    private val appUtil: AppUtil = mock()
    private val payloadDataManager: PayloadDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val prefsUtil: PersistentPrefs = mock()
    private val analytics: Analytics = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val formatChecker: FormatChecker = mock()
    private val eligibilityService: EligibilityService = mock()
    private val referralInteractor: ReferralInteractor = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
    }

    @Before
    fun setUp() {
        subject = CreateWalletPresenter(
            payloadDataManager = payloadDataManager,
            prefs = prefsUtil,
            appUtil = appUtil,
            specificAnalytics = mock(),
            analytics = analytics,
            environmentConfig = environmentConfig,
            formatChecker = formatChecker,
            eligibilityService = eligibilityService,
            referralInteractor = referralInteractor
        )
        subject.initView(view)

        whenever(referralInteractor.validateReferralIfNeeded(any()))
            .doReturn(Single.just(ReferralCodeState.NOT_AVAILABLE))

        val eligibleCountries: List<CountryIso> = listOf("US", "UK", "PT", "DE", "NL")
        whenever(eligibilityService.getCustodialEligibleCountries()).thenReturn(Single.just(eligibleCountries))
    }

    @Test
    fun onViewReady() {
        val eligibleCountries: List<CountryIso> = listOf("US", "UK", "PT", "DE", "NL")
        whenever(eligibilityService.getCustodialEligibleCountries()).thenReturn(Single.just(eligibleCountries))
        subject.onViewReady()

        verify(eligibilityService).getCustodialEligibleCountries()
    }

    @Test
    fun `create wallet`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val accountName = "AccountName"
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val recoveryPhrase = ""
        val wallet: Wallet = mock {
            on { this.guid }.thenReturn(guid)
            on { this.sharedKey }.thenReturn(sharedKey)
        }

        whenever(view.getDefaultAccountName()).thenReturn(accountName)
        whenever(payloadDataManager.createHdWallet(pw1, accountName, email)).thenReturn(
            Single.just(wallet)
        )

        // Act
        subject.createOrRestoreWallet(email, pw1, recoveryPhrase, "", "", "")
        // Assert
        val observer = payloadDataManager.createHdWallet(pw1, accountName, email).test()
        observer.assertComplete()
        observer.assertNoErrors()

        verify(view).showProgressDialog(any())
        verify(prefsUtil).email = email
        verify(prefsUtil).walletGuid = guid
        verify(prefsUtil).sharedKey = sharedKey
        verify(prefsUtil).isNewlyCreated = true
        verify(view).startPinEntryActivity("")
        verify(view).dismissProgressDialog()
        verify(analytics).logEvent(AnalyticsEvents.WalletCreation)
    }

    @Test
    fun `create wallet invalid referral`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val accountName = "AccountName"
        val recoveryPhrase = ""
        val referral = "invalid"

        whenever(view.getDefaultAccountName()).thenReturn(accountName)
        whenever(referralInteractor.validateReferralIfNeeded(referral)).doReturn(Single.just(ReferralCodeState.INVALID))

        // Act
        subject.createOrRestoreWallet(email, pw1, recoveryPhrase, "", "", referral)
        // Assert
        val observer = payloadDataManager.createHdWallet(pw1, accountName, email).test()
        observer.assertComplete()
        observer.assertNoErrors()

        verifyNoMoreInteractions(prefsUtil)
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).showReferralInvalidMessage()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `create wallet referral api error`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val accountName = "AccountName"
        val recoveryPhrase = ""
        val referral = "valid"

        whenever(view.getDefaultAccountName()).thenReturn(accountName)
        whenever(referralInteractor.validateReferralIfNeeded(referral)).doReturn(Single.error(Throwable()))

        // Act
        subject.createOrRestoreWallet(email, pw1, recoveryPhrase, "", "", referral)
        // Assert
        val observer = payloadDataManager.createHdWallet(pw1, accountName, email).test()
        observer.assertComplete()
        observer.assertNoErrors()

        verifyNoMoreInteractions(prefsUtil)
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).showReferralInvalidMessage()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `restore wallet`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val accountName = "AccountName"
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val recoveryPhrase = "all all all all all all all all all all all all"
        val wallet: Wallet = mock {
            on { this.guid }.thenReturn(guid)
            on { this.sharedKey }.thenReturn(sharedKey)
        }
        whenever(view.getDefaultAccountName()).thenReturn(accountName)
        whenever(payloadDataManager.restoreHdWallet(recoveryPhrase, accountName, email, pw1))
            .thenReturn(Single.just(wallet))

        // Act
        subject.createOrRestoreWallet(email, pw1, recoveryPhrase, "", "", "")

        // Assert
        val observer = payloadDataManager.restoreHdWallet(email, pw1, accountName, recoveryPhrase)
            .test()
        observer.assertComplete()
        observer.assertNoErrors()

        verify(view).showProgressDialog(any())
        verify(prefsUtil).email = email
        verify(prefsUtil).walletGuid = guid
        verify(prefsUtil).sharedKey = sharedKey
        verify(prefsUtil).isNewlyCreated = true
        verify(view).startPinEntryActivity(null)
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `validateReferralIfNeeded empty`() {
        // Arrange
        val referralCode = ""

        // Act
        val result = subject.validateReferralFormat(referralCode)

        // Assert
        assertTrue(result)
        verify(view).hideReferralInvalidMessage()
    }

    @Test
    fun `validateReferralIfNeeded valid`() {
        // Arrange
        val referralCode = "ABCD1234"

        // Act
        val result = subject.validateReferralFormat(referralCode)

        // Assert
        assertTrue(result)
        verify(view).hideReferralInvalidMessage()
    }

    @Test
    fun `validateReferralFormat invalid`() {
        // Arrange
        val referralCode = "SNOW123"

        // Act
        val result = subject.validateReferralFormat(referralCode)

        // Assert
        assertFalse(result)
        verify(view).showReferralInvalidMessage()
    }

    @Test
    fun `validateCredentials are valid`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val pw2 = "MyTestWallet"
        whenever(formatChecker.isValidEmailAddress(anyString())).thenReturn(true)
        // Act
        val result = subject.validateCredentials(email, pw1, pw2)
        // Assert
        assert(result)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials invalid email`() {
        val pw1 = "MyTestWallet"
        // Arrange
        whenever(formatChecker.isValidEmailAddress(anyString())).thenReturn(false)
        // Act
        val result = subject.validateCredentials("john", pw1, pw1)
        // Assert
        assert(!result)
        verify(view).showError(R.string.invalid_email)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials short password`() {
        val pw1 = "aaa"
        // Arrange
        whenever(formatChecker.isValidEmailAddress(anyString())).thenReturn(true)
        // Act
        val result = subject.validateCredentials("john@snow.com", pw1, pw1)
        // Assert
        assert(!result)
        verify(view).showError(R.string.invalid_password_too_short)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials password mismatch`() {
        val pw1 = "MyTestWallet"
        // Arrange
        whenever(formatChecker.isValidEmailAddress(anyString())).thenReturn(true)
        // Act
        val result = subject.validateCredentials("john@snow.com", pw1, "MyTestWallet2")
        // Assert
        assert(!result)
        verify(view).showError(R.string.password_mismatch_error)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials weak password running on non debug mode`() {
        val pw1 = "aaaaaa"
        // Arrange
        whenever(formatChecker.isValidEmailAddress(anyString())).thenReturn(true)
        whenever(environmentConfig.isRunningInDebugMode()).thenReturn(false)
        // Act
        val result = subject.validateCredentials("john@snow.com", pw1, pw1)
        // Assert
        assert(!result)
        verify(view).warnWeakPassword(any(), any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials weak password running on debug mode`() {
        val pw1 = "aaaaaa"
        // Arrange
        whenever(formatChecker.isValidEmailAddress(anyString())).thenReturn(true)
        whenever(environmentConfig.isRunningInDebugMode()).thenReturn(true)
        // Act
        val result = subject.validateCredentials("john@snow.com", pw1, pw1)
        // Assert
        assert(result)
        verifyZeroInteractions(view)
    }

    @Test
    fun `validateGeolocation country != US is selected`() {
        val result = subject.validateGeoLocation("DE")

        assert(result)
        verifyZeroInteractions(view)
    }

    @Test
    fun `validateGeolocation country not selected`() {
        val result = subject.validateGeoLocation()

        assert(!result)
        verify(view).showError(R.string.country_not_selected)
        verifyZeroInteractions(view)
    }

    @Test
    fun `validateGeolocation country == US is selected and state is not selected`() {
        val result = subject.validateGeoLocation("US")

        assert(!result)
        verify(view).showError(R.string.state_not_selected)
        verifyZeroInteractions(view)
    }

    @Test
    fun `on fetch eligible countries success and set these countries`() {
        val eligibleCountries: List<CountryIso> = listOf("US", "UK", "PT", "DE", "NL")
        whenever(eligibilityService.getCustodialEligibleCountries()).thenReturn(Single.just(eligibleCountries))
        subject.onViewReady()

        verify(eligibilityService).getCustodialEligibleCountries()
        verify(view).setEligibleCountries(eligibleCountries)
    }

    @Test
    fun `on fetch eligible countries failure should fallback to Locale`() {
        val error = IllegalStateException("error")
        whenever(eligibilityService.getCustodialEligibleCountries()).thenReturn(Single.error(error))
        subject.onViewReady()

        verify(eligibilityService).getCustodialEligibleCountries()
        verify(view).setEligibleCountries(Locale.getISOCountries().toList())
    }
}
