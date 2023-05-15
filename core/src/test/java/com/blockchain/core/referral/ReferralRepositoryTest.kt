package com.blockchain.core.referral

import app.cash.turbine.test
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.api.services.ReferralApiService
import com.blockchain.core.referral.dataresource.ReferralResponseWrapper
import com.blockchain.core.referral.dataresource.ReferralStore
import com.blockchain.data.DataResource
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

private const val AUTH = "auth"
private const val FIAT = "GBP"
private const val REF_CODE = "DIEGO123"

class ReferralRepositoryTest {

    private val referralResponse = ReferralResponse(
        rewardTitle = "Reward Title",
        rewardSubtitle = "Reward Subtitle",
        code = REF_CODE,
        criteria = emptyList(),
        campaignId = "UK rewards",
        announcement = null,
        promotion = null
    )

    private val mockFiat: FiatCurrency = mock {
        on { networkTicker } doReturn FIAT
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } doReturn mockFiat
    }
    private val referralApiService: ReferralApiService = mock()
    private val referralStore: ReferralStore = mock()

    private lateinit var referralRepository: ReferralRepository

    @Before
    fun setUp() = runBlocking {
        referralRepository = ReferralRepository(referralStore, referralApiService, currencyPrefs)
    }

    @Test
    fun `should fetch available referral info`() = runBlocking {
        whenever(referralStore.stream(any()))
            .doReturn(flowOf(DataResource.Data(ReferralResponseWrapper(referralResponse))))

        val expectedData = ReferralInfo.Data(
            rewardTitle = referralResponse.rewardTitle,
            rewardSubtitle = referralResponse.rewardSubtitle,
            criteria = referralResponse.criteria,
            code = referralResponse.code,
            campaignId = "UK rewards",
            announcementInfo = null,
            promotionInfo = null
        )

        referralRepository.fetchReferralData().test {
            expectMostRecentItem().run {
                assertEquals(DataResource.Data(expectedData), this)
            }
        }
    }

    @Test
    fun `should fetch referral info not available`() = runBlocking {
        whenever(referralStore.stream(any()))
            .doReturn(flowOf(DataResource.Data(ReferralResponseWrapper(null))))

        referralRepository.fetchReferralData().test {
            expectMostRecentItem().run {
                assertEquals(DataResource.Data(ReferralInfo.NotAvailable), this)
            }
        }
    }

    @Test
    fun `should fetch referral info other errors`() = runBlocking {
        val apiError: NabuApiException = mock {
            on { getErrorStatusCode() } doReturn NabuErrorStatusCodes.InternalServerError
        }

        whenever(referralStore.stream(any()))
            .doReturn(flowOf(DataResource.Error(apiError)))

        referralRepository.fetchReferralData().test {
            expectMostRecentItem().run {
                assertEquals(DataResource.Error(apiError), this)
            }
        }
    }

    @Test
    fun `should check validity valid code`() = runBlocking {
        whenever(referralApiService.validateReferralCode(REF_CODE)).doReturn(Outcome.Success(Unit))

        val result = referralRepository.isReferralCodeValid(REF_CODE)

        assertEquals(Outcome.Success(true), result)
    }

    @Test
    fun `should check validity invalid`() = runBlocking {
        val apiError: NabuApiException = mock {
            on { getErrorStatusCode() } doReturn NabuErrorStatusCodes.NotFound
        }
        whenever(referralApiService.validateReferralCode(REF_CODE))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.isReferralCodeValid(REF_CODE)

        assertEquals(Outcome.Success(false), result)
    }

    @Test
    fun `should check forward other errors`() = runBlocking {
        val apiError: NabuApiException = mock {
            on { getErrorStatusCode() } doReturn NabuErrorStatusCodes.InternalServerError
        }
        whenever(referralApiService.validateReferralCode(REF_CODE))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.isReferralCodeValid(REF_CODE)

        assertEquals(Outcome.Failure(apiError), result)
    }

    @Test
    fun `should send validated referral code`() = runBlocking {
        whenever(referralApiService.associateReferralCode(REF_CODE)).doReturn(Outcome.Success(Unit))

        val result = referralRepository.associateReferralCodeIfPresent(REF_CODE)

        verify(referralApiService).associateReferralCode(REF_CODE)
        assertEquals(Outcome.Success(Unit), result)
    }

    @Test
    fun `should not send request when code is null`() = runBlocking {
        val result = referralRepository.associateReferralCodeIfPresent(null)

        assertEquals(Outcome.Success(Unit), result)
        verifyNoMoreInteractions(referralApiService)
    }
}
