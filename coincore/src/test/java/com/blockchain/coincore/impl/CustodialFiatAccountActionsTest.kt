package com.blockchain.coincore.impl

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.junit.Before
import org.junit.Test

class CustodialFiatAccountActionsTest : CoincoreTestBase() {

    private val custodialManager: CustodialWalletManager = mock()
    private val tradingService: TradingService = mock()
    private val simpleBuyService: SimpleBuyService = mock()
    private val bankService: BankService = mock()

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiat(TEST_FIAT_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
    }

    @Test
    fun `If can't transact with banks then only activity is available`() {
        // Arrange
        val subject = configureActionSubject()

        configureActionTest(
            canTransactWithBankMethods = false,
            accountBalance = FiatValue.fromMinor(TEST_FIAT_ASSET, BigInteger.TEN),
            actionableBalance = FiatValue.fromMinor(TEST_FIAT_ASSET, BigInteger.TEN),
        )

        // Act
        subject.stateAwareActions
            .test()
            .assertValue {
                it.size == 1 &&
                    it.first().action == AssetAction.ViewActivity &&
                    it.first().state == ActionState.Available
            }
    }

    @Test
    fun `If can transact with banks but has no withdrawable balance withdraw is not available`() {
        // Arrange
        val subject = configureActionSubject()

        configureActionTest(
            canTransactWithBankMethods = true,
            accountBalance = FiatValue.fromMinor(TEST_FIAT_ASSET, BigInteger.TEN),
            actionableBalance = FiatValue.fromMinor(TEST_FIAT_ASSET, BigInteger.ZERO),
        )

        // Act
        subject.stateAwareActions
            .test()
            .assertValue {
                it.size == 2 &&
                    it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(1).action == AssetAction.FiatDeposit &&
                    it.elementAt(1).state == ActionState.Available
            }
    }

    @Test
    fun `If can transact with banks and has withdrawable balance withdraw is available`() {
        // Arrange
        val subject = configureActionSubject()

        configureActionTest(
            canTransactWithBankMethods = true,
            accountBalance = FiatValue.fromMinor(TEST_FIAT_ASSET, BigInteger.TEN),
            actionableBalance = FiatValue.fromMinor(TEST_FIAT_ASSET, BigInteger.TEN),
        )

        // Act
        subject.stateAwareActions
            .test()
            .assertValue {
                it.size == 3 &&
                    it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(1).action == AssetAction.FiatDeposit &&
                    it.elementAt(1).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.FiatWithdraw &&
                    it.elementAt(2).state == ActionState.Available
            }
    }

    private fun configureActionSubject(): FiatCustodialAccount =
        FiatCustodialAccount(
            label = "Test Account",
            currency = TEST_FIAT_ASSET,
            isDefault = false,
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            tradingService = tradingService,
            simpleBuyService = simpleBuyService,
            bankService = bankService
        )

    private fun configureActionTest(
        canTransactWithBankMethods: Boolean,
        accountBalance: FiatValue,
        actionableBalance: FiatValue,
        pendingBalance: FiatValue = FiatValue.zero(TEST_FIAT_ASSET),
    ) {

        whenever(bankService.canTransactWithBankMethods(TEST_FIAT_ASSET))
            .thenReturn(
                Single.just(canTransactWithBankMethods)
            )

        val balance = TradingAccountBalance(
            total = accountBalance,
            dashboardDisplay = accountBalance,
            withdrawable = actionableBalance,
            pending = pendingBalance,
            hasTransactions = true
        )

        whenever(tradingService.getBalanceFor(FiatCurrency.Dollars))
            .thenReturn(Observable.just(balance))
    }

    companion object {
        private val TEST_FIAT_ASSET = FiatCurrency.Dollars

        private val TEST_TO_USER_RATE = ExchangeRate(
            from = TEST_USER_FIAT,
            to = TEST_USER_FIAT,
            rate = 1.toBigDecimal()
        )
    }
}
