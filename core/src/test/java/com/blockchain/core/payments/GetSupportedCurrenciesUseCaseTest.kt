package com.blockchain.core.payments

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.blockchain.android.testutils.rxInit
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.nabu.models.responses.nabu.Address
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.testutils.MockKRule
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class GetSupportedCurrenciesUseCaseTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    @get:Rule
    val mockKRule = MockKRule()

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val nabuDataUserProvider: NabuDataUserProvider = mockk()
    private val bindFeatureFlag: FeatureFlag = mockk()

    private lateinit var subject: GetSupportedCurrenciesUseCase

    @Before
    fun setUp() {
        subject = GetSupportedCurrenciesUseCase(
            nabuDataUserProvider,
            bindFeatureFlag
        )
    }

    @Test
    fun `argentinian users should support ARS currency`() {
        // Arrange
        val mockAddress = mockk<Address>().apply {
            every { countryCode } returns "AR"
        }
        val user = mockk<NabuUser>().apply {
            every { address } returns mockAddress
        }
        every { nabuDataUserProvider.getUser() } returns Single.just(user)
        every { bindFeatureFlag.enabled } returns Single.just(true)

        // Assert
        subject.invoke(Unit).test()
            .assertValue(
                SupportedCurrencies(
                    fundsCurrencies = listOf("USD", "ARS"),
                    wireTransferCurrencies = listOf("USD", "ARS")
                )
            )
    }

    @Test
    fun `users should support USD, EUR and GBP currency`() {
        // Arrange
        val mockAddress = mockk<Address>().apply {
            every { countryCode } returns "GB"
        }
        val user = mockk<NabuUser>().apply {
            every { address } returns mockAddress
        }
        every { nabuDataUserProvider.getUser() } returns Single.just(user)
        every { bindFeatureFlag.enabled } returns Single.just(true)

        // Assert
        subject.invoke(Unit).test()
            .assertValue(
                SupportedCurrencies(
                    fundsCurrencies = listOf("GBP", "EUR", "USD"),
                    wireTransferCurrencies = listOf("GBP", "EUR", "USD")
                )
            )
    }
}
