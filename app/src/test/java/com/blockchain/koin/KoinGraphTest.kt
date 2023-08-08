package com.blockchain.koin

import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.testutils.NoOpCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.test.mock.MockProviderRule
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.FakeCoreClient
import piuk.blockchain.android.FakeWeb3Wallet

@Config(sdk = [26], application = BlockchainTestApplication::class, shadows = [FakeCoreClient::class, FakeWeb3Wallet::class])
@RunWith(RobolectricTestRunner::class)
class KoinGraphTest : KoinTest {

    @get:Rule
    val mockProvider = MockProviderRule.create { clazz ->
        Mockito.mock(clazz.java)
    }

    // This rule was added specifically for ViewModels, during the koin tests it would instantiate the viewmodels, calling the init {},
    // which would in turn use fields from the constructor which are null during these tests, causing the test to fail.
    // With the NoOp dispatcher the code inside the viewModelScope.launch {} will not execute, which although is not a perfect solution,
    // it solves most of our problems, allowing us to use the init {} of the viewmodels which is where we should be initialising
    // instead of relying on a LoadData/Initialise/Start Intent or relying on the viewCreated.
    // This is because now in compose + navigation, we can no longer ensure that these initialisation intents or viewCreated are
    // only called once.
    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule(NoOpCoroutineDispatcher())

    @After
    fun cleanup() {
        stopKoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test module configuration`() {
        getKoin().checkModules {
            withInstance<CoroutineScope>(TestScope())
            // for example viewmodels expecting a boolean argument
            withInstance(true)
        }
    }
}
