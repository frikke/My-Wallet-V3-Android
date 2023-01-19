package com.blockchain.coincore.impl

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.preferences.WalletStatusPrefs
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("TestFunctionName")
fun STUB_THIS(): Nothing = throw NotImplementedError("This method should be mocked")

class OnChainTxEngineBaseTest : CoincoreTestBase() {

    private val walletPreferences: WalletStatusPrefs = mock()
    private val sourceAccount: CryptoAccount = mock()
    private val txTarget: TransactionTarget = mock()
    private val resolvedAddress: Single<String> = mock()

    private class OnChainTxEngineTestSubject(
        requireSecondPassword: Boolean,
        walletPreferences: WalletStatusPrefs,
        resolvedAddress: Single<String>
    ) : OnChainTxEngineBase(
        requireSecondPassword,
        walletPreferences,
        resolvedAddress
    ) {
        override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }

        override fun doInitialiseTx(): Single<PendingTx> {
            STUB_THIS()
        }

        override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }

        override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }

        override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
            STUB_THIS()
        }

        override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
            STUB_THIS()
        }
    }

    private val subject: OnChainTxEngineBase =
        OnChainTxEngineTestSubject(
            requireSecondPassword = false,
            walletPreferences = walletPreferences,
            resolvedAddress = resolvedAddress
        )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        whenever(sourceAccount.currency).thenReturn(ASSET)

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, ASSET)

        verify(sourceAccount).currency

        noMoreInteractions()
    }

    @Test
    fun `userFiat returns value from stored prefs`() {
        // Arrange

        // Act
        val result = subject.userFiat

        assertEquals(result, TEST_USER_FIAT)
        verify(currencyPrefs).selectedFiatCurrency

        noMoreInteractions()
    }

    @Test
    fun `exchange rate stream is returned`() {
        // Arrange
        whenever(sourceAccount.currency).thenReturn(ASSET)
        whenever(exchangeRates.exchangeRateToUserFiat(ASSET))
            .thenReturn(
                Observable.just(
                    ExchangeRate(
                        from = ASSET,
                        to = TEST_USER_FIAT,
                        rate = EXCHANGE_RATE
                    )
                )
            )

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.userExchangeRate()
            .test()
            .assertValueAt(0) {
                it.price.toBigDecimal() == EXCHANGE_RATE
            }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).currency
        verify(exchangeRates).exchangeRateToUserFiat(ASSET)

        noMoreInteractions()
    }

    private fun noMoreInteractions() {
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.ETHER
        private val EXCHANGE_RATE = 0.01.toBigDecimal()
    }
}
