package com.blockchain.coincore.impl

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.testutil.USD
import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class CustodialTradingAccountActionsTest : CoincoreTestBase() {

    private val custodialManager: CustodialWalletManager = mock()
    private val tradingBalances: TradingBalanceDataManager = mock()
    private val identity: UserIdentity = mock()

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiat(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
    }

    @Test
    fun `If no base Actions set then action set is empty`() {

        // Arrange
        val subject = configureActionSubject(emptySet())

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it.isEmpty()
            }
    }

    @Test
    fun `All actions are available`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == SUPPORTED_CUSTODIAL_ACTIONS
            }
    }

    @Test
    fun `If user has no balance and all default Actions set then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.Receive,
                    AssetAction.Buy
                )
            }
    }

    @Test
    fun `If user has actionable balance and all default Actions then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.InterestDeposit,
                    AssetAction.Swap,
                    AssetAction.Sell,
                    AssetAction.Receive,
                    AssetAction.Buy
                )
            }
    }

    @Test
    fun `If user has balance and all default Actions, but interest is disabled then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = false,
            supportedFiat = listOf(USD),
            buySupported = true
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.Swap,
                    AssetAction.Sell,
                    AssetAction.Receive,
                    AssetAction.Buy
                )
            }
    }

    @Test
    fun `If user has actionable balance and all default Actions, but no fiat then action set is correct`() {

        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = true,
            supportedFiat = emptyList(),
            buySupported = true
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.InterestDeposit,
                    AssetAction.Swap,
                    AssetAction.Buy,
                    AssetAction.Receive,
                    AssetAction.Send
                )
            }
    }

    @Test
    fun `If user has actionable balance and all default Actions, but no crypto buy pair then buy action should not be present`() {
        // Arrange
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                !it.contains(AssetAction.Buy)
            }
    }

    @Test
    fun `Unsupported actions are ignored`() {

        // Arrange
        val subject = configureActionSubject(
            setOf(
                AssetAction.ViewActivity,
                AssetAction.Send,
                AssetAction.ViewStatement
            )
        )

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false
        )

        // Act
        subject.actions
            .test()
            .assertValue {
                it == setOf(
                    AssetAction.ViewActivity,
                    AssetAction.Send
                )
            }
    }

    private fun configureActionSubject(actions: Set<AssetAction>): CustodialTradingAccount =
        CustodialTradingAccount(
            currency = TEST_ASSET,
            label = "Test Account",
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            tradingBalances = tradingBalances,
            identity = identity,
            baseActions = actions
        )

    private fun configureActionTest(
        accountBalance: CryptoValue,
        actionableBalance: CryptoValue,
        pendingBalance: CryptoValue = CryptoValue.zero(TEST_ASSET),
        interest: Boolean,
        supportedFiat: List<FiatCurrency>,
        buySupported: Boolean
    ) {
        mockActionsFeatureAccess { original ->
            val buyAccess = if (buySupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible)
            }
            original.plus(Feature.Buy to buyAccess)
        }

        whenever(custodialManager.isCurrencyAvailableForTrading(TEST_ASSET)).thenReturn(Single.just(buySupported))

        val interestFeature = Feature.Interest(TEST_ASSET)
        whenever(identity.isEligibleFor(interestFeature)).thenReturn(Single.just(interest))

        val balance = TradingAccountBalance(
            total = accountBalance,
            withdrawable = actionableBalance,
            pending = pendingBalance,
            hasTransactions = true
        )
        whenever(tradingBalances.getBalanceForCurrency(TEST_ASSET))
            .thenReturn(Observable.just(balance))

        whenever(custodialManager.getSupportedFundsFiats())
            .thenReturn(Single.just(supportedFiat))
    }

    private fun mockActionsFeatureAccess(
        block: (original: Map<Feature, FeatureAccess>) -> Map<Feature, FeatureAccess>
    ) {
        val features = listOf(
            Feature.Buy,
            Feature.Swap,
            Feature.Sell,
            Feature.DepositCrypto,
            Feature.DepositInterest
        )
        val access = features.associateWith { FeatureAccess.Granted() }

        val updatedAccess = block(access)
        whenever(identity.userAccessForFeatures(features))
            .thenReturn(Single.just(updatedAccess))
    }

    companion object {
        private val SUPPORTED_CUSTODIAL_ACTIONS = setOf(
            AssetAction.ViewActivity,
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Receive,
            AssetAction.Buy
        )

        private val TEST_ASSET = object : CryptoCurrency(
            displayTicker = "NOPE",
            networkTicker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}

        private val TEST_TO_USER_RATE = ExchangeRate(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = 1.2.toBigDecimal()
        )
    }
}
