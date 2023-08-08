package com.blockchain.notifications

import com.blockchain.android.testutils.rxInit
import com.blockchain.data.DataResource
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationServiceTest {
    private lateinit var subject: NotificationService
    private val mockWalletApi: WalletApi = mock()
    private val notificationStorage: NotificationStorage = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
        mainTrampoline()
    }

    @Before
    fun setUp() {
        subject = NotificationService(mockWalletApi, notificationStorage)
    }

    @Test
    fun sendNotificationToken() {
        val guid = "guid"
        val key = "1234"
        val token = "4321"
        whenever(notificationStorage.stream(any()))
            .thenReturn(flowOf(DataResource.Data(Unit)))

        val testObserver = subject.sendNotificationToken(token, guid, key).test()

        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(notificationStorage).stream(any())
        verifyNoMoreInteractions(notificationStorage)
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
