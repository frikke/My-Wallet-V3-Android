package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.testutils.numberToBigDecimal
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class CryptoAccountCompoundGroupTest : CoincoreTestBase() {

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiat(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
    }

    @Test
    fun `group with single account returns single account balance`() {
        // Arrange
        val accountBalance = AccountBalance(
            total = 100.testValue(),
            pending = 0.testValue(),
            withdrawable = 100.testValue(),
            exchangeRate = TEST_TO_USER_RATE
        )

        val account: CryptoAccount = mock {
            on { balanceRx() }.thenReturn(Observable.just(accountBalance))
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account)
        )

        // Act
        subject.balanceRx().test()
            .assertComplete()
            .assertValue {
                it.total == accountBalance.total &&
                    it.withdrawable == accountBalance.withdrawable &&
                    it.pending == accountBalance.pending &&
                    it.exchangeRate == TEST_TO_USER_RATE
            }
    }

    @Test
    fun `group with two accounts returns the sum of the account balance`() {
        // Arrange
        val accountBalance1 = AccountBalance(
            total = 100.testValue(),
            pending = 0.testValue(),
            withdrawable = 100.testValue(),
            exchangeRate = TEST_TO_USER_RATE
        )

        val account1: CryptoAccount = mock {
            on { balanceRx() }.thenReturn(Observable.just(accountBalance1))
        }

        val accountBalance2 = AccountBalance(
            total = 50.testValue(),
            pending = 0.testValue(),
            withdrawable = 40.testValue(),
            exchangeRate = TEST_TO_USER_RATE
        )
        val account2: CryptoAccount = mock {
            on { balanceRx() }.thenReturn(Observable.just(accountBalance2))
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account1, account2)
        )

        // Act
        subject.balanceRx().test()
            .assertComplete()
            .assertValueAt(1) {
                it.total == accountBalance1.total + accountBalance2.total &&
                    it.pending == accountBalance1.pending + accountBalance2.pending &&
                    it.withdrawable == accountBalance1.withdrawable + accountBalance2.withdrawable &&
                    it.exchangeRate == TEST_TO_USER_RATE
            }
    }

    @Test
    fun `group with single account returns single account actions`() {
        // Arrange
        val accountActions = setOf(
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Receive)
        )

        val account: CryptoAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(accountActions))
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account)
        )

        // Act
        val r = subject.stateAwareActions.test()

        // Assert
        r.assertValue(accountActions)
    }

    @Test
    fun `group with three accounts returns the union of possible actions`() {
        // Arrange
        val accountActions1 = Single.just(
            setOf(
                StateAwareAction(ActionState.Available, AssetAction.Send),
                StateAwareAction(ActionState.Available, AssetAction.Receive)
            )
        )

        val accountActions2 = Single.just(
            setOf(
                StateAwareAction(ActionState.Available, AssetAction.Send),
                StateAwareAction(ActionState.Available, AssetAction.Swap)
            )
        )

        val accountActions3 = Single.just(
            setOf(
                StateAwareAction(ActionState.Available, AssetAction.Send),
                StateAwareAction(ActionState.Available, AssetAction.Receive)
            )
        )

        val expectedResult = setOf(
            StateAwareAction(ActionState.Available, AssetAction.Send),
            StateAwareAction(ActionState.Available, AssetAction.Swap),
            StateAwareAction(ActionState.Available, AssetAction.Receive)
        )

        val account1: CryptoAccount = mock {
            on { stateAwareActions }.thenReturn(accountActions1)
        }

        val account2: CryptoAccount = mock {
            on { stateAwareActions }.thenReturn(accountActions2)
        }

        val account3: CryptoAccount = mock {
            on { stateAwareActions }.thenReturn(accountActions3)
        }

        val subject = CryptoAccountNonCustodialGroup(
            asset = TEST_ASSET,
            label = "group label",
            accounts = listOf(account1, account2, account3)
        )

        // Act
        val r = subject.stateAwareActions.test()

        // Assert
        r.assertValue(expectedResult)
    }

    companion object {
        private val TEST_TO_USER_RATE = ExchangeRate(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = 1.2.toBigDecimal()
        )

        fun Number.testValue() = CryptoValue.fromMajor(TEST_ASSET, numberToBigDecimal())
    }
}
