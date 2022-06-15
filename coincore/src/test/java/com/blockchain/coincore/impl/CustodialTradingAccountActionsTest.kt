package com.blockchain.coincore.impl

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.testutil.USD
import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.extensions.minus
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
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
    fun `If no base Actions set then action set is unavailable`() {
        // Arrange
        val subject = configureActionSubject(emptySet())

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = false,
            supportedFiat = listOf(USD),
            buySupported = false,
            swapSupported = false,
            userTier = Tier.GOLD
        )

        // Act
        subject.stateAwareActions
            .test()
            .assertValue {
                !it.any { it.state == ActionState.Available }
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
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )

        // Act
        subject.stateAwareActions
            .test()
            .assertValue {
                var allMatch = true
                it.map { it.action }.map {
                    if (!SUPPORTED_CUSTODIAL_ACTIONS.contains(it)) {
                        allMatch = false
                    }
                }
                allMatch
            }
    }

    @Test
    fun `If user is bronze then activity is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.BRONZE
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.ViewActivity }?.state == ActionState.LockedForTier
            }
    }

    @Test
    fun `If user is gold then activity is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.ViewActivity }?.state == ActionState.Available
            }
    }

    @Test
    fun `If base actions don't contain activity then activity is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.ViewActivity })

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.ViewActivity }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If base actions don't contain receive then receive is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.Receive })

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Receive }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If asset deposit eligibility is blocked then Receive is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            cryptoDepositSupported = false,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Receive }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If all conditions met then receive is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Receive }?.state == ActionState.Available
            }
    }

    @Test
    fun `If base actions don't contain Buy then Buy is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.Buy })

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If not a supported currency then Buy is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If buy eligibility blocked then Buy is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If all criteria met then Buy is available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Available
            }
    }

    @Test
    fun `If base actions don't contain Send, then Send is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.Send })

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Send }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If funded and has withdrawable balance, then Send is available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Send }?.state == ActionState.Available
            }
    }

    @Test
    fun `If not funded or no withdrawable balance, then Send is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Send }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If deposit to Interest is blocked then interest is not available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            interestDepositSupported = false,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If base actions don't contain InterestDeposit then interest is not available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.InterestDeposit })

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            interestDepositSupported = false,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If funded and eligible for interest then InterestDeposit is available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.Available
            }
    }

    @Test
    fun `If funded and not eligible for interest then InterestDeposit is locked for tier`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = false,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.LockedForTier
            }
    }

    @Test
    fun `If not funded and eligible for interest then InterestDeposit is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If base actions don't contain Swap then Swap is not available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.Swap })

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If asset not supported for swap then Swap is not available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = false,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If swap eligibility is blocked then Swap is not available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            swapAccessAvailable = false,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If account has no balance then Swap is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If all criteria are met then Swap is available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Available
            }
    }

    @Test
    fun `If base actions dont contain Sell then Sell is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS.minus { it == AssetAction.Sell })

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If Sell eligibility is allowed then Sell is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            sellEligibilityAccess = false,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If no fiat accounts then Sell is locked for tier`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = emptyList(),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.BRONZE
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.LockedForTier
            }
    }

    @Test
    fun `If account is not funded then Sell is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.LockedForBalance
            }
    }
    @Test
    fun `If all criteria are met then Sell is available`() {
        val subject = configureActionSubject(SUPPORTED_CUSTODIAL_ACTIONS)

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = Tier.GOLD
        )
        subject.stateAwareActions
            .test()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.Available
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
        buySupported: Boolean,
        cryptoDepositSupported: Boolean = true,
        interestDepositSupported: Boolean = true,
        swapAccessAvailable: Boolean = true,
        sellEligibilityAccess: Boolean = true,
        swapSupported: Boolean,
        userTier: Tier
    ) {
        mockActionsFeatureAccess { original ->
            var updated = original
            val buyAccess = if (buySupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible)
            }
            updated = updated.plus(Feature.Buy to buyAccess)

            val cryptoDepositAccess = if (cryptoDepositSupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible)
            }

            updated = updated.plus(Feature.DepositCrypto to cryptoDepositAccess)

            val interestDepositAccess = if (interestDepositSupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible)
            }
            updated = updated.plus(Feature.DepositInterest to interestDepositAccess)

            val swapAccess = if (swapAccessAvailable) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible)
            }
            updated = updated.plus(Feature.Swap to swapAccess)

            val sellAccess = if (sellEligibilityAccess) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible)
            }
            updated = updated.plus(Feature.Sell to sellAccess)

            return@mockActionsFeatureAccess updated
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

        whenever(custodialManager.isAssetSupportedForSwap(TEST_ASSET))
            .thenReturn(Single.just(swapSupported))

        whenever(custodialManager.getSupportedBuySellCryptoCurrencies())
            .thenReturn(
                Single.just(
                    if (buySupported) {
                        listOf(
                            CurrencyPair(TEST_ASSET, mock())
                        )
                    } else {
                        emptyList()
                    }
                )
            )

        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(userTier))
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
