package com.blockchain.core.referral

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.api.services.ReferralApiService
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
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
        campaignId = "UK rewards"
    )

    private val authenticator: Authenticator = mock {
        on { getAuthHeader() } doReturn Single.just(AUTH)
    }

    private val mockFiat: FiatCurrency = mock {
        on { networkTicker } doReturn FIAT
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } doReturn mockFiat
    }

    private val featureFlag: IntegratedFeatureFlag = mock()

    private val referralApiService: ReferralApiService = mock()

    private lateinit var referralRepository: ReferralRepository

    @Before
    fun setUp() = runBlocking {
        whenever(featureFlag.coEnabled()).doReturn(true)
        referralRepository = ReferralRepository(authenticator, referralApiService, currencyPrefs, featureFlag)
    }

    @Test
    fun `should fetch available referral info`() = runBlocking {
        whenever(referralApiService.getReferralCode(AUTH, FIAT))
            .doReturn(Outcome.Success(referralResponse))

        val expectedData = ReferralInfo.Data(
            rewardTitle = referralResponse.rewardTitle,
            rewardSubtitle = referralResponse.rewardSubtitle,
            criteria = referralResponse.criteria,
            code = referralResponse.code,
            campaignId = "UK rewards"
        )

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Success(expectedData), result)
    }

    @Test
    fun `should fetch referral info not available`() = runBlocking {
        whenever(referralApiService.getReferralCode(AUTH, FIAT))
            .doReturn(Outcome.Success(null))

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Success(ReferralInfo.NotAvailable), result)
    }

    @Test
    fun `should fetch referral info not available when feature flag disabled`() = runBlocking {
        whenever(featureFlag.coEnabled()).doReturn(false)

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Success(ReferralInfo.NotAvailable), result)
        verifyNoMoreInteractions(referralApiService)
    }

    @Test
    fun `should fetch referral info other errors`() = runBlocking {
        val apiError: NabuApiException = mock {
            on { getErrorStatusCode() } doReturn NabuErrorStatusCodes.InternalServerError
        }

        whenever(referralApiService.getReferralCode(AUTH, FIAT))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Failure(apiError), result)
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
        whenever(referralApiService.associateReferralCode(AUTH, REF_CODE)).doReturn(Outcome.Success(Unit))

        val result = referralRepository.associateReferralCodeIfPresent(REF_CODE)

        verify(referralApiService).associateReferralCode(AUTH, REF_CODE)
        assertEquals(Outcome.Success(Unit), result)
    }

    @Test
    fun `should not send request when feature disabled`() = runBlocking {
        whenever(featureFlag.coEnabled()).doReturn(false)

        val result = referralRepository.associateReferralCodeIfPresent(REF_CODE)

        assertEquals(Outcome.Success(Unit), result)
        verifyNoMoreInteractions(authenticator)
        verifyNoMoreInteractions(referralApiService)
    }

    @Test
    fun `should not send request when code is null`() = runBlocking {

        val result = referralRepository.associateReferralCodeIfPresent(null)

        assertEquals(Outcome.Success(Unit), result)
        verifyNoMoreInteractions(authenticator)
        verifyNoMoreInteractions(referralApiService)
    }
}
