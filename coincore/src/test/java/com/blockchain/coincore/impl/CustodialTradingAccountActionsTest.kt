package com.blockchain.coincore.impl

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.testutil.CoinCoreFakeData
import com.blockchain.coincore.testutil.CoinCoreFakeData.TEST_API_FIAT
import com.blockchain.coincore.testutil.CoinCoreFakeData.TEST_USER_FIAT
import com.blockchain.coincore.testutil.USD
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class CustodialTradingAccountActionsTest : KoinTest {

    private val custodialManager: CustodialWalletManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val tradingService: TradingService = mock()
    private val userIdentity: UserIdentity = mock()
    private val kycService: KycService = mock()

    private val exchangeRates: ExchangeRatesDataManager = mock {
        on { getLastFiatToUserFiatRate(TEST_USER_FIAT) }.thenReturn(
            CoinCoreFakeData.userFiatToUserFiat
        )
        on { getLastFiatToUserFiatRate(TEST_API_FIAT) }.thenReturn(
            CoinCoreFakeData.TEST_TO_USER_RATE
        )
    }

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiatFlow(TEST_ASSET))
            .thenReturn(flowOf(DataResource.Data(TEST_TO_USER_RATE)))
    }

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            mockedDependenciesModule
        )
    }

    private val mockedDependenciesModule = module {
        factory {
            payloadDataManager
        }

        factory {
            mock<RemoteLogger>()
        }.bind(RemoteLogger::class)

        factory {
            tradingService
        }

        factory {
            mock<WalletModeService> {
                on { walletModeSingle }.thenReturn(Single.just(WalletMode.CUSTODIAL))
            }
        }
        factory {
            custodialManager
        }
        factory {
            userIdentity
        }
    }

    @Test
    fun `All actions are available`() {
        // Arrange
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, 1000.toBigInteger()),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            stakingEnabled = true,
            stakingDepositSupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )

        // Act
        subject.stateAwareActions
            .test()
            .await()
            .assertValue { actions ->
                actions.map { it.action } == listOf(
                    AssetAction.ViewActivity,
                    AssetAction.Send,
                    AssetAction.Sell,
                    AssetAction.InterestDeposit,
                    AssetAction.StakingDeposit,
                    AssetAction.Swap,
                    AssetAction.Receive,
                    AssetAction.Buy,
                    AssetAction.InterestWithdraw
                )
            }
    }

    @Test
    fun `If user is bronze then activity is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.BRONZE
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.ViewActivity }?.state == ActionState.LockedForTier
            }
    }

    @Test
    fun `If user is gold then activity is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.ViewActivity }?.state == ActionState.Available
            }
    }

    @Test
    fun `If asset deposit eligibility is blocked then Receive is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            cryptoDepositSupported = false,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Receive }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If all conditions met then receive is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Receive }?.state == ActionState.Available
            }
    }

    @Test
    fun `If not a supported currency then Buy is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If buy eligibility blocked then Buy is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = false,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If all criteria met then Buy is available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.zero(TEST_ASSET),
            actionableBalance = CryptoValue.zero(TEST_ASSET),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Buy }?.state == ActionState.Available
            }
    }

    @Test
    fun `If funded and has withdrawable balance, then Send is available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Send }?.state == ActionState.Available
            }
    }

    @Test
    fun `If not funded or no withdrawable balance, then Send is locked for balance`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Send }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If deposit to Interest is blocked then interest is not available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            interestDepositSupported = false,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If funded and eligible for interest then InterestDeposit is available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.Available
            }
    }

    @Test
    fun `If funded and not eligible for interest then InterestDeposit is locked for tier`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = false,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.LockedForTier
            }
    }

    @Test
    fun `If not funded and eligible for interest then InterestDeposit is locked for balance`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.InterestDeposit }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If Staking is blocked then StakingDeposit is not available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            stakingDepositSupported = false,
            stakingEnabled = false,
            interestDepositSupported = false,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.StakingDeposit }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If funded and eligible for staking then stakingdeposit is available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            stakingDepositSupported = true,
            stakingEnabled = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.StakingDeposit }?.state == ActionState.Available
            }
    }

    @Test
    fun `If not funded and eligible for staking then StakingDeposit is locked for balance`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            stakingDepositSupported = true,
            stakingEnabled = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.StakingDeposit }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If asset not supported for swap then Swap is not available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = false,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If swap eligibility is blocked then Swap is not available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            swapAccessAvailable = false,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If account has no balance then Swap is locked for balance`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If all criteria are met then Swap is available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Swap }?.state == ActionState.Available
            }
    }

    @Test
    fun `If Sell eligibility is allowed then Sell is unavailable`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            sellEligibilityAccess = false,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.Unavailable
            }
    }

    @Test
    fun `If no fiat accounts then Sell is locked for tier`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = emptyList(),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.BRONZE
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.LockedForTier
            }
    }

    @Test
    fun `If account is not funded then Sell is locked for balance`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `If all criteria are met then Sell is available`() {
        val subject = configureActionSubject()

        configureActionTest(
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            actionableBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            interest = true,
            supportedFiat = listOf(USD),
            buySupported = true,
            swapSupported = true,
            userTier = KycTier.GOLD
        )
        subject.stateAwareActions
            .test()
            .await()
            .assertValue {
                it.find { it.action == AssetAction.Sell }?.state == ActionState.Available
            }
    }

    private fun configureActionSubject(): CustodialTradingAccount =
        CustodialTradingAccount(
            currency = TEST_ASSET,
            label = "Test Account",
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            tradingService = tradingService,
            identity = userIdentity,
            kycService = kycService,
            walletModeService = mock {
                on { walletModeSingle }.thenReturn(Single.just(WalletMode.CUSTODIAL))
            }
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
        stakingDepositSupported: Boolean = true,
        stakingEnabled: Boolean = true,
        swapSupported: Boolean,
        userTier: KycTier
    ) {
        mockActionsFeatureAccess { original ->
            var updated = original
            val buyAccess = if (buySupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
            }
            updated = updated.plus(Feature.Buy to buyAccess)

            val cryptoDepositAccess = if (cryptoDepositSupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
            }

            updated = updated.plus(Feature.DepositCrypto to cryptoDepositAccess)

            val interestDepositAccess = if (interestDepositSupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
            }
            updated = updated.plus(Feature.DepositInterest to interestDepositAccess)

            val stakingDepositAccess = if (stakingDepositSupported) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
            }
            updated = updated.plus(Feature.DepositStaking to stakingDepositAccess)

            val swapAccess = if (swapAccessAvailable) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
            }
            updated = updated.plus(Feature.Swap to swapAccess)

            val sellAccess = if (sellEligibilityAccess) {
                FeatureAccess.Granted()
            } else {
                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
            }
            updated = updated.plus(Feature.Sell to sellAccess)

            return@mockActionsFeatureAccess updated
        }

        whenever(custodialManager.isCurrencyAvailableForTradingLegacy(TEST_ASSET)).thenReturn(Single.just(buySupported))

        val interestFeature = Feature.Interest(TEST_ASSET)
        whenever(userIdentity.isEligibleFor(eq(interestFeature), any())).thenReturn(Single.just(interest))

        whenever(userIdentity.userAccessForFeature(eq(Feature.DepositStaking), any())).thenReturn(
            Single.just(
                if (stakingEnabled) FeatureAccess.Granted() else FeatureAccess.Blocked(BlockedReason.NotEligible(""))
            )
        )

        val balance = TradingAccountBalance(
            total = accountBalance,
            withdrawable = actionableBalance,
            pending = pendingBalance,
            hasTransactions = true
        )
        whenever(tradingService.getBalanceFor(any(), any()))
            .thenReturn(Observable.just(balance))

        whenever(
            custodialManager.getSupportedFundsFiats(
                fiatCurrency = anyOrNull(),
                freshnessStrategy = any()
            )
        )
            .thenReturn(flowOf(supportedFiat))

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

        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(userTier))
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
        features.forEach {
            whenever(userIdentity.userAccessForFeature(eq(it), any()))
                .thenReturn(Single.just(updatedAccess[it]!!))
        }
    }

    companion object {

        private val TEST_ASSET = object : CryptoCurrency(
            displayTicker = "NOPE",
            networkTicker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.TRADING),
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
