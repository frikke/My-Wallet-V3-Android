package com.blockchain.coincore.impl

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.testutil.CoincoreTestBase.Companion.TEST_ASSET
import com.blockchain.earn.domain.service.InterestService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class ActiveAccountListTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val interestService: InterestService = mock()
    private val subject = ActiveAccountList(
        asset = TEST_ASSET,
        interestService = interestService
    )

    @Test
    fun noAccountsFound() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadEmptyAccountList)
            .test()
            .assertComplete()
            .assertValue { it.isEmpty() }

        verify(interestService).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun allAccountsLoaded() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadFourAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 4 }

        verify(interestService).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun noReloadIfUnchanged() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadTwoAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.fetchAccountList(::loadThreeAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        verify(interestService, times(2)).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun reloadIfInterestStateChanged() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(true))

        subject.fetchAccountList(::loadTwoAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.fetchAccountList(::loadThreeAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        verify(interestService, times(2)).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun reloadIfForced() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadTwoAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.setForceRefresh()

        subject.fetchAccountList(::loadThreeAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        verify(interestService, times(2)).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun forceLoadResets() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadTwoAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.setForceRefresh()

        subject.fetchAccountList(::loadThreeAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        subject.fetchAccountList(::loadFourAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        verify(interestService, times(3)).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun accountsAreRemoved() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadFourAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 4 }

        subject.setForceRefresh()

        subject.fetchAccountList(::loadThreeAccountList)
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }
            .assertValue { !it.contains(mockAccountD) }

        verify(interestService, times(2)).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun errorsArePropagatedFromLoad() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(::loadAccountListFailed)
            .test()
            .assertError(Throwable::class.java)

        verify(interestService).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    private val mockAccountA: CryptoAccount = mock()
    private val mockAccountB: CryptoAccount = mock()
    private val mockAccountC: CryptoAccount = mock()
    private val mockAccountD: CryptoAccount = mock()

    private fun loadEmptyAccountList(): Single<SingleAccountList> =
        Single.just(listOf())

    private fun loadOneAccountList(): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA
            )
        )

    private fun loadTwoAccountList(): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA,
                mockAccountB
            )
        )

    private fun loadThreeAccountList(): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA,
                mockAccountB,
                mockAccountC
            )
        )

    private fun loadFourAccountList(): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA,
                mockAccountB,
                mockAccountC,
                mockAccountD
            )
        )

    private fun loadAccountListFailed(): Single<SingleAccountList> =
        Single.error(Throwable("Something went wrong"))
}
