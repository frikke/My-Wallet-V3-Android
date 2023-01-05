package com.blockchain.coincore.impl

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AssetFilter
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

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadEmptyAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.isEmpty() }

        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun allAccountsLoaded() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadFourAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 4 }

        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun noReloadIfUnchanged() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadTwoAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadThreeAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        verify(interestService).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun reloadIfInterestIsEnabled() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(true))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadTwoAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadThreeAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue {
                it.size == 3
            }

        verify(interestService).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun reloadIfForced() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadTwoAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.setForceRefresh()

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadThreeAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun forceLoadResets() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadTwoAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 2 }

        subject.setForceRefresh()

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadThreeAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadFourAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }

        verify(interestService).isAssetAvailableForInterest(TEST_ASSET)
        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun accountsAreRemoved() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadFourAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 4 }

        subject.setForceRefresh()

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadThreeAccountList(it)
        }
            .test()
            .assertComplete()
            .assertValue { it.size == 3 }
            .assertValue { !it.contains(mockAccountD) }

        verifyNoMoreInteractions(interestService)
    }

    @Test
    fun errorsArePropagatedFromLoad() {

        whenever(interestService.isAssetAvailableForInterest(TEST_ASSET))
            .thenReturn(Single.just(false))

        subject.fetchAccountList(
            assetFilter = AssetFilter.Custodial
        ) {
            loadAccountListFailed(it)
        }
            .test()
            .assertError(Throwable::class.java)

        verifyNoMoreInteractions(interestService)
    }

    private val mockAccountA: CryptoAccount = mock()
    private val mockAccountB: CryptoAccount = mock()
    private val mockAccountC: CryptoAccount = mock()
    private val mockAccountD: CryptoAccount = mock()

    private fun loadEmptyAccountList(filter: AssetFilter): Single<SingleAccountList> =
        Single.just(listOf())

    private fun loadOneAccountList(filter: AssetFilter): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA
            )
        )

    private fun loadTwoAccountList(filter: AssetFilter): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA,
                mockAccountB
            )
        )

    private fun loadThreeAccountList(filter: AssetFilter): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA,
                mockAccountB,
                mockAccountC
            )
        )

    private fun loadFourAccountList(filter: AssetFilter): Single<SingleAccountList> =
        Single.just(
            listOf(
                mockAccountA,
                mockAccountB,
                mockAccountC,
                mockAccountD
            )
        )

    private fun loadAccountListFailed(filter: AssetFilter): Single<SingleAccountList> =
        Single.error(Throwable("Something went wrong"))
}
