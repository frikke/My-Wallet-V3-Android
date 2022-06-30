package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.getBlankNabuUser
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class NabuUserSyncUpdateUserWalletInfoWithJWTTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `no interactions until subscribe`() {
        val nabuDataUserProvider = mock<NabuDataUserProvider>()
        val nabuDataManager = mock<NabuDataManager>()
        val nabuUserSync = givenSyncInstance(nabuDataManager, nabuDataUserProvider)
        nabuUserSync
            .syncUser()
        verifyZeroInteractions(nabuDataUserProvider)
        verifyZeroInteractions(nabuDataManager)
    }

    @Test
    fun `on sync user`() {
        val jwt = "JWT"
        val offlineToken = NabuOfflineToken("", "")
        val nabuDataUserProvider: NabuDataUserProvider = mock {
            on { updateUserWalletInfo(any()) }.thenReturn(Single.just(getBlankNabuUser()))
        }
        val nabuDataManager: NabuDataManager = mock {
            on { requestJwt() }.thenReturn(Single.just(jwt))
        }

        val nabuUserSync = givenSyncInstance(nabuDataManager, nabuDataUserProvider)

        nabuUserSync
            .syncUser()
            .test()
            .assertComplete()

        verify(nabuDataUserProvider).updateUserWalletInfo(jwt)
        verifyNoMoreInteractions(nabuDataUserProvider)

        verify(nabuDataManager).requestJwt()
        verifyNoMoreInteractions(nabuDataManager)
    }

    private fun givenSyncInstance(
        nabuDataManager: NabuDataManager,
        nabuDataUserProvider: NabuDataUserProvider
    ): NabuUserSync =
        NabuUserSyncUpdateUserWalletInfoWithJWT(nabuDataManager, nabuDataUserProvider)
}
