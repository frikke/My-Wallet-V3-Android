package piuk.blockchain.android.ui.kyc.navhost

import com.blockchain.analytics.Analytics
import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.api.kyc.data.store.KycStore
import com.blockchain.nabu.models.responses.nabu.Address
import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.ResubmissionResponse
import com.blockchain.nabu.models.responses.nabu.TierLevels
import com.blockchain.nabu.models.responses.nabu.UserState
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.getBlankNabuUser
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecisionKycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryPoint
import piuk.blockchain.android.ui.validOfflineToken

class KycNavHostPresenterTest {

    private lateinit var subject: KycNavHostPresenter
    private val view: KycNavHostView = mock()
    private val userService: UserService = mock()
    private val nabuToken: NabuToken = mock()
    private val analytics: Analytics = mock()
    private val reentryDecision: ReentryDecision = mock()
    private val kycStore: KycStore = mock()
    private val getUserStore: GetUserStore = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycNavHostPresenter(
            nabuToken = nabuToken,
            userService = userService,
            reentryDecision = reentryDecision,
            kycNavigator = ReentryDecisionKycNavigator(
                userService, reentryDecision, analytics
            ),
            kycStore = kycStore,
            getUserStore = getUserStore,
            analytics = mock(),
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady should invalidate stores`() {
        // Arrange - throw an exception just to have definitions,
        // won't be checked in this test
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.error { Throwable() })
        whenever(userService.getUser()).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(kycStore).invalidate()
        verify(getUserStore).invalidate()
    }

    @Test
    fun `onViewReady exception thrown`() {
        // Arrange
        whenever(userService.getUser()).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).showErrorSnackbarAndFinish(any())
    }

    @Test
    fun `onViewReady metadata found, empty user object`() {
        // Arrange
        whenever(userService.getUser()).thenReturn(Single.just(getBlankNabuUser()))

        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to country selection`() {
        // Arrange
        givenReentryDecision(ReentryPoint.CountrySelection)
        whenever(view.campaignType).thenReturn(CampaignType.Swap)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))
        whenever(userService.getUser())
            .thenReturn(
                Single.just(
                    NabuUser(
                        id = "id",
                        firstName = "FIRST_NAME",
                        lastName = "LAST_NAME",
                        email = "",
                        emailVerified = false,
                        dob = null,
                        mobile = null,
                        mobileVerified = false,
                        address = null,
                        state = UserState.Created,
                        kycState = KycState.None,
                        insertedAt = null,
                        updatedAt = null,
                        currencies = CurrenciesResponse(
                            preferredFiatTradingCurrency = "EUR",
                            usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                            defaultWalletCurrency = "BRL",
                            userFiatCurrencies = listOf("EUR", "GBP")
                        )
                    )
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigate(KycNavXmlDirections.actionStartCountrySelection())
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady resubmission campaign, should redirect to splash`() {
        // Arrange
        givenReentryDecision(ReentryPoint.CountrySelection)
        whenever(view.campaignType).thenReturn(CampaignType.Resubmission)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))
        whenever(userService.getUser())
            .thenReturn(
                Single.just(
                    NabuUser(
                        id = "id",
                        firstName = "FIRST_NAME",
                        lastName = "LAST_NAME",
                        email = "",
                        emailVerified = true,
                        dob = null,
                        mobile = null,
                        mobileVerified = false,
                        address = null,
                        state = UserState.Created,
                        kycState = KycState.UnderReview,
                        insertedAt = null,
                        updatedAt = null,
                        currencies = CurrenciesResponse(
                            preferredFiatTradingCurrency = "EUR",
                            usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                            defaultWalletCurrency = "BRL",
                            userFiatCurrencies = listOf("EUR", "GBP")
                        )
                    )
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToResubmissionSplash()
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady resubmission user, should redirect to splash`() {
        // Arrange
        givenReentryDecision(ReentryPoint.CountrySelection)
        whenever(view.campaignType).thenReturn(CampaignType.Swap)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))
        whenever(userService.getUser())
            .thenReturn(
                Single.just(
                    NabuUser(
                        id = "id",
                        firstName = "FIRST_NAME",
                        lastName = "LAST_NAME",
                        email = "",
                        emailVerified = true,
                        dob = null,
                        mobile = null,
                        mobileVerified = false,
                        address = null,
                        state = UserState.Created,
                        kycState = KycState.UnderReview,
                        insertedAt = null,
                        updatedAt = null,
                        resubmission = ResubmissionResponse(0, ""),
                        currencies = CurrenciesResponse(
                            preferredFiatTradingCurrency = "EUR",
                            usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                            defaultWalletCurrency = "BRL",
                            userFiatCurrencies = listOf("EUR", "GBP")
                        )
                    )
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToResubmissionSplash()
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to address`() {
        // Arrange
        givenReentryDecision(ReentryPoint.Address)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))

        val nabuUser = NabuUser(
            id = "id",
            firstName = "firstName",
            lastName = "lastName",
            email = "",
            emailVerified = false,
            dob = null,
            mobile = null,
            mobileVerified = false,
            address = getCompletedAddress(),
            state = UserState.Created,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null,
            currencies = CurrenciesResponse(
                preferredFiatTradingCurrency = "EUR",
                usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                defaultWalletCurrency = "BRL",
                userFiatCurrencies = listOf("EUR", "GBP")
            )
        )
        whenever(userService.getUser()).thenReturn(Single.just(nabuUser))

        // Act
        subject.onViewReady()

        // Assert
        verify(view).displayLoading(true)
        verify(view).navigate(
            KycNavXmlDirections.actionStartAutocompleteAddressEntry(nabuUser.toProfileModel())
        )
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to phone entry`() {
        // Arrange
        givenReentryDecision(ReentryPoint.MobileEntry)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))

        val nabuUser = NabuUser(
            id = "id",
            firstName = "firstName",
            lastName = "lastName",
            email = "",
            emailVerified = false,
            dob = null,
            mobile = "mobile",
            mobileVerified = false,
            address = getCompletedAddress(),
            state = UserState.Created,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null,
            currencies = CurrenciesResponse(
                preferredFiatTradingCurrency = "EUR",
                usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                defaultWalletCurrency = "BRL",
                userFiatCurrencies = listOf("EUR", "GBP")
            )
        )
        whenever(userService.getUser())
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigate(KycNavXmlDirections.actionStartMobileVerification("regionCode"))
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, when user is a tier 1, should not redirect to phone entry`() {
        // Arrange
        givenReentryDecision(ReentryPoint.MobileEntry)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))

        val nabuUser = NabuUser(
            id = "id",
            firstName = "firstName",
            lastName = "lastName",
            email = "",
            emailVerified = false,
            dob = null,
            mobile = "mobile",
            mobileVerified = false,
            address = getCompletedAddress(),
            state = UserState.Created,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null,
            tiers = TierLevels(current = 1, next = 2, selected = 2),
            currencies = CurrenciesResponse(
                preferredFiatTradingCurrency = "EUR",
                usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                defaultWalletCurrency = "BRL",
                userFiatCurrencies = listOf("EUR", "GBP")
            )
        )
        whenever(userService.getUser()).thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view, never()).navigate(any())
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to Onfido`() {
        // Arrange
        givenReentryDecision(ReentryPoint.Veriff)
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))

        val nabuUser = NabuUser(
            id = "id",
            firstName = "firstName",
            lastName = "lastName",
            email = "",
            emailVerified = false,
            mobile = "mobile",
            dob = null,
            mobileVerified = true,
            address = getCompletedAddress(),
            state = UserState.Active,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null,
            currencies = CurrenciesResponse(
                preferredFiatTradingCurrency = "EUR",
                usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                defaultWalletCurrency = "BRL",
                userFiatCurrencies = listOf("EUR", "GBP")
            )
        )
        whenever(userService.getUser())
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigate(KycNavXmlDirections.actionStartVeriff("regionCode"))
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to KYC status page`() {
        // Arrange
        whenever(nabuToken.fetchNabuToken()).thenReturn(Single.just(validOfflineToken))
        val nabuUser = NabuUser(
            id = "id",
            firstName = "firstName",
            lastName = "lastName",
            email = "",
            emailVerified = false,
            mobile = "mobile",
            dob = null,
            mobileVerified = true,
            address = getCompletedAddress(),
            state = UserState.Active,
            kycState = KycState.Pending,
            insertedAt = null,
            updatedAt = null,
            currencies = CurrenciesResponse(
                preferredFiatTradingCurrency = "EUR",
                usableFiatCurrencies = listOf("EUR", "USD", "GBP", "ARS"),
                defaultWalletCurrency = "BRL",
                userFiatCurrencies = listOf("EUR", "GBP")
            )
        )
        whenever(userService.getUser()).thenReturn(Single.just(nabuUser))

        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
    }

    private fun getCompletedAddress(): Address = Address(
        city = "city",
        line1 = "line1",
        line2 = "line2",
        state = "state",
        countryCode = "regionCode",
        postCode = "postCode"
    )

    private fun givenReentryDecision(reentryPoint: ReentryPoint) {
        whenever(reentryDecision.findReentryPoint(any())).thenReturn(Single.just(reentryPoint))
    }
}
