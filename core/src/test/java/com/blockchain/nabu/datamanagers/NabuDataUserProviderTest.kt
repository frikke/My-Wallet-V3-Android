package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.api.getuser.domain.GetUserStoreService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class NabuDataUserProviderTest {

    private val getUserStoreService = mockk<GetUserStoreService>()

    private val nabuDataUserProvider: NabuDataUserProvider = NabuDataUserProviderNabuDataManagerAdapter(
        getUserStoreService = getUserStoreService,
    )

    private val userObject: NabuUser = mockk()

    @Before
    fun setUp() {
        every { getUserStoreService.getUser() } returns Single.just(userObject)
    }

    @Test
    fun `WHEN getUser, THEN getUserStoreService getUser should be called`() {
        // Act
        nabuDataUserProvider.getUser().test()
        // Assert
        verify(exactly = 1) { getUserStoreService.getUser() }
    }
}
