package com.blockchain.analytics.data

import com.blockchain.analytics.AnalyticsContext
import com.blockchain.analytics.AnalyticsContextProvider
import com.blockchain.analytics.AnalyticsLocalPersistence
import com.blockchain.analytics.NabuAnalyticsEvent
import com.blockchain.api.services.AnalyticsService
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.preferences.SessionPrefs
import com.blockchain.utils.Optional
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeStore
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class NabuAnalyticsTest {
    private val localAnalyticsPersistence = mock<AnalyticsLocalPersistence>()

    private val token: Optional<NabuSessionTokenResponse> = Optional.Some(
        NabuSessionTokenResponse(
            "",
            "",
            "",
            true,
            "",
            "",
            ""
        )
    )

    private val tokenStore: NabuSessionTokenStore = mock {
        on { getAccessToken() }.thenReturn(Observable.just(token))
    }

    private val walletModeStore: WalletModeStore = mock {
        on { walletMode }.thenReturn(WalletMode.NON_CUSTODIAL)
    }

    private val sessionPrefs: SessionPrefs = mock {
        on { deviceId }.thenReturn("deviceID")
    }
    private val prefs: Lazy<SessionPrefs> = mock {
        onGeneric { value }.thenReturn(sessionPrefs)
    }
    private val mockedContext: AnalyticsContext = mock()

    private val analyticsService = mock<AnalyticsService>()

    private val analyticsContextProvider = mock<AnalyticsContextProvider>()

    private val lifecycleObservable = mock<LifecycleObservable> {
        on { onStateUpdated }.thenReturn(Observable.just(AppState.FOREGROUNDED))
    }

    private val subject = NabuAnalytics(
        localAnalyticsPersistence = localAnalyticsPersistence,
        prefs = prefs,
        remoteLogger = mock(),
        analyticsService = analyticsService,
        tokenStore = tokenStore,
        walletModeStore = lazy { walletModeStore },
        analyticsContextProvider = analyticsContextProvider,
        lifecycleObservable = lifecycleObservable
    )

    @Ignore
    @Test
    fun flushIsWorking() = runTest {
        whenever(analyticsContextProvider.context()).thenReturn(mockedContext)

        whenever(
            analyticsService.postEvents(
                events = any(),
                id = any(),
                analyticsContext = analyticsContextProvider.context(),
                platform = any(),
                device = any(),
                authorization = anyOrNull()
            )
        ).thenReturn(Completable.complete())

        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(84)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = subject.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, times(3))
            .postEvents(any(), any(), analyticsContextProvider.context(), any(), any(), anyOrNull())

        Mockito.verify(localAnalyticsPersistence, times(2)).removeOldestItems(30)
        Mockito.verify(localAnalyticsPersistence).removeOldestItems(24)
    }

    @Test
    fun flushOnEmptyStorageShouldNotInvokeAnyPosts() {
        whenever(
            analyticsService.postEvents(
                events = any(),
                id = any(),
                analyticsContext = any(),
                platform = any(),
                device = any(),
                authorization = anyOrNull()
            )
        ).thenReturn(Completable.complete())

        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(0)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = subject.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, never())
            .postEvents(any(), any(), any(), any(), any(), anyOrNull())

        Mockito.verify(localAnalyticsPersistence, never()).removeOldestItems(any())
    }

    @Test
    fun ifPostFailsCompletableShouldFailToo() {
        whenever(
            analyticsService.postEvents(
                events = any(),
                id = any(),
                analyticsContext = any(),
                platform = any(),
                device = any(),
                authorization = anyOrNull()
            )
        ).thenReturn(Completable.error(Throwable()))

        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(10)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = subject.flush().test()

        testSubscriber.assertNotComplete()
    }

    private fun randomListOfEventsWithSize(i: Int): List<NabuAnalyticsEvent> {
        return IntArray(i) { i }.map {
            NabuAnalyticsEvent(
                name = "name$it",
                type = "type$it",
                originalTimestamp = "originalTimestamp$it",
                properties = emptyMap()
            )
        }
    }
}
