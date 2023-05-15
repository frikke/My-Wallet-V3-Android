package piuk.blockchain.android.ui.kyc.profile

import app.cash.turbine.test
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.CurrenciesResponse
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.util.toISO8601DateString
import com.blockchain.outcome.Outcome
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.testutils.date
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class KycProfileModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private lateinit var subject: KycProfileModel
    private val analytics: Analytics = mockk(relaxed = true)
    private val nabuDataManager: NabuDataManager = mockk {
        coEvery { isProfileNameValid(any(), any()) } returns Outcome.Success(true)
    }
    private val userService: UserService = mockk {
        every { getUserFlow(any()) } returns flow { throw NullPointerException() }
    }
    private val getUserStore: GetUserStore = mockk(relaxed = true)

    @Before
    fun setUp() {
        subject = KycProfileModel(
            analytics,
            nabuDataManager,
            userService,
            getUserStore
        )
    }

    @Test
    fun `continue button should be disabled unless all fields are filled`() = runTest {
        subject.viewState.test {
            expectMostRecentItem()

            subject.onIntent(KycProfileIntent.FirstNameInputChanged("John"))
            awaitItem().continueButtonState shouldBeEqualTo ButtonState.Disabled

            subject.onIntent(KycProfileIntent.LastNameInputChanged("Doe"))
            awaitItem().continueButtonState shouldBeEqualTo ButtonState.Disabled

            subject.onIntent(KycProfileIntent.DateOfBirthInputChanged(Calendar.getInstance()))
            awaitItem().continueButtonState shouldBeEqualTo ButtonState.Enabled
        }
    }

    @Test
    fun `on continue clicked all data correct, user conflict`() = runTest {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        subject.onIntent(KycProfileIntent.FirstNameInputChanged(firstName))
        subject.onIntent(KycProfileIntent.LastNameInputChanged(lastName))
        subject.onIntent(KycProfileIntent.DateOfBirthInputChanged(dateOfBirth))

        val responseBody =
            ResponseBody.create(
                ("application/json").toMediaTypeOrNull(),
                "{}"
            )
        val createBasicUserDeferred = CompletableDeferred<Unit>()
        coEvery {
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString()
            )
        } coAnswers {
            createBasicUserDeferred.await()
            Outcome.Failure(
                NabuApiExceptionFactory.fromResponseBody(
                    HttpException(Response.error<Unit>(409, responseBody))
                )
            )
        }
        subject.viewState.test {
            expectMostRecentItem()
            // Act
            subject.onIntent(KycProfileIntent.ContinueClicked)
            awaitItem().continueButtonState shouldBeEqualTo ButtonState.Loading
            createBasicUserDeferred.complete(Unit)
            // Assert
            awaitItem().apply {
                continueButtonState shouldBeEqualTo ButtonState.Enabled
                error shouldBeEqualTo KycProfileError.UserConflict
            }
        }
    }

    @Test
    fun `onViewReady restores data to the UI`() = runTest {
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
        every { userService.getUserFlow(any()) } returns flowOf(nabuUser)

        subject.viewState.test {
            expectMostRecentItem()
            // Act
            subject.viewCreated(Args("UK", null, true))
            // Assert
            awaitItem().apply {
                firstNameInput shouldBeEqualTo nabuUser.firstName!!
                lastNameInput shouldBeEqualTo nabuUser.lastName!!
                dateOfBirthInput shouldBeEqualTo Calendar.Builder().setDate(2000, 8, 5).build()
            }
        }
    }
}
