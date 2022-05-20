package com.blockchain.core.referral

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.api.adapters.ApiError
import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.api.services.ReferralApiService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.domain.referral.model.ReferralValidity
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
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
        criteria = emptyList()
    )

    private val authenticator: Authenticator = mock {
        on { getAuthHeader() } doReturn Single.just(AUTH)
    }

    private val mockFiat: FiatCurrency = mock {
        on { symbol } doReturn FIAT
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } doReturn mockFiat
    }

    private val referralApiService: ReferralApiService = mock()

    private var referralRepository = ReferralRepository(authenticator, referralApiService, currencyPrefs)

    @Before
    fun setUp() {
        referralRepository = ReferralRepository(authenticator, referralApiService, currencyPrefs)
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
        )

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Success(expectedData), result)
    }

    @Test
    fun `should fetch referral info not available`() = runBlocking {
        val apiError: ApiError.KnownError = mock {
            on { statusCode } doReturn NabuErrorStatusCodes.Forbidden
        }

        whenever(referralApiService.getReferralCode(AUTH, FIAT))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Success(ReferralInfo.NotAvailable), result)
    }

    @Test
    fun `should fetch referral info other errors`() = runBlocking {
        val expectedThrowable: NabuApiException = mock {}
        val apiError: ApiError.KnownError = mock {
            on { statusCode } doReturn NabuErrorStatusCodes.InternalServerError
            on { throwable } doReturn expectedThrowable
        }

        whenever(referralApiService.getReferralCode(AUTH, FIAT))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.fetchReferralData()

        assertEquals(Outcome.Failure(expectedThrowable), result)
    }

    @Test
    fun `should check validity valid code`() = runBlocking {
        whenever(referralApiService.validateReferralCode(AUTH, REF_CODE)).doReturn(Outcome.Success(Unit))

        val result = referralRepository.validateReferralCode(REF_CODE)

        assertEquals(Outcome.Success(ReferralValidity.VALID), result)
    }

    @Test
    fun `should check validity invalid`() = runBlocking {
        val apiError: ApiError.KnownError = mock {
            on { statusCode } doReturn NabuErrorStatusCodes.NotFound
        }
        whenever(referralApiService.validateReferralCode(AUTH, REF_CODE))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.validateReferralCode(REF_CODE)

        assertEquals(Outcome.Success(ReferralValidity.INVALID), result)
    }

    @Test
    fun `should check forward other errors`() = runBlocking {
        val expectedThrowable: NabuApiException = mock {}
        val apiError: ApiError.KnownError = mock {
            on { statusCode } doReturn NabuErrorStatusCodes.InternalServerError
            on { throwable } doReturn expectedThrowable
        }
        whenever(referralApiService.validateReferralCode(AUTH, REF_CODE))
            .doReturn(Outcome.Failure(apiError))

        val result = referralRepository.validateReferralCode(REF_CODE)

        assertEquals(Outcome.Failure(expectedThrowable), result)
    }
}
