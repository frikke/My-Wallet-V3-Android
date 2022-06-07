package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.nftwaitlist.data.api.NftWaitlistApi
import com.blockchain.api.nftwaitlist.data.model.NftWaitlistDto
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NftWaitlistApiServiceTest {
    private val nftWaitlistApi = mockk<NftWaitlistApi>()

    private val nftWaitlistApiService = NftWaitlistApiService(nftWaitlistApi = nftWaitlistApi)
    private val dto = mockk<NftWaitlistDto>()
    private val apiError = mockk<ApiError>()

    @Test
    fun `GIVEN joinNftWaitlist Successful, WHEN joinNftWaitlist is called, THEN Success should be returned`() = runTest {
        coEvery { nftWaitlistApi.joinNftWaitlist(any()) } returns Outcome.Success(Unit)

        val result = nftWaitlistApiService.joinNftWaitlist(dto)

        assertEquals(Outcome.Success(Unit), result)
    }

    @Test
    fun `GIVEN joinNftWaitlist Unsuccessful, WHEN joinNftWaitlist is called, THEN Success should be returned`() = runTest {
        coEvery { nftWaitlistApi.joinNftWaitlist(any()) } returns Outcome.Failure(apiError)

        val result = nftWaitlistApiService.joinNftWaitlist(dto)

        assertEquals(Outcome.Failure(apiError), result)
    }
}
