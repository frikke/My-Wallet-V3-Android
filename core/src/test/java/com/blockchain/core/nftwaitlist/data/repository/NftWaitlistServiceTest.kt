package com.blockchain.core.nftwaitlist.data.repository

import com.blockchain.api.services.NftWaitlistApiService
import com.blockchain.core.nftwaitlist.data.NftWailslitRepository
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NftWaitlistServiceTest {
    private val nftWaitlistApiService = mockk<NftWaitlistApiService>()
    private val userIdentity = mockk<UserIdentity>()

    private val nftWaitlistService: NftWaitlistService = NftWailslitRepository(
        nftWaitlistApiService = nftWaitlistApiService,
        userIdentity = userIdentity,
    )

    private val basicProfileInfo = mockk<BasicProfileInfo>()
    private val apiError = mockk<Exception>()

    @Before
    fun setUp() {
        every { userIdentity.getBasicProfileInformation() } returns Single.just(basicProfileInfo)
        every { basicProfileInfo.email } returns "email"
    }

    @Test
    fun `GIVEN joinNftWaitlist Successful, WHEN joinWaitlist is called, THEN Success should be returned`() = runTest {
        coEvery { nftWaitlistApiService.joinNftWaitlist(any()) } returns Outcome.Success(Unit)

        val result = nftWaitlistService.joinWaitlist()

        assertEquals(Outcome.Success(Unit), result)
    }

    @Test
    fun `GIVEN joinNftWaitlist Unsuccessful, WHEN joinWaitlist is called, THEN Failure should be returned`() = runTest {
        coEvery { nftWaitlistApiService.joinNftWaitlist(any()) } returns Outcome.Failure(apiError)

        val result = nftWaitlistService.joinWaitlist()

        assertEquals(Outcome.Failure(apiError), result)
    }
}
