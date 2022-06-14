package com.blockchain.coincore.impl

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.extensions.minus
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class CryptoNonCustodialAccountActionsTest : CoincoreTestBase() {

    private val custodialManager: CustodialWalletManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val userIdentity: UserIdentity = mock()

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiat(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
    }

    @Test
    fun `when base actions does not contain receive then Receive is Unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS.minus { it == AssetAction.Receive })

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(1).action == AssetAction.Receive &&
                    it.elementAt(1).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains receive and account is not archived then Receive is available`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(1).action == AssetAction.Receive &&
                    it.elementAt(1).state == ActionState.Available
            }
    }

    @Test
    fun `when base actions contains send and account is funded then Send is available`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.Send &&
                    it.elementAt(2).state == ActionState.Available
            }
    }

    @Test
    fun `when base actions contains send and account is not funded then Send is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, false)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.Send &&
                    it.elementAt(2).state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `when base actions does not contain send and account is funded then Send is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS.minus { it == AssetAction.Send }, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.Send &&
                    it.elementAt(2).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions does not contain Swap then Swap is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS.minus { it == AssetAction.Swap }, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.Swap &&
                    it.elementAt(3).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains swap but asset not available for swap then Swap is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(isAssetSupportedForSwap = false)

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.Swap &&
                    it.elementAt(3).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains swap but user not eligible for swap then Swap is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(userAccessForSwap = FeatureAccess.Blocked(BlockedReason.NotEligible))

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.Swap &&
                    it.elementAt(3).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains swap but account not funded then Swap is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, false)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.Swap &&
                    it.elementAt(3).state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `when base actions contains swap and all other criteria met then Swap is available`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.Swap &&
                    it.elementAt(3).state == ActionState.Available
            }
    }

    @Test
    fun `when base actions does not contain Sell then Sell is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS.minus { it == AssetAction.Sell }, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(4).action == AssetAction.Sell &&
                    it.elementAt(4).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains Sell but feature is blocked then Sell is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(userAccessForSell = FeatureAccess.Blocked(BlockedReason.NotEligible))

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(4).action == AssetAction.Sell &&
                    it.elementAt(4).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains Sell but fiat accounts are empty then Sell is locked for tier`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(supportedFiatFunds = emptyList())

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(4).action == AssetAction.Sell &&
                    it.elementAt(4).state == ActionState.LockedForTier
            }
    }

    @Test
    fun `when base actions contains Sell but account has no funds then Sell is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, false)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(4).action == AssetAction.Sell &&
                    it.elementAt(4).state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `when base actions contains Sell and all other criteria met then Sell is available`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(4).action == AssetAction.Sell &&
                    it.elementAt(4).state == ActionState.Available
            }
    }

    @Test
    fun `when base actions does not contain InterestDeposit then InterestDeposit is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS.minus { it == AssetAction.InterestDeposit }, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(5).action == AssetAction.InterestDeposit &&
                    it.elementAt(5).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains InterestDeposit but deposit crypto is blocked then InterestDeposit is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(userAccessForCryptoDeposit = FeatureAccess.Blocked(BlockedReason.NotEligible))

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(5).action == AssetAction.InterestDeposit &&
                    it.elementAt(5).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains InterestDeposit but interest crypto is blocked then InterestDeposit is unavailable`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(userAccessForInterestDeposit = FeatureAccess.Blocked(BlockedReason.NotEligible))

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(5).action == AssetAction.InterestDeposit &&
                    it.elementAt(5).state == ActionState.Unavailable
            }
    }

    @Test
    fun `when base actions contains InterestDeposit but user not eligible for interest then InterestDeposit is locked for tier`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest(eligibleForInterest = false)

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(5).action == AssetAction.InterestDeposit &&
                    it.elementAt(5).state == ActionState.LockedForTier
            }
    }

    @Test
    fun `when base actions contains InterestDeposit but account not funded then InterestDeposit is locked for balance`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, false)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(5).action == AssetAction.InterestDeposit &&
                    it.elementAt(5).state == ActionState.LockedForBalance
            }
    }

    @Test
    fun `when base actions contains InterestDeposit and all other criteria met then InterestDeposit is available`() {
        val subject = configureActionSubject(SUPPORTED_NC_ACTIONS, true)

        configureActionTest()

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.ViewActivity &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(5).action == AssetAction.InterestDeposit &&
                    it.elementAt(5).state == ActionState.Available
            }
    }

    private fun configureActionSubject(
        baseActions: Set<AssetAction>,
        isFunded: Boolean = true
    ): CryptoNonCustodialAccount =
        NonCustodialTestAccount(
            label = "Test Account",
            currency = TEST_ASSET,
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            identity = userIdentity,
            activity = Single.just(emptyList()),
            receiveAddress = mock(),
            payloadDataManager = payloadDataManager,
            isDefault = true,
            baseActions = baseActions,
            addressResolver = mock(),
            isFunded = isFunded
        )

    private fun configureActionTest(
        userAccessForSwap: FeatureAccess = FeatureAccess.Granted(),
        userAccessForSell: FeatureAccess = FeatureAccess.Granted(),
        userAccessForInterestDeposit: FeatureAccess = FeatureAccess.Granted(),
        userAccessForCryptoDeposit: FeatureAccess = FeatureAccess.Granted(),
        eligibleForInterest: Boolean = true,
        supportedFiatFunds: List<FiatCurrency> = listOf(FiatCurrency.Dollars),
        isAssetSupportedForSwap: Boolean = true
    ) {
        whenever(custodialManager.selectedFiatcurrency).thenReturn(FiatCurrency.Dollars)
        whenever(custodialManager.getSupportedFundsFiats(FiatCurrency.Dollars)).thenReturn(
            Single.just(supportedFiatFunds)
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
        whenever(custodialManager.isAssetSupportedForSwap(TEST_ASSET)).thenReturn(Single.just(isAssetSupportedForSwap))
    }

    companion object {

        private val SUPPORTED_NC_ACTIONS = setOf(
            AssetAction.ViewActivity,
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Receive
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
            from = TEST_USER_FIAT,
            to = TEST_USER_FIAT,
            rate = 1.toBigDecimal()
        )
    }
}

private class NonCustodialTestAccount(
    override val label: String,
    override val activity: Single<ActivitySummaryList>,
    override val receiveAddress: Single<ReceiveAddress>,
    override val isDefault: Boolean,
    override val exchangeRates: ExchangeRatesDataManager,
    override val baseActions: Set<AssetAction>,
    override val addressResolver: AddressResolver,
    override val isFunded: Boolean,
    payloadDataManager: PayloadDataManager,
    currency: AssetInfo,
    custodialWalletManager: CustodialWalletManager,
    identity: UserIdentity
) : CryptoNonCustodialAccount(payloadDataManager, currency, custodialWalletManager, identity) {
    override fun getOnChainBalance(): Observable<Money> =
        Observable.just(Money.zero(currency))

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine = mock()
}
