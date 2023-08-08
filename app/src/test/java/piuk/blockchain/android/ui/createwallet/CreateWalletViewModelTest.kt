package piuk.blockchain.android.ui.createwallet

import app.cash.turbine.test
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.referral.ReferralService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.wallet.DefaultLabels
import com.google.android.gms.recaptcha.RecaptchaActionType
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.util.PasswordUtil
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkStatic
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker

class CreateWalletViewModelTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val environmentConfig: EnvironmentConfig = mockk()
    private val defaultLabels: DefaultLabels = mockk()
    private val authPrefs: AuthPrefs = mockk(relaxed = true)
    private val walletStatusPrefs: WalletStatusPrefs = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)
    private val specificAnalytics: ProviderSpecificAnalytics = mockk(relaxed = true)
    private val appUtil: AppUtil = mockk(relaxed = true)
    private val formatChecker: FormatChecker = mockk()
    private val eligibilityService: EligibilityService = mockk {
        coEvery { getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
    }
    private val referralService: ReferralService = mockk()
    private val payloadDataManager: PayloadDataManager = mockk()
    private val nabuUserDataManager: NabuUserDataManager = mockk {
        coEvery { getUserGeolocation() } returns Outcome.Success(USER_LOCATION)
    }

    private lateinit var subject: CreateWalletViewModel

    @Before
    fun setUp() {
        subject = CreateWalletViewModel(
            environmentConfig = environmentConfig,
            defaultLabels = defaultLabels,
            authPrefs = authPrefs,
            walletStatusPrefs = walletStatusPrefs,
            analytics = analytics,
            specificAnalytics = specificAnalytics,
            appUtil = appUtil,
            formatChecker = formatChecker,
            eligibilityService = eligibilityService,
            referralService = referralService,
            payloadDataManager = payloadDataManager,
            nabuUserDataManager = nabuUserDataManager
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(PasswordUtil::class)
    }

    @Test
    fun `onViewCreated should fetch countries and geolocation and show them if successful`() = runTest {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
        coEvery { nabuUserDataManager.getUserGeolocation() } returns Outcome.Success(USER_LOCATION)

        subject.viewState.test {
            subject.viewCreated(ModelConfigArgs.NoArgs)
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loading
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, null, COUNTRY_UK)
            expectNoEvents()
        }

        coVerify { eligibilityService.getCountriesList(GetRegionScope.Signup) }
        coVerify { nabuUserDataManager.getUserGeolocation() }
    }

    @Test
    fun `onViewCreated should fetch countries and show error on failure`() = runTest {
        val error = RuntimeException("error")
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Failure(error)

        subject.viewState.test {
            subject.viewCreated(ModelConfigArgs.NoArgs)
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loading
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(emptyList(), null, null)
                this.error shouldBeEqualTo CreateWalletError.Unknown("error")
            }
            expectNoEvents()
        }
    }

    @Test
    fun `onViewCreated user geolocation error should not affect the flow`() = runTest {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
        coEvery { nabuUserDataManager.getUserGeolocation() } returns Outcome.Failure(RuntimeException("error"))

        subject.viewState.test {
            subject.viewCreated(ModelConfigArgs.NoArgs)
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loading
            awaitItem().apply {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, null, null)
                error shouldBeEqualTo null
            }
            expectNoEvents()
        }

        coVerify { eligibilityService.getCountriesList(GetRegionScope.Signup) }
        coVerify { nabuUserDataManager.getUserGeolocation() }
    }

    @Test
    fun `selecting country should update the selected country and not fetch states if the country does not have states`() = runTest {
        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.CountryInputChanged("UK"))
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, COUNTRY_UK, COUNTRY_UK)
                stateInputState shouldBeEqualTo StateInputState.Hidden
            }
        }

        coVerify { eligibilityService.getStatesList(any(), any()) wasNot Called }
    }

    @Test
    fun `selecting country with states should fetch states`() = runTest {
        coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } returns Outcome.Success(STATES)

        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.CountryInputChanged("US"))
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, COUNTRY_US, COUNTRY_UK)
                stateInputState shouldBeEqualTo StateInputState.Loading
            }
            awaitItem().run {
                stateInputState shouldBeEqualTo StateInputState.Loaded(STATES, null)
            }
        }

        coVerify { eligibilityService.getStatesList("US", GetRegionScope.Signup) }
    }

    @Test
    fun `selecting country should cancel previous states fetching`() = runTest {
        val statesDeferred = CompletableDeferred<Unit>()
        coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } coAnswers {
            statesDeferred.await()
            Outcome.Success(STATES)
        }

        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.CountryInputChanged("US"))
            awaitItem() // States Loading
            subject.onIntent(CreateWalletIntent.CountryInputChanged("UK"))
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, COUNTRY_UK, COUNTRY_UK)
                stateInputState shouldBeEqualTo StateInputState.Hidden
            }
            statesDeferred.complete(Unit) // states finished loading
            expectNoEvents() // job should have been cancelled
        }

        coVerify { eligibilityService.getStatesList("US", GetRegionScope.Signup) }
    }

    @Test
    fun `whenever an input change the next button state should be recomputed and display the correct state`() =
        runTest {
            every { formatChecker.isValidEmailAddress(any()) } returns true
            coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } returns Outcome.Success(STATES)

            subject.viewState.test {
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled
                subject.viewCreated(ModelConfigArgs.NoArgs)
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled

                subject.onIntent(CreateWalletIntent.CountryInputChanged(COUNTRY_US.countryCode))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled // States Loading
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled // States Loaded
                subject.onIntent(CreateWalletIntent.StateInputChanged(STATE_AK.stateCode))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Enabled
                subject.onIntent(CreateWalletIntent.ReferralInputChanged("123"))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled
                subject.onIntent(CreateWalletIntent.ReferralInputChanged("12345678"))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Enabled
                subject.onIntent(CreateWalletIntent.ReferralInputChanged(""))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Enabled
                subject.onIntent(CreateWalletIntent.RegionNextClicked)
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled

                subject.onIntent(CreateWalletIntent.EmailInputChanged("bla@something.com"))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled
                subject.onIntent(CreateWalletIntent.PasswordInputChanged("1234"))
                awaitItem().nextButtonState shouldBeEqualTo ButtonState.Disabled
            }
        }

    @Test
    fun `clicking next should validate email and show error if invalid`() = runTest {
        every { formatChecker.isValidEmailAddress(any()) } returns false

        populateEmailAndPasswordNextEnabledState()
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.EmailPasswordNextClicked)
            awaitItem().isShowingInvalidEmailError shouldBeEqualTo true
        }
        verify { formatChecker.isValidEmailAddress("bla@something.com") }
    }

    @Test
    fun `typing a password should check if the password is valid`() = runTest {
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1"))
            awaitItem().passwordInputErrors.containsAll(
                listOf(
                    CreateWalletPasswordError.InvalidPasswordTooShort,
                    CreateWalletPasswordError.InvalidPasswordNoLowerCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoUpperCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound
                )
            ) shouldBeEqualTo true

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1".repeat(256)))
            awaitItem().passwordInputErrors.containsAll(
                listOf(
                    CreateWalletPasswordError.InvalidPasswordTooLong,
                    CreateWalletPasswordError.InvalidPasswordNoLowerCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoUpperCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound
                )
            ) shouldBeEqualTo true

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("12345678"))
            awaitItem().passwordInputErrors.containsAll(
                listOf(
                    CreateWalletPasswordError.InvalidPasswordNoLowerCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoUpperCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound
                )
            ) shouldBeEqualTo true

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("12345678a"))
            awaitItem().passwordInputErrors.containsAll(
                listOf(
                    CreateWalletPasswordError.InvalidPasswordNoUpperCaseFound,
                    CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound
                )
            ) shouldBeEqualTo true

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("12345678aA"))
            awaitItem().passwordInputErrors.containsAll(
                listOf(
                    CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound
                )
            ) shouldBeEqualTo true

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("12345678aA!"))
            awaitItem().passwordInputErrors.isEmpty() shouldBeEqualTo true
        }
    }

    @Test
    fun `clicking next with valid inputs and should try to verify captcha`() =
        runTest {
            populateValidInputsState()
            coEvery { referralService.isReferralCodeValid("12345678") } returns Outcome.Success(true)

            subject.navigationEventFlow.test {
                subject.onIntent(CreateWalletIntent.EmailPasswordNextClicked)
                awaitItem() shouldBeEqualTo CreateWalletNavigation.RecaptchaVerification(RecaptchaActionType.SIGNUP)
                expectNoEvents()
            }
        }

    @Test
    fun `if captcha verification fails should show error`() =
        runTest {
            populateValidInputsState()
            val error = Exception("error")
            val recaptcha = "CAPTCHA"
            coEvery { referralService.isReferralCodeValid("12345678") } returns Outcome.Success(true)

            subject.viewState.test {
                expectMostRecentItem()
                subject.onIntent(CreateWalletIntent.RecaptchaVerificationFailed(error))
                awaitItem().run {
                    this.error shouldBeEqualTo CreateWalletError.RecaptchaFailed
                }
                expectNoEvents()
            }
        }

    @Test
    fun `recaptcha succeeded with valid inputs and valid referral code should try to create hd wallet and show error if fails`() =
        runTest {
            populateValidInputsState()
            val error = Exception("error")
            val recaptcha = "CAPTCHA"
            coEvery { referralService.isReferralCodeValid("12345678") } returns Outcome.Success(true)
            every { defaultLabels.getDefaultNonCustodialWalletLabel() } returns "default_nc_name"
            every {
                payloadDataManager.createHdWallet(
                    "123456789Aa!",
                    "default_nc_name",
                    "bla@something.com",
                    recaptcha
                )
            } returns Single.error(error)

            subject.viewState.test {
                expectMostRecentItem()
                subject.onIntent(CreateWalletIntent.RecaptchaVerificationSucceeded(recaptcha))
                verify {
                    payloadDataManager.createHdWallet(
                        "123456789Aa!",
                        "default_nc_name",
                        "bla@something.com",
                        recaptcha
                    )
                }
                verify { appUtil.clearCredentials() }
                awaitItem().run {
                    isCreateWalletLoading shouldBeEqualTo false
                    this.screen shouldBeEqualTo CreateWalletScreen.CREATION_FAILED
                }
                expectNoEvents()
            }
        }

    @Test
    fun `recaptcha succeeded with valid inputs and valid referral code should try to create hd wallet and navigate to PIN if successful`() =
        runTest {
            populateValidInputsState()
            val wallet: Wallet = mockk {
                every { guid } returns "guid"
                every { sharedKey } returns "sharedKey"
            }
            val recaptcha = "CAPTCHA"
            every { defaultLabels.getDefaultNonCustodialWalletLabel() } returns "default_nc_name"
            every {
                payloadDataManager.createHdWallet(
                    "123456789Aa!",
                    "default_nc_name",
                    "bla@something.com",
                    recaptcha
                )
            } returns Single.just(wallet)

            subject.navigationEventFlow.test {
                subject.onIntent(CreateWalletIntent.RecaptchaVerificationSucceeded(recaptcha))
                verify {
                    payloadDataManager.createHdWallet(
                        "123456789Aa!",
                        "default_nc_name",
                        "bla@something.com",
                        recaptcha
                    )
                }
                verify { walletStatusPrefs.isNewlyCreated = true }
                verify { authPrefs.walletGuid = "guid" }
                verify { authPrefs.sharedKey = "sharedKey" }
                verify { walletStatusPrefs.countrySelectedOnSignUp = "US" }
                verify { walletStatusPrefs.stateSelectedOnSignUp = "AK" }
                verify { walletStatusPrefs.email = "bla@something.com" }
                awaitItem() shouldBeEqualTo CreateWalletNavigation.PinEntry("12345678")
                expectNoEvents()
            }
        }

    private fun populateEmailAndPasswordNextEnabledState() {
        advanceToEmailAndPasswordStep()
        subject.onIntent(CreateWalletIntent.EmailInputChanged("bla@something.com"))
        subject.onIntent(CreateWalletIntent.PasswordInputChanged("123456789Aa!"))
        subject.onIntent(CreateWalletIntent.ConfirmPasswordInputChanged("123456789Aa!"))
    }

    private fun populateValidInputsState() {
        every { formatChecker.isValidEmailAddress(any()) } returns true
        every { environmentConfig.isRunningInDebugMode() } returns false
        populateEmailAndPasswordNextEnabledState()
    }

    private fun advanceToEmailAndPasswordStep() {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
        coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } returns Outcome.Success(STATES)
        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.onIntent(CreateWalletIntent.CountryInputChanged(COUNTRY_US.countryCode))
        subject.onIntent(CreateWalletIntent.StateInputChanged(STATE_AK.stateCode))
        coEvery { referralService.isReferralCodeValid("12345678") } returns Outcome.Success(true)
        subject.onIntent(CreateWalletIntent.ReferralInputChanged("12345678"))
        subject.onIntent(CreateWalletIntent.RegionNextClicked)
    }

    companion object {
        private val COUNTRY_US = Region.Country("US", "United States", true, listOf("AK", "MH")).localise()
        private val COUNTRY_UK = Region.Country("UK", "United Kingdom", true, emptyList()).localise()
        private val COUNTRY_PT = Region.Country("PT", "Portugal", true, emptyList()).localise()
        private val COUNTRIES = listOf(
            COUNTRY_US,
            COUNTRY_UK,
            COUNTRY_PT
        )
        private val USER_LOCATION: CountryIso = COUNTRY_UK.countryCode

        private val STATE_AK = Region.State("US", "Arkansas", true, "AK")
        private val STATE_MH = Region.State("US", "Michigan", true, "MH")
        private val STATES = listOf(
            STATE_AK,
            STATE_MH
        )

        private fun Region.Country.localise(): Region.Country = let {
            val locale = Locale("", it.countryCode)
            it.copy(name = locale.displayName)
        }
    }
}
