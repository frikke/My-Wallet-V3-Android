package com.blockchain.nabu.api.getuser.data

import com.blockchain.nabu.api.getuser.domain.GetUserStoreService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.StoreResponse
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class GetUserStoreRepositoryTest {
    private val getUserStore = mockk<GetUserStore>()

    private val getUserStoreService: GetUserStoreService = GetUserStoreRepository(
        getUserStore = getUserStore
    )

    private val nabuUser = mockk<NabuUser>()

    @Before
    fun setUp() {
        every { getUserStore.stream(any()) } returns
            flowOf(StoreResponse.Data(nabuUser))

        every { getUserDataSource.invalidate() } just Runs
    }

    @Test
    fun testGetUser() {
        getUserStoreService.getUser()
            .test()
            .await()
            .assertValue {
                it == nabuUser
            }

        verify(exactly = 1) { getUserStore.stream(any()) }
    }
}
