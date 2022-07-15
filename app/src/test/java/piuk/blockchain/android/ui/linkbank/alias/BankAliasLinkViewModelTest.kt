package piuk.blockchain.android.ui.linkbank.alias

import app.cash.turbine.test
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class BankAliasLinkViewModelTest {

    private val bankService = mockk<BankService>(relaxed = true)

    private lateinit var viewModel: BankAliasLinkViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = BankAliasLinkViewModel(
            bankService,
            0 // TODO There has to be a better way to skip debouncing in tests other than this
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN AliasUpdated is triggered, THEN show loading and clear all details`() = runTest {
        viewModel.viewState.test {
            viewModel.onIntent(BankAliasLinkIntent.AliasUpdated(ADDRESS))

            with(expectMostRecentItem()) {
                assertEquals(ADDRESS, alias)
                assertTrue(showAliasInput)
                assertNull(aliasInfo)
                assertNull(error)
            }
        }
    }

    @Test
    fun `WHEN AliasUpdated is triggered with an alias that is too short, THEN ctaState should be Disabled`() = runTest {
        viewModel.viewState.test {
            viewModel.onIntent(BankAliasLinkIntent.AliasUpdated("short"))

            with(expectMostRecentItem()) {
                assertEquals(ButtonState.Disabled, ctaState)
            }
        }
    }

    @Test
    fun `WHEN AliasUpdated is triggered with an alias that is too long, THEN ctaState should be Disabled`() = runTest {
        viewModel.viewState.test {
            viewModel.onIntent(BankAliasLinkIntent.AliasUpdated("suuuuuuuuuper loooooong alias"))

            with(expectMostRecentItem()) {
                assertEquals(ButtonState.Disabled, ctaState)
            }
        }
    }

    @Test
    fun `WHEN LoadAliasInfo is triggered with SUCCESS, THEN alias info should be populated`() = runTest {
        coEvery { bankService.getBeneficiaryInfo(CURRENCY, ADDRESS) } returns Outcome.Success(mockk(relaxed = true))

        viewModel.viewState.test {
            viewModel.onIntent(BankAliasLinkIntent.LoadBeneficiaryInfo(CURRENCY, ADDRESS))

            with(expectMostRecentItem()) {
                assertNull(error)
                assertNotNull(this.aliasInfo)
            }
        }
    }

    private companion object {
        private const val ADDRESS = "alias"
        private const val CURRENCY = "ARS"
    }
}
