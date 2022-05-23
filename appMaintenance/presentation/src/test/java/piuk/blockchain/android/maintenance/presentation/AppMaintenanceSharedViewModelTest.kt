package piuk.blockchain.android.maintenance.presentation

import app.cash.turbine.test
import com.blockchain.testutils.CoroutineTestRule
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AppMaintenanceSharedViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val viewModel = AppMaintenanceSharedViewModel()

    @Test
    fun `WHEN resumeAppFlow is called, THEN Unit should be emitted`() = runTest {
        viewModel.resumeAppFlow.test {
            viewModel.resumeAppFlow()

            assertEquals(Unit, expectMostRecentItem())
        }
    }
}
