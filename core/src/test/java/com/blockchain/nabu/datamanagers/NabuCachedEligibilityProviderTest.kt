package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.service.NabuService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test

class NabuCachedEligibilityProviderTest {

    private val eligibility = SimpleBuyEligibilityDto(true, true, 0, 10)
    private val eligibility2 = SimpleBuyEligibilityDto(false, false, 10, 10)

    private var nabuService: NabuService = mock()
    private var authenticator: Authenticator = mock()

    private lateinit var cachedEligibilityProvider: NabuCachedEligibilityProvider

    private val testScheduler = TestScheduler()

    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        cachedEligibilityProvider = NabuCachedEligibilityProvider(nabuService, authenticator)
    }

    @After
    fun tearDown() = RxJavaPlugins.reset()

    @Test
    fun `isEligibleForSimpleBuy should return cached response`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        val testObserver2 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }

    @Test
    fun `isEligibleForSimpleBuy should refresh cache after timeout`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        testScheduler.advanceTimeBy(DEFAULT_CACHE_LIFETIME + 10, TimeUnit.SECONDS)

        val testObserver2 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(false)

        verify(authenticator, times(2)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }

    @Test
    fun `isEligibleForSimpleBuy should refresh and return cached response`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.just(eligibility.copy()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)
        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        val testObserver2 = cachedEligibilityProvider.isEligibleForSimpleBuy(true).test()

        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(authenticator, times(2)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        val testObserver3 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        testObserver3.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(authenticator, times(2)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }

    @Test
    fun `isEligibleForSimpleBuy should return false when there is an error`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(false)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }

    @Test
    fun `simpleBuyTradingEligibility should return cached response`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        val testObserver2 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }

    @Test
    fun `simpleBuyTradingEligibility should refresh cache after forced refresh`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.just(eligibility2))

        // Act
        val testObserver1 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        cachedEligibilityProvider.isEligibleForSimpleBuy(true).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(false)

        verify(authenticator, times(2)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        cachedEligibilityProvider.simpleBuyTradingEligibility().test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility2)

        verify(authenticator, times(2)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }

    @Test
    fun `simpleBuyTradingEligibility should refresh cache after timeout`() {
        // Arrange
        whenever(authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>()))
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.just(eligibility2))

        // Act
        val testObserver1 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(authenticator, times(1)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())

        testScheduler.advanceTimeBy(DEFAULT_CACHE_LIFETIME + 10, TimeUnit.SECONDS)

        val testObserver2 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility2)

        verify(authenticator, times(2)).authenticate(any<(NabuSessionTokenResponse) -> Single<SimpleBuyEligibilityDto>>())
    }
}
