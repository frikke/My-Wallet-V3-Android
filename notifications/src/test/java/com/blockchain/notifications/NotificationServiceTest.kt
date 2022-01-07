package com.blockchain.notifications

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationServiceTest {
    private lateinit var subject: NotificationService
    private val mockWalletApi: WalletApi = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
        mainTrampoline()
    }

    @Before
    fun setUp() {
        subject = NotificationService(mockWalletApi)
    }

    @Test
    fun sendNotificationToken() {
        val guid = "guid"
        val key = "1234"
        val token = "4321"
        whenever(mockWalletApi.updateFirebaseNotificationToken(token, guid, key))
            .thenReturn(Observable.just(mock<ResponseBody>()))

        val testObserver = subject.sendNotificationToken(token, guid, key).test()

        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(mockWalletApi).updateFirebaseNotificationToken(token, guid, key)
        verifyNoMoreInteractions(mockWalletApi)
    }

    @Test
    fun removeNotificationToken() {
        val guid = "guid"
        val key = "1234"
        whenever(mockWalletApi.removeFirebaseNotificationToken(guid, key))
            .thenReturn(Completable.complete())

        val testObserver = subject.removeNotificationToken(guid, key).test()

        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(mockWalletApi).removeFirebaseNotificationToken(guid, key)
        verifyNoMoreInteractions(mockWalletApi)
    }
}
