package piuk.blockchain.android.cards.cvv

import app.cash.turbine.test
import com.blockchain.api.paymentmethods.models.CardDetailsResponse
import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.outcome.Outcome
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityCodeViewModelTest {

    private val paymentMethodsService = mockk<PaymentMethodsService>(relaxed = true)
    private val args = SecurityCodeArgs(
        paymentId = "paymentId",
        cardId = "cardId"
    )
    private val cardDetailsResponse = CardDetailsResponse(
        number = "number",
        type = "AMEX",
        label = "label"
    )

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private companion object {
        private const val CVV = "123"
    }

    private lateinit var viewModel: SecurityCodeViewModel

    @Before
    fun setUp() {
        viewModel = SecurityCodeViewModel(
            paymentMethodsService
        )
    }

    @Test
    fun `WHEN CardDetails are valid THEN should update state with CardDetails`() = runTest {
        val cardResponse = mockk<CardResponse>().apply {
            coEvery { card } returns cardDetailsResponse
        }

        coEvery { paymentMethodsService.getCardDetailsCo(cardId = args.cardId) } returns Outcome.Success(cardResponse)

        viewModel.viewState.test {
            viewModel.viewCreated(args)

            with(expectMostRecentItem()) {
                assertFalse(cardDetailsLoading)
                assertEquals(cardName, cardDetailsResponse.label)
                assertEquals(lastCardDigits, cardDetailsResponse.number)
                assertEquals(cvvLength, 4)
                assertNull(error)
            }
        }
    }

    @Test
    fun `WHEN CardDetails are NOT valid THEN should update state with Error`() = runTest {
        coEvery { paymentMethodsService.getCardDetailsCo(cardId = args.cardId) } returns Outcome.Failure(Exception())

        viewModel.viewState.test {
            viewModel.viewCreated(args)

            with(expectMostRecentItem()) {
                assertFalse(cardDetailsLoading)
                assertEquals(error, UpdateSecurityCodeError.CardDetailsFailed(null))
            }
        }
    }

    @Test
    fun `WHEN CvvInput is changed THEN should update state with CVV`() = runTest {
        viewModel.viewState.test {
            viewModel.onIntent(SecurityCodeIntent.CvvInputChanged(CVV))

            with(expectMostRecentItem()) {
                assertEquals(cvv, CVV)
            }
        }
    }

    @Test
    fun `WHEN CvvUpdate is successful THEN should update state`() = runTest {
        coEvery { paymentMethodsService.updateCvv(paymentId = args.paymentId, cvv = CVV) } returns Outcome.Success(Unit)

        viewModel.navigationEventFlow.test {
            viewModel.viewCreated(args)
            viewModel.onIntent(SecurityCodeIntent.CvvInputChanged(CVV))
            viewModel.onIntent(SecurityCodeIntent.NextClicked)

            expectMostRecentItem() shouldBeEqualTo SecurityCodeNavigation.Next
        }
    }

    @Test
    fun `WHEN CvvUpdate is NOT successful THEN should update state with error`() = runTest {
        coEvery { paymentMethodsService.updateCvv(args.paymentId, CVV) } returns Outcome.Failure(Exception())

        viewModel.viewState.test {
            viewModel.viewCreated(args)
            viewModel.onIntent(SecurityCodeIntent.CvvInputChanged(CVV))
            viewModel.onIntent(SecurityCodeIntent.NextClicked)

            with(expectMostRecentItem()) {
                assertEquals(error, UpdateSecurityCodeError.UpdateCvvFailed(null))
            }
        }
    }
}
