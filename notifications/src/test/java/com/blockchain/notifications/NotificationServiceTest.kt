package com.blockchain.notifications

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Observable
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.testutils.RxTest

class NotificationServiceTest : RxTest() {
    private lateinit var subject: NotificationService
    private val mockWalletApi: WalletApi = mock()

    @Before
    fun setUp() {
        subject = NotificationService(mockWalletApi)
    }

    @Test
    fun sendNotificationToken() {
        // Arrange
        whenever(mockWalletApi.updateFirebaseNotificationToken("", "", ""))
            .thenReturn(
                Observable.just(
                    mock<ResponseBody>()
                )
            )
        // Act
        val testObserver = subject.sendNotificationToken("", "", "").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        Mockito.verify(mockWalletApi).updateFirebaseNotificationToken("", "", "")
        Mockito.verifyNoMoreInteractions(mockWalletApi)
    }

    @Test
    fun removeNotificationToken() {
        // TODO in upcoming ticket
    }
}