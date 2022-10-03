package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.util.fakefactory.nabu.FakeNabuSessionTokenFactory
import com.blockchain.testutils.rxInit
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NabuUserSyncUpdateUserWalletInfoWithJWTTest {
    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    private val authenticator = mockk<NabuAuthenticator>()
    private val jwt = "JWT"
    private val nabuDataManager: NabuDataManager = mockk()
    private val nabuService = mockk<NabuService>()
    private val getUserStore = mockk<GetUserStore>()

    private val nabuUserSync: NabuUserSync = NabuUserSyncUpdateUserWalletInfoWithJWT(
        authenticator = authenticator,
        nabuDataManager = nabuDataManager,
        nabuService = nabuService,
        getUserStore = getUserStore
    )

    private val sessionToken = FakeNabuSessionTokenFactory.any
    private val userObject: NabuUser = mockk()

    @Before
    fun setUp() {
        every { authenticator.authenticate(any<(NabuSessionTokenResponse) -> Single<NabuUser>>()) } answers {
            firstArg<(NabuSessionTokenResponse) -> Single<NabuUser>>().invoke(sessionToken)
        }

        every { nabuDataManager.requestJwt() } returns Single.just(jwt)

        every { nabuService.updateWalletInformation(any(), any()) } returns Single.just(userObject)

        every { getUserStore.invalidate() } just Runs

        every { userObject.emailVerified } returns true
        every { userObject.mobileVerified } returns true
    }

    @Test
    fun `on sync user`() {
        nabuUserSync
            .syncUser()
            .test()
            .assertComplete()

        verify { nabuService.updateWalletInformation(sessionToken, jwt) }
        verify { nabuDataManager.requestJwt() }
    }
}
