package piuk.blockchain.android.ui.createwallet

import app.cash.turbine.test
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibilityError
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.referral.ReferralService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.wallet.DefaultLabels
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.util.PasswordUtil
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class CreateWalletViewModelTest {
//    @get:Rule
//    val rule = InstantTaskExecutorRule()

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
    private val eligibilityService: EligibilityService = mockk()
    private val referralService: ReferralService = mockk()
    private val payloadDataManager: PayloadDataManager = mockk()

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
        )
    }

    @Test
    fun `onViewCreated should fetch countries and show them if successful`() = runTest {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)

        subject.viewState.test {
            subject.viewCreated(ModelConfigArgs.NoArgs)
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loading
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, null)
            expectNoEvents()
        }

        coVerify { eligibilityService.getCountriesList(GetRegionScope.Signup) }
    }

    @Test
    fun `onViewCreated should fetch countries and show error on failure`() = runTest {
        val error = EligibilityError.RequestFailed("error")
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Failure(error)

        subject.viewState.test {
            subject.viewCreated(ModelConfigArgs.NoArgs)
            awaitItem().countryInputState shouldBeEqualTo CountryInputState.Loading
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(emptyList(), null)
                this.error shouldBeEqualTo CreateWalletError.Unknown("error")
            }
            expectNoEvents()
        }
    }

    @Test
    fun `selecting country should update the selected country and not fetch states if the country does not have states`() = runTest {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)

        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.CountryInputChanged("UK"))
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, COUNTRY_UK)
                stateInputState shouldBeEqualTo StateInputState.Hidden
            }
        }

        coVerify { eligibilityService.getStatesList(any(), any()) wasNot Called }
    }

    @Test
    fun `selecting country with states should fetch states`() = runTest {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
        coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } returns Outcome.Success(STATES)

        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.CountryInputChanged("US"))
            awaitItem().run {
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, COUNTRY_US)
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
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
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
                countryInputState shouldBeEqualTo CountryInputState.Loaded(COUNTRIES, COUNTRY_UK)
                stateInputState shouldBeEqualTo StateInputState.Hidden
            }
            statesDeferred.complete(Unit) // states finished loading
            expectNoEvents() // job should have been cancelled
        }

        coVerify { eligibilityService.getStatesList("US", GetRegionScope.Signup) }
    }

    @Test
    fun `whenever an input change the next button state should be recomputed and display the correct state`() = runTest {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
        coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } returns Outcome.Success(STATES)
        subject.viewState.test {
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.viewCreated(ModelConfigArgs.NoArgs)
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.EmailInputChanged("bla@something.com"))
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1234"))
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("1234"))
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.TermsOfServiceStateChanged(true))
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.CountryInputChanged("US"))
            awaitItem().isNextEnabled shouldBeEqualTo false // States Loading
            awaitItem().isNextEnabled shouldBeEqualTo false // States Loaded
            subject.onIntent(CreateWalletIntent.StateInputChanged("AK"))
            awaitItem().isNextEnabled shouldBeEqualTo true
            subject.onIntent(CreateWalletIntent.ReferralInputChanged("1234567"))
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.ReferralInputChanged("12345678"))
            awaitItem().isNextEnabled shouldBeEqualTo true
            subject.onIntent(CreateWalletIntent.CountryInputChanged("UK"))
            awaitItem().isNextEnabled shouldBeEqualTo true
            subject.onIntent(CreateWalletIntent.PasswordInputChanged("123"))
            awaitItem().isNextEnabled shouldBeEqualTo false
            subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("123"))
            awaitItem().isNextEnabled shouldBeEqualTo true
        }
    }

    @Test
    fun `clicking next should validate email and show error if invalid`() = runTest {
        every { formatChecker.isValidEmailAddress(any()) } returns false

        populateNextEnabledState()
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            awaitItem().error shouldBeEqualTo CreateWalletError.InvalidEmail
        }
        verify { formatChecker.isValidEmailAddress("bla@something.com") }
    }

    @Test
    fun `clicking next should validate password and show error if invalid`() = runTest {
        every { formatChecker.isValidEmailAddress(any()) } returns true
        populateNextEnabledState()
        subject.viewState.test {
            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1"))
            subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("1"))
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            awaitItem().error shouldBeEqualTo CreateWalletError.InvalidPasswordTooShort

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1".repeat(256)))
            subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("1".repeat(256)))
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            awaitItem().error shouldBeEqualTo CreateWalletError.InvalidPasswordTooLong

            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1234"))
            subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("4321"))
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            awaitItem().error shouldBeEqualTo CreateWalletError.PasswordsMismatch

            mockkStatic(PasswordUtil::class)
            every { environmentConfig.isRunningInDebugMode() } returns false
            every { PasswordUtil.getStrength(any()) } returns 49.0
            subject.onIntent(CreateWalletIntent.PasswordInputChanged("1234"))
            subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("1234"))
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            awaitItem().error shouldBeEqualTo CreateWalletError.InvalidPasswordTooWeak
            verify { PasswordUtil.getStrength("1234") }
            unmockkStatic(PasswordUtil::class)

            expectNoEvents()
        }
    }

    @Test
    fun `clicking next with valid inputs should check referral code validity if filled in and show error if it fails and clear error if referral input is changed`() = runTest {
        populateValidInputsState()
        val error = Exception("error")
        coEvery { referralService.isReferralCodeValid("12345678") } coAnswers {
            yield()
            Outcome.Failure(error)
        }

        subject.onIntent(CreateWalletIntent.ReferralInputChanged("12345678"))
        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            awaitItem().isCreateWalletLoading shouldBeEqualTo true
            coVerify { referralService.isReferralCodeValid("12345678") }
            awaitItem().run {
                isCreateWalletLoading shouldBeEqualTo false
                isInvalidReferralErrorShowing shouldBeEqualTo true
            }
            expectNoEvents()
            subject.onIntent(CreateWalletIntent.ReferralInputChanged("87654321"))
            awaitItem().isInvalidReferralErrorShowing shouldBeEqualTo false
        }
    }

    @Test
    fun `clicking next with valid inputs and valid referral code should try to create hd wallet and show error if fails`() = runTest {
        populateValidInputsState()
        val error = Exception("error")
        coEvery { referralService.isReferralCodeValid("12345678") } returns Outcome.Success(true)
        every { defaultLabels.getDefaultNonCustodialWalletLabel() } returns "default_nc_name"
        every {
            payloadDataManager.createHdWallet("1234", "default_nc_name", "bla@something.com")
        } returns Single.error(error)

        subject.viewState.test {
            expectMostRecentItem()
            subject.onIntent(CreateWalletIntent.NextClicked)
            verify { payloadDataManager.createHdWallet("1234", "default_nc_name", "bla@something.com") }
            verify { appUtil.clearCredentialsAndRestart() }
            awaitItem().run {
                isCreateWalletLoading shouldBeEqualTo false
                this.error shouldBeEqualTo CreateWalletError.WalletCreationFailed
            }
            expectNoEvents()
        }
    }

    @Test
    fun `clicking next with valid inputs and valid referral code should try to create hd wallet and navigate to PIN if successful`() = runTest {
        populateValidInputsState()
        val wallet: Wallet = mockk {
            every { guid } returns "guid"
            every { sharedKey } returns "sharedKey"
        }
        coEvery { referralService.isReferralCodeValid("12345678") } returns Outcome.Success(true)
        every { defaultLabels.getDefaultNonCustodialWalletLabel() } returns "default_nc_name"
        every {
            payloadDataManager.createHdWallet("1234", "default_nc_name", "bla@something.com")
        } returns Single.just(wallet)

        subject.navigationEventFlow.test {
            subject.onIntent(CreateWalletIntent.ReferralInputChanged("12345678"))
            subject.onIntent(CreateWalletIntent.NextClicked)
            verify { payloadDataManager.createHdWallet("1234", "default_nc_name", "bla@something.com") }
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

    private fun populateNextEnabledState() {
        coEvery { eligibilityService.getCountriesList(GetRegionScope.Signup) } returns Outcome.Success(COUNTRIES)
        coEvery { eligibilityService.getStatesList("US", GetRegionScope.Signup) } returns Outcome.Success(STATES)
        subject.viewCreated(ModelConfigArgs.NoArgs)
        subject.onIntent(CreateWalletIntent.EmailInputChanged("bla@something.com"))
        subject.onIntent(CreateWalletIntent.PasswordInputChanged("1234"))
        subject.onIntent(CreateWalletIntent.PasswordConfirmationInputChanged("1234"))
        subject.onIntent(CreateWalletIntent.TermsOfServiceStateChanged(true))
        subject.onIntent(CreateWalletIntent.CountryInputChanged("US"))
        subject.onIntent(CreateWalletIntent.StateInputChanged("AK"))
    }

    private fun populateValidInputsState() {
        every { formatChecker.isValidEmailAddress(any()) } returns true
        mockkStatic(PasswordUtil::class)
        every { environmentConfig.isRunningInDebugMode() } returns false
        every { PasswordUtil.getStrength(any()) } returns 50.0
        populateNextEnabledState()
    }

    companion object {
        private val COUNTRY_US = Region.Country("US", "United States", true, listOf("AK", "MH")).localise()
        private val COUNTRY_UK = Region.Country("UK", "United Kingdom", true, emptyList()).localise()
        private val COUNTRY_PT = Region.Country("PT", "Portugal", true, emptyList()).localise()
        private val COUNTRIES = listOf(
            COUNTRY_US,
            COUNTRY_UK,
            COUNTRY_PT,
        )

        private val STATE_AK = Region.State("US", "Arkansas", true, "AK")
        private val STATE_MH = Region.State("US", "Michigan", true, "MH")
        private val STATES = listOf(
            STATE_AK,
            STATE_MH,
        )

        private fun Region.Country.localise(): Region.Country = let {
            val locale = Locale("", it.countryCode)
            it.copy(name = locale.displayName)
        }
    }
}
