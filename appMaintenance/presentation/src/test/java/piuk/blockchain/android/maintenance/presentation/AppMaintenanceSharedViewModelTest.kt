package piuk.blockchain.android.maintenance.presentation

import app.cash.turbine.test
import kotlin.test.assertEquals
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
class AppMaintenanceSharedViewModelTest {

    private val viewModel = AppMaintenanceSharedViewModel()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN resumeAppFlow is called, THEN Unit should be emitted`() = runTest {
        viewModel.resumeAppFlow.test {
            viewModel.resumeAppFlow()

            assertEquals(Unit, expectMostRecentItem())
        }
    }
}
