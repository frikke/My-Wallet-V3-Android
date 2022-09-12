package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibility
import com.blockchain.nabu.service.NabuService
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

    private val eligibility = SimpleBuyEligibility(true, true, 0, 10)
    private val eligibility2 = SimpleBuyEligibility(false, false, 10, 10)

    private var nabuService: NabuService = mock()

    private lateinit var cachedEligibilityProvider: NabuCachedEligibilityProvider

    private val testScheduler = TestScheduler()

    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        cachedEligibilityProvider = NabuCachedEligibilityProvider(nabuService)
    }

    @After
    fun tearDown() = RxJavaPlugins.reset()

    @Test
    fun `isEligibleForSimpleBuy should return cached response`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()

        val testObserver2 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()
    }

    @Test
    fun `isEligibleForSimpleBuy should refresh cache after timeout`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()

        testScheduler.advanceTimeBy(DEFAULT_CACHE_LIFETIME + 10, TimeUnit.SECONDS)

        val testObserver2 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(false)

        verify(nabuService, times(2)).isEligibleForSimpleBuy()
    }

    @Test
    fun `isEligibleForSimpleBuy should refresh and return cached response`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.just(eligibility.copy()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)
        verify(nabuService, times(1)).isEligibleForSimpleBuy()

        val testObserver2 = cachedEligibilityProvider.isEligibleForSimpleBuy(true).test()

        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(nabuService, times(2)).isEligibleForSimpleBuy()

        val testObserver3 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        testObserver3.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(true)

        verify(nabuService, times(2)).isEligibleForSimpleBuy()
    }

    @Test
    fun `isEligibleForSimpleBuy should return false when there is an error`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.isEligibleForSimpleBuy().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(false)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()
    }

    @Test
    fun `simpleBuyTradingEligibility should return cached response`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.error(IOException()))

        // Act
        val testObserver1 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()

        val testObserver2 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()
    }

    @Test
    fun `simpleBuyTradingEligibility should refresh cache after forced refresh`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.just(eligibility2))

        // Act
        val testObserver1 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()

        cachedEligibilityProvider.isEligibleForSimpleBuy(true).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(false)

        verify(nabuService, times(2)).isEligibleForSimpleBuy()

        cachedEligibilityProvider.simpleBuyTradingEligibility().test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility2)

        verify(nabuService, times(2)).isEligibleForSimpleBuy()
    }

    @Test
    fun `simpleBuyTradingEligibility should refresh cache after timeout`() {
        // Arrange
        whenever(nabuService.isEligibleForSimpleBuy())
            .thenReturn(Single.just(eligibility))
            .thenReturn(Single.just(eligibility2))

        // Act
        val testObserver1 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()

        // Assert
        testObserver1.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility)

        verify(nabuService, times(1)).isEligibleForSimpleBuy()

        testScheduler.advanceTimeBy(DEFAULT_CACHE_LIFETIME + 10, TimeUnit.SECONDS)

        val testObserver2 = cachedEligibilityProvider.simpleBuyTradingEligibility().test()
        testObserver2.assertComplete()
            .assertComplete()
            .assertNoErrors()
            .assertValue(eligibility2)

        verify(nabuService, times(2)).isEligibleForSimpleBuy()
    }
}
