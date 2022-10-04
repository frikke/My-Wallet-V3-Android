package piuk.blockchain.android.ui.kyc.profile

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.util.toISO8601DateString
import com.blockchain.testutils.date
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.amshove.kluent.`should throw`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.util.StringUtils
import retrofit2.HttpException
import retrofit2.Response

class KycProfilePresenterTest {

    private lateinit var subject: KycProfilePresenter
    private val view: KycProfileView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val userService: UserService = mock()
    private val getUserStore: GetUserStore = mock()
    private val stringUtils: StringUtils = mock()
    private val loqateFeatureFlag: FeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(true))
    }

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycProfilePresenter(
            nabuDataManager,
            userService,
            getUserStore,
            stringUtils,
            loqateFeatureFlag,
        )
        whenever(stringUtils.getString(any())).thenReturn("")
        subject.initView(view)
    }

    @Test
    fun `firstName set but other values not, should disable button`() {
        subject.firstNameSet = true

        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `firstName and lastName set but DoB not, should disable button`() {
        subject.firstNameSet = true
        subject.lastNameSet = true

        verify(view, times(2)).setButtonEnabled(false)
    }

    @Test
    fun `all values set, should enable button`() {
        subject.firstNameSet = true
        subject.lastNameSet = true
        subject.dateSet = true

        verify(view, times(2)).setButtonEnabled(false)
        verify(view).setButtonEnabled(true)
    }

    @Test
    fun `on continue clicked firstName empty should throw IllegalStateException`() {
        whenever(view.firstName).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked lastName empty should throw IllegalStateException`() {
        whenever(view.firstName).thenReturn("Adam")
        whenever(view.lastName).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked date of birth null should throw IllegalStateException`() {
        whenever(view.firstName).thenReturn("Adam")
        whenever(view.lastName).thenReturn("Bennett")
        whenever(view.dateOfBirth).thenReturn(null);

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked all data correct, metadata fetch success, loqate ON`() {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        whenever(loqateFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
            )
        ).thenReturn(Completable.complete())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).navigateToAddressVerification(any())
        verify(getUserStore).markAsStale()
    }

    @Test
    fun `on continue clicked all data correct, metadata fetch success, loqate OFF`() {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        whenever(loqateFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
            )
        ).thenReturn(Completable.complete())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).navigateToOldAddressVerification(any())
        verify(getUserStore).markAsStale()
    }

    @Test
    fun `on continue clicked all data correct, user conflict`() {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)

        val responseBody =
            ResponseBody.create(
                ("application/json").toMediaTypeOrNull(),
                "{}"
            )
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
            )
        ).thenReturn(
            Completable.error {
                NabuApiExceptionFactory.fromResponseBody(
                    HttpException(Response.error<Unit>(409, responseBody))
                )
            }
        )
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorSnackbar(any())
    }

    @Test
    fun `onViewReady restores data to the UI`() {
        // Arrange
        val nabuUser = NabuUser(
            id = "id",
            firstName = "FIRST_NAME",
            lastName = "LAST_NAME",
            email = "",
            emailVerified = false,
            dob = "2000-09-05",
            mobile = null,
            mobileVerified = false,
            address = null,
            state = UserState.Created,
            kycState = KycState.None,
            updatedAt = "",
            insertedAt = "",
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
        verify(view).restoreUiState(
            eq(nabuUser.firstName!!),
            eq(nabuUser.lastName!!),
            eq("September 05, 2000"),
            any()
        )
    }

    @Test
    fun `onViewReady does not restore data as it's already present`() {
        // Arrange
        subject.firstNameSet = true
        subject.lastNameSet = true
        subject.dateSet = true
        // Act
        subject.onViewReady()
        // Assert
        verifyZeroInteractions(nabuDataManager)
    }
}
