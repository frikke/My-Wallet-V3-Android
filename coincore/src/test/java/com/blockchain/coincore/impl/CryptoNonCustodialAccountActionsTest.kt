package com.blockchain.coincore.impl

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.testutil.CoinCoreFakeData.TEST_API_FIAT
import com.blockchain.coincore.testutil.CoinCoreFakeData.TEST_ASSET
import com.blockchain.coincore.testutil.CoinCoreFakeData.TEST_TO_USER_RATE
import com.blockchain.coincore.testutil.CoinCoreFakeData.TEST_USER_FIAT
import com.blockchain.coincore.testutil.CoinCoreFakeData.userFiatToUserFiat
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class CryptoNonCustodialAccountActionsTest : KoinTest {

    private val custodialManager: CustodialWalletManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val userIdentity: UserIdentity = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock {
        on { getLastFiatToUserFiatRate(TEST_USER_FIAT) }.thenReturn(userFiatToUserFiat)
        on { getLastFiatToUserFiatRate(TEST_API_FIAT) }.thenReturn(TEST_TO_USER_RATE)
    }

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiat(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
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
            mock<WalletModeService> {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
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
    fun `when base actions contains receive and account is not archived then Receive is available`() {
        val subject = configureActionSubject()

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Available, AssetAction.Receive))
            }
    }

    @Test
    fun `when base actions contains send and account is funded then Send is available`() {
        val subject = configureActionSubject()

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Available, AssetAction.Send))
            }
    }

    @Test
    fun `when base actions contains send and account is not funded then Send is locked for balance`() {
        val subject = configureActionSubject(false)

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.LockedForBalance, AssetAction.Send))
            }
    }

    @Test
    fun `when base actions contains swap but asset not available for swap then Swap is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(isAssetSupportedForSwap = false)

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.Swap))
            }
    }

    @Test
    fun `when base actions contains swap but user not eligible for swap then Swap is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(userAccessForSwap = FeatureAccess.Blocked(BlockedReason.NotEligible(null)))
        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.Swap))
            }
    }

    @Test
    fun `when base actions contains swap but account not funded then Swap is locked for balance`() {
        val subject = configureActionSubject(false)

        configureActionTest()

        subject.stateAwareActions.test().await().assertValue {
            it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                it.contains(StateAwareAction(ActionState.LockedForBalance, AssetAction.Swap))
        }
    }

    @Test
    fun `when base actions contains swap and all other criteria met then Swap is available`() {
        val subject = configureActionSubject(true)

        configureActionTest()

        subject.stateAwareActions.test().await().assertValue {
            it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                it.contains(StateAwareAction(ActionState.Available, AssetAction.Swap))
        }
    }

    @Test
    fun `when base actions contains Sell but feature is blocked then Sell is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(userAccessForSell = FeatureAccess.Blocked(BlockedReason.NotEligible(null)))

        subject.stateAwareActions.test().await().assertValue {
            it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.Sell))
        }
    }

    @Test
    fun `when base actions contains Sell but fiat accounts are empty then Sell is locked for tier`() {
        val subject = configureActionSubject(true)

        configureActionTest(supportedFiatFunds = emptyList())

        subject.stateAwareActions.test().await().assertValue {
            it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                it.contains(StateAwareAction(ActionState.LockedForTier, AssetAction.Sell))
        }
    }

    @Test
    fun `when base actions contains Sell but account has no funds then Sell is locked for balance`() {
        val subject = configureActionSubject(false)

        configureActionTest()
        subject.stateAwareActions.test().await().assertValue {
            it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                it.contains(StateAwareAction(ActionState.LockedForBalance, AssetAction.Sell))
        }
    }

    @Test
    fun `when base actions contains Sell and all other criteria met then Sell is available`() {
        val subject = configureActionSubject(true)

        configureActionTest()

        subject.stateAwareActions.test().await().assertValue {
            it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                it.contains(StateAwareAction(ActionState.Available, AssetAction.Sell))
        }
    }

    @Test
    fun `when base actions contains InterestDeposit but deposit crypto is blocked then InterestDeposit is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(userAccessForCryptoDeposit = FeatureAccess.Blocked(BlockedReason.NotEligible(null)))

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.InterestDeposit))
            }
    }

    @Test
    fun `when base actions contains StakingDeposit but staking crypto is blocked then InterestDeposit is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(userAccessForStakingDeposit = FeatureAccess.Blocked(BlockedReason.NotEligible(null)))

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains StakingDeposit but account not funded then StakingDeposit is locked for balance`() {
        val subject = configureActionSubject(false)

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.LockedForBalance, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains StakingDeposit and all other criteria met then StakingDeposit is available`() {
        val subject = configureActionSubject(true)

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Available, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains StakingDeposit but deposit crypto is blocked then StakingDeposit is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(userAccessForCryptoDeposit = FeatureAccess.Blocked(BlockedReason.NotEligible(null)))

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains StakingDeposit but staking is blocked then StakingDeposit is unavailable`() {
        val subject = configureActionSubject(true)

        configureActionTest(userAccessForStakingDeposit = FeatureAccess.Blocked(BlockedReason.NotEligible(null)))

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Unavailable, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains StakingDeposit but user not eligible for staking then StakingDeposit is locked for tier`() {
        val subject = configureActionSubject(true)

        configureActionTest(
            userAccessForStakingDeposit = FeatureAccess.Blocked(BlockedReason.InsufficientTier.Tier2Required)
        )

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.LockedForTier, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains InterestDeposit but account not funded then StakingDeposit is locked for balance`() {
        val subject = configureActionSubject(false)

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.LockedForBalance, AssetAction.StakingDeposit))
            }
    }

    @Test
    fun `when base actions contains InterestDeposit and all other criteria met then InterestDeposit is available`() {
        val subject = configureActionSubject(true)

        configureActionTest()

        subject.stateAwareActions
            .test().await().assertValue {
                it.contains(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)) &&
                    it.contains(StateAwareAction(ActionState.Available, AssetAction.InterestDeposit))
            }
    }

    private fun configureActionSubject(
        isFunded: Boolean = true,
    ): CryptoNonCustodialAccount =
        NonCustodialTestAccount(
            label = "Test Account",
            currency = TEST_ASSET,
            exchangeRates = exchangeRates,
            activity = Single.just(emptyList()),
            receiveAddress = mock(),
            isDefault = true,
            addressResolver = mock(),
            isFunded = isFunded
        )

    private fun configureActionTest(
        userAccessForSwap: FeatureAccess = FeatureAccess.Granted(),
        userAccessForSell: FeatureAccess = FeatureAccess.Granted(),
        userAccessForInterestDeposit: FeatureAccess = FeatureAccess.Granted(),
        userAccessForCryptoDeposit: FeatureAccess = FeatureAccess.Granted(),
        userAccessForStakingDeposit: FeatureAccess = FeatureAccess.Granted(),
        eligibleForInterest: Boolean = true,
        supportedFiatFunds: List<FiatCurrency> = listOf(FiatCurrency.Dollars),
        isAssetSupportedForSwap: Boolean = true,
    ) {
        whenever(custodialManager.selectedFiatcurrency).thenReturn(FiatCurrency.Dollars)
        whenever(custodialManager.getSupportedFundsFiats(FiatCurrency.Dollars)).thenReturn(
            flowOf(supportedFiatFunds)
        )
        whenever(userIdentity.isEligibleFor(Feature.Interest(TEST_ASSET))).thenReturn(Single.just(eligibleForInterest))
        whenever(userIdentity.userAccessForFeature(Feature.Swap)).thenReturn(Single.just(userAccessForSwap))
        whenever(userIdentity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(userAccessForSell))
        whenever(userIdentity.userAccessForFeature(Feature.DepositInterest)).thenReturn(
            Single.just(userAccessForInterestDeposit)
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositCrypto)).thenReturn(
            Single.just(userAccessForCryptoDeposit)
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositStaking)).thenReturn(
            Single.just(userAccessForStakingDeposit)
        )
        whenever(custodialManager.isAssetSupportedForSwapLegacy(TEST_ASSET)).thenReturn(
            Single.just(isAssetSupportedForSwap)
        )
    }
}

private class NonCustodialTestAccount(
    override val label: String,
    override val activity: Single<ActivitySummaryList>,
    override val receiveAddress: Single<ReceiveAddress>,
    override val isDefault: Boolean,
    override val exchangeRates: ExchangeRatesDataManager,
    override val addressResolver: AddressResolver,
    override val isFunded: Boolean,
    currency: AssetInfo,
) : CryptoNonCustodialAccount(currency) {

    override fun getOnChainBalance(): Observable<Money> =
        Observable.just(Money.zero(currency))

    override val index: Int
        get() = 1

    override suspend fun publicKey(): List<String> {
        return listOf("")
    }

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine = mock()
}
