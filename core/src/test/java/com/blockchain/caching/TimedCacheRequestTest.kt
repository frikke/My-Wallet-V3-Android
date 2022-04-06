package com.blockchain.caching

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class TimedCacheRequestTest {

    private val testScheduler = TestScheduler()

    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
    }

    @After
    fun tearDown() = RxJavaPlugins.reset()

    @Test
    fun `invoke refresh function once and return cached result before expiry`() {
        val invocationCounter = AtomicInteger()
        val testFunction = { Single.just(invocationCounter.incrementAndGet()) }

        val timedCacheRequest = TimedCacheRequest(10L, testFunction)

        assertEquals(0, invocationCounter.get())

        timedCacheRequest.getCachedSingle()
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(1)

        val testObservable = timedCacheRequest.getCachedSingle().test()
        testObservable.assertValue(1)
    }

    @Test
    fun `invoke refresh function again after invalidated`() {
        val invocationCounter = AtomicInteger()
        val testFunction = { Single.just(invocationCounter.incrementAndGet()) }

        val timedCacheRequest = TimedCacheRequest(10L, testFunction)

        assertEquals(0, invocationCounter.get())

        timedCacheRequest.getCachedSingle()
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(1)

        timedCacheRequest.invalidate()

        val testObservable = timedCacheRequest.getCachedSingle().test()
        testObservable.assertValue(2)
    }

    @Test
    fun `invoke refresh function again after time expired`() {
        val invocationCounter = AtomicInteger()
        val testFunction = { Single.just(invocationCounter.incrementAndGet()) }

        val timedCacheRequest = TimedCacheRequest(10L, testFunction)

        timedCacheRequest.getCachedSingle()
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(1)

        testScheduler.advanceTimeBy(13, TimeUnit.SECONDS)

        val testObservable = timedCacheRequest.getCachedSingle().test()
        testObservable.assertValue(2)
    }

    @Test
    fun `invoke refresh function again after an error`() {
        val invocationCounter = AtomicInteger()

        val expectedError = IOException()

        val testFunction: () -> Single<Int> = mock()
        whenever(testFunction.invoke())
            .thenReturn(Single.error(expectedError))

        val timedCacheRequest = TimedCacheRequest(10L, testFunction)

        timedCacheRequest.getCachedSingle()
            .test()
            .assertError(expectedError)
            .assertNoValues()

        whenever(testFunction.invoke())
            .thenReturn(Single.just(invocationCounter.incrementAndGet()))

        timedCacheRequest.getCachedSingle()
            .test()
            .assertValue(1)
            .assertComplete()
    }
}
