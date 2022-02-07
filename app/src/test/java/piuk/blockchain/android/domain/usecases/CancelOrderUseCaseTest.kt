package piuk.blockchain.android.domain.usecases

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.preferences.BankLinkingPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.toPreferencesValue

class CancelOrderUseCaseTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val buySellOrder: BuySellOrder = mock()

    private lateinit var subject: CancelOrderUseCase

    @Before
    fun setUp() {
        subject = CancelOrderUseCase(
            bankLinkingPrefs,
            custodialWalletManager
        )
    }

    @Test
    fun `if state is at least PENDING_CONFIRMATION should delete order`() {
        // Arrange
        whenever(buySellOrder.state).thenReturn(OrderState.PENDING_CONFIRMATION)
        whenever(buySellOrder.id).thenReturn(ORDER_ID)
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(buySellOrder))
        whenever(custodialWalletManager.deleteBuyOrder(ORDER_ID)).thenReturn(Completable.complete())

        // Act
        val result = subject.invoke(ORDER_ID)

        // Assert
        verify(bankLinkingPrefs).setBankLinkingState(BankAuthDeepLinkState().toPreferencesValue())
        result
            .test()
            .assertComplete()
        verify(custodialWalletManager).deleteBuyOrder(ORDER_ID)
    }

    @Test
    fun `if state has been confirmed should not delete order`() {
        // Arrange
        whenever(buySellOrder.state).thenReturn(OrderState.AWAITING_FUNDS)
        whenever(buySellOrder.id).thenReturn(ORDER_ID)
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(buySellOrder))

        // Act
        val result = subject.invoke(ORDER_ID)

        // Assert
        verify(bankLinkingPrefs).setBankLinkingState(BankAuthDeepLinkState().toPreferencesValue())
        result
            .test()
            .assertComplete()
        verify(custodialWalletManager, never()).deleteBuyOrder(ORDER_ID)
    }

    private companion object {
        private const val ORDER_ID = "orderId"
    }
}
