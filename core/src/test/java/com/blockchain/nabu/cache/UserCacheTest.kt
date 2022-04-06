package com.blockchain.nabu.cache

import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.service.NabuService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify

class UserCacheTest {

    private val testScheduler = TestScheduler()

    private val nabuService: NabuService = mock()
    private val token: NabuSessionTokenResponse = NabuSessionTokenResponse(
        id = "444",
        userId = "34231423",
        token = "123",
        isActive = false,
        expiresAt = "123",
        insertedAt = "2312123",
        updatedAt = "12231221233",
    )
    private val user: NabuUser = mock()

    private lateinit var subject: UserCache

    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }

        subject = UserCache(nabuService)
    }

    @Test
    fun `getUser should return cached result when called more than once`() {
        whenever(nabuService.getUser(token)).thenReturn(Single.just(user))

        subject.cached(token).test()
            .assertComplete()
        verify(nabuService).getUser(token)

        subject.cached(token).test()
            .assertComplete()
        verifyNoMoreInteractions(nabuService)
    }
}
