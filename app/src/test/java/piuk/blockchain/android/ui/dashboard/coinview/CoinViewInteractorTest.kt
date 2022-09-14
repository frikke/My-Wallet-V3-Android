package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.testutils.rxInit
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator

class CoinViewInteractorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var subject: CoinViewInteractor
    private val coincore: Coincore = mock()
    private val tradeDataService: TradeDataService = mock()
    private val assetManager: DynamicAssetsDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val dashboardPrefs: DashboardPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val identity: UserIdentity = mock()
    private val kycService: KycService = mock()
    private val walletModeService: WalletModeService = mock() {
        on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
    }
    private val assetInfo: AssetInfo = object : CryptoCurrency(
        displayTicker = "BTC",
        networkTicker = "BTC",
        name = "Not a real thing",
        categories = setOf(),
        precisionDp = 8,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}
    private val watchlistDataManager: WatchlistDataManager = mock()

    private val defaultNcAccount: CryptoNonCustodialAccount = mock {
        on { isDefault }.thenReturn(true)
        on { label }.thenReturn("default nc account")
        on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        on { stateAwareActions }.thenReturn(Single.just(setOf()))
    }
    private val secondNcAccount: CryptoNonCustodialAccount = mock {
        on { isDefault }.thenReturn(false)
        on { label }.thenReturn("second nc account")
        on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        on { stateAwareActions }.thenReturn(Single.just(setOf()))
    }
    private val archivedNcAccount: CryptoNonCustodialAccount = mock {
        on { isDefault }.thenReturn(false)
        on { label }.thenReturn("second nc account")
        on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        on { stateAwareActions }.thenReturn(Single.just(setOf()))
        on { isArchived }.thenReturn(true)
    }
    private val custodialAccount: FiatCustodialAccount = mock {
        on { label }.thenReturn("default c account")
        on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        on { stateAwareActions }.thenReturn(Single.just(setOf()))
    }
    private val interestAccount: CryptoInterestAccount = mock {
        on { label }.thenReturn("default i account")
        on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        on { stateAwareActions }.thenReturn(Single.just(setOf()))
    }
    private val nonCustodialGroup: AccountGroup = mock {
        on { accounts }.thenReturn(listOf(defaultNcAccount, secondNcAccount, archivedNcAccount))
    }
    private val custodialGroup: AccountGroup = mock {
        on { accounts }.thenReturn(listOf(custodialAccount))
    }
    private val interestGroup: AccountGroup = mock {
        on { accounts }.thenReturn(listOf(interestAccount))
    }
    private val allGroups: AccountGroup = mock {
        on { accounts }.thenReturn(
            listOf(defaultNcAccount, secondNcAccount, archivedNcAccount, custodialAccount, interestAccount)
        )
    }
    private val actionsComparator: StateAwareActionsComparator = mock()

    private val prices: Prices24HrWithDelta = mock {
        on { currentRate }.thenReturn(ExchangeRate(BigDecimal.ONE, assetInfo, FiatCurrency.Dollars))
    }
    private val asset: CryptoAsset = mock {
        on { this.currency }.thenReturn(assetInfo)
        on { accountGroup(AssetFilter.NonCustodial) }.thenReturn(Maybe.just(nonCustodialGroup))
        on { accountGroup(AssetFilter.Trading) }.thenReturn(Maybe.just(custodialGroup))
        on { accountGroup(AssetFilter.Interest) }.thenReturn(Maybe.just(interestGroup))
        on { accountGroup(AssetFilter.All) }.thenReturn(Maybe.just(allGroups))
        on { getPricesWith24hDeltaLegacy() }.thenReturn(Single.just(prices))
        on { interestRate() }.thenReturn(Single.just(5.0))
    }

    @Before
    fun setUp() {
        subject = CoinViewInteractor(
            coincore = coincore,
            tradeDataService = tradeDataService,
            currencyPrefs = currencyPrefs,
            dashboardPrefs = dashboardPrefs,
            identity = identity,
            kycService = kycService,
            custodialWalletManager = custodialWalletManager,
            watchlistDataManager = watchlistDataManager,
            assetActionsComparator = actionsComparator,
            assetsManager = assetManager,
            walletModeService = walletModeService
        )
    }

    @Test
    fun `load recurring buys should call endpoint`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        whenever(tradeDataService.getRecurringBuysForAsset(asset.currency, FreshnessStrategy.Fresh)).thenReturn(flowOf(DataResource.Data(emptyList())))
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(asset.currency)).thenReturn(Single.just(true))
        val test = subject.loadRecurringBuys(asset.currency).test().await()
        test.assertValue(Pair(emptyList(), true))
        verify(tradeDataService).getRecurringBuysForAsset(asset.currency, FreshnessStrategy.Fresh)
    }

    private fun prepareQuickActionsCustodial(
        kycTier: KycTier,
        sdd: Boolean,
        buyAccess: FeatureAccess,
        sellAccess: FeatureAccess,
        availableForTrading: Boolean,
        supportedForSwap: Boolean
    ) {
        whenever(walletModeService.enabledWalletMode()).thenReturn(WalletMode.CUSTODIAL_ONLY)

        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(kycTier))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(sdd))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(buyAccess))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(sellAccess))
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(CryptoCurrency.BTC)).thenReturn(
            Single.just(availableForTrading)
        )
        whenever(custodialWalletManager.isAssetSupportedForSwap(CryptoCurrency.BTC)).thenReturn(
            Single.just(supportedForSwap)
        )
    }

    @Test
    fun `GIVEN custodial, kyc gold, eligible, has balance, WHEN loadQuickActions is called, THEN Swap(true) Sell(true) Buy(true)`() {
        prepareQuickActionsCustodial(
            kycTier = KycTier.GOLD,
            sdd = true,
            buyAccess = FeatureAccess.Granted(mock()),
            sellAccess = FeatureAccess.Granted(mock()),
            availableForTrading = true,
            supportedForSwap = true
        )

        val account: CustodialTradingAccount = mock()

        val totalCryptoBalance = hashMapOf(
            AssetFilter.Trading to CryptoValue.fromMajor(CryptoCurrency.BTC, BigDecimal.TEN)
        )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.middleAction == QuickActionCta.Swap(true) &&
                it.startAction == QuickActionCta.Sell(true) &&
                it.endAction == QuickActionCta.Buy(true) &&
                it.actionableAccount == account
        }
    }

    @Test
    fun `GIVEN custodial, kyc gold, not eligible buy sell, has zero balance, WHEN loadQuickActions is called, THEN Swap(false) Sell(false) Buy(false)`() {
        prepareQuickActionsCustodial(
            kycTier = KycTier.GOLD,
            sdd = true,
            buyAccess = FeatureAccess.Blocked(BlockedReason.NotEligible(null)),
            sellAccess = FeatureAccess.Blocked(mock()),
            availableForTrading = true,
            supportedForSwap = true
        )

        val account: CustodialTradingAccount = mock()

        val totalCryptoBalance = hashMapOf(
            AssetFilter.Custodial to CryptoValue.fromMajor(CryptoCurrency.BTC, BigDecimal.ZERO)
        )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.middleAction == QuickActionCta.Swap(false) &&
                it.startAction == QuickActionCta.Sell(false) &&
                it.endAction == QuickActionCta.Buy(false) &&
                it.actionableAccount == account
        }
    }

    @Test
    fun `GIVEN custodial, kyc gold, not eligible buy sell, has zero balance, no swap, WHEN loadQuickActions is called, THEN None Sell(false) Buy(false)`() {
        prepareQuickActionsCustodial(
            kycTier = KycTier.GOLD,
            sdd = true,
            buyAccess = FeatureAccess.Blocked(BlockedReason.NotEligible(null)),
            sellAccess = FeatureAccess.Blocked(mock()),
            availableForTrading = true,
            supportedForSwap = false
        )

        val account: CustodialTradingAccount = mock()

        val totalCryptoBalance = hashMapOf(
            AssetFilter.Custodial to CryptoValue.fromMajor(CryptoCurrency.BTC, BigDecimal.ZERO)
        )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.middleAction == QuickActionCta.None &&
                it.startAction == QuickActionCta.Sell(false) &&
                it.endAction == QuickActionCta.Buy(false) &&
                it.actionableAccount == account
        }
    }

    @Test
    fun `GIVEN non custodial, has balance, WHEN loadQuickActions is called, THEN Swap(true) Receive(true) Send(true)`() {
        whenever(walletModeService.enabledWalletMode()).thenReturn(WalletMode.NON_CUSTODIAL_ONLY)

        whenever(custodialWalletManager.isAssetSupportedForSwap(CryptoCurrency.BTC)).thenReturn(
            Single.just(true)
        )

        val account: CryptoNonCustodialAccount = mock()

        val totalCryptoBalance = hashMapOf(
            AssetFilter.NonCustodial to CryptoValue.fromMajor(CryptoCurrency.BTC, BigDecimal.TEN)
        )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.middleAction == QuickActionCta.Swap(true) &&
                it.startAction == QuickActionCta.Receive(true) &&
                it.endAction == QuickActionCta.Send(true) &&
                it.actionableAccount == account
        }
    }

    @Test
    fun `GIVEN non custodial, has zero balance, WHEN loadQuickActions is called, THEN Swap(true) Receive(true) Send(true)`() {
        whenever(walletModeService.enabledWalletMode()).thenReturn(WalletMode.NON_CUSTODIAL_ONLY)

        whenever(custodialWalletManager.isAssetSupportedForSwap(CryptoCurrency.BTC)).thenReturn(
            Single.just(true)
        )

        val account: CryptoNonCustodialAccount = mock()

        val totalCryptoBalance = hashMapOf(
            AssetFilter.NonCustodial to CryptoValue.fromMajor(CryptoCurrency.BTC, BigDecimal.ZERO)
        )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.middleAction == QuickActionCta.Swap(false) &&
                it.startAction == QuickActionCta.Receive(true) &&
                it.endAction == QuickActionCta.Send(false) &&
                it.actionableAccount == account
        }
    }

    @Test
    fun `GIVEN non custodial, has zero balance, no swap, WHEN loadQuickActions is called, THEN None Receive(true) Send(true)`() {
        whenever(walletModeService.enabledWalletMode()).thenReturn(WalletMode.NON_CUSTODIAL_ONLY)

        whenever(custodialWalletManager.isAssetSupportedForSwap(CryptoCurrency.BTC)).thenReturn(
            Single.just(false)
        )

        val account: CryptoNonCustodialAccount = mock()

        val totalCryptoBalance = hashMapOf(
            AssetFilter.NonCustodial to CryptoValue.fromMajor(CryptoCurrency.BTC, BigDecimal.ZERO)
        )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.middleAction == QuickActionCta.None &&
                it.startAction == QuickActionCta.Receive(true) &&
                it.endAction == QuickActionCta.Send(false) &&
                it.actionableAccount == account
        }
    }

    @Test
    fun `load account details when asset is non tradeable`() {
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(FiatCurrency.Dollars)
        val asset: CryptoAsset = mock {
            on { this.currency }.thenReturn(assetInfo)
            on { accountGroup(AssetFilter.All) }.thenReturn(Maybe.empty())
            on { getPricesWith24hDeltaLegacy() }.thenReturn(Single.just(prices))
            on { interestRate() }.thenReturn(Single.just(5.0))
        }
        whenever(watchlistDataManager.isAssetInWatchlist(asset.currency)).thenReturn(Single.just(true))

        val test = subject.loadAccountDetails(asset).test()

        test.assertValue {
            it.prices == prices &&
                it is AssetInformation.NonTradeable && it.isAddedToWatchlist
        }
    }

    @Test
    fun `load account details for tradeable asset should work`() {
        val testAsset = object : CryptoCurrency(
            displayTicker = "BTC",
            networkTicker = "BTC",
            name = "Not a real thing",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(FiatCurrency.Dollars)
        whenever(watchlistDataManager.isAssetInWatchlist(testAsset)).thenReturn(Single.just(true))

        val test = subject.loadAccountDetails(asset).test()

        test.assertValue {
            it.prices == prices &&
                it is AssetInformation.AccountsInfo &&
                it.totalCryptoBalance == hashMapOf(
                AssetFilter.All to Money.fromMajor(testAsset, BigDecimal.ZERO),
                AssetFilter.Interest to Money.fromMajor(testAsset, BigDecimal.ZERO),
                AssetFilter.NonCustodial to Money.fromMajor(testAsset, BigDecimal.ZERO),
                AssetFilter.Trading to Money.fromMajor(testAsset, BigDecimal.ZERO)
            ) &&
                it.totalFiatBalance == Money.fromMajor(FiatCurrency.Dollars, BigDecimal.ZERO) &&
                it.accountsList.size == 4 &&
                it.accountsList[0].account is CryptoNonCustodialAccount &&
                it.accountsList[0].account.label == "default nc account" &&
                it.accountsList[1].account is FiatCustodialAccount &&
                it.accountsList[1].account.label == "default c account" &&
                it.accountsList[2].account is CryptoInterestAccount &&
                it.accountsList[2].account.label == "default i account" &&
                it.accountsList[3].account is CryptoNonCustodialAccount &&
                it.accountsList[3].account.label == "second nc account" &&
                it.accountsList.firstOrNull {
                it.account is CryptoNonCustodialAccount &&
                    (it.account as CryptoNonCustodialAccount).isArchived
            } == null
        }
    }

    @Test
    fun `when buy is granted and asset is supported for trading, canBuy should be true`() {
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted()))
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any()))
            .thenReturn(Single.just(true))

        val currency: AssetInfo = mock()
        val asset: CryptoAsset = mock {
            on { this.currency }.thenReturn(currency)
        }

        val test = subject.isBuyOptionAvailable(asset).test()

        test.assertValue { canBuy ->
            canBuy
        }

        verify(identity).userAccessForFeature(Feature.Buy)
        verify(custodialWalletManager).isCurrencyAvailableForTradingLegacy(currency)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `when buy is granted and asset is not supported for trading, canBuy should be false`() {
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted()))
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any()))
            .thenReturn(Single.just(false))

        val currency: AssetInfo = mock()
        val asset: CryptoAsset = mock {
            on { this.currency }.thenReturn(currency)
        }

        val test = subject.isBuyOptionAvailable(asset).test()

        test.assertValue { canBuy ->
            canBuy.not()
        }

        verify(identity).userAccessForFeature(Feature.Buy)
        verify(custodialWalletManager).isCurrencyAvailableForTradingLegacy(currency)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `getting actions when account interest account is enabled or funded should show deposit`() {
        val actions = setOf<StateAwareAction>()
        whenever(dashboardPrefs.isRewardsIntroSeen).thenReturn(true)
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any())).thenReturn(Single.just(true))
        val account: CryptoInterestAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(asset, account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountActionSheet &&
                it.actions.find { it.action == AssetAction.InterestDeposit } != null
        }
    }

    @Test
    fun `getting actions when account is not interest account should not show deposit`() {
        val actions = setOf<StateAwareAction>()
        whenever(dashboardPrefs.isPrivateKeyIntroSeen).thenReturn(true)
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any())).thenReturn(Single.just(true))
        val account: CryptoNonCustodialAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(asset, account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountActionSheet &&
                it.actions.find { it.action == AssetAction.InterestWithdraw } == null &&
                it.actions.find { it.action == AssetAction.InterestDeposit } == null
        }
    }

    @Test
    fun `getting actions when pair is supported should not have sell disabled`() {
        val actions = setOf(StateAwareAction(ActionState.Available, AssetAction.Sell))
        whenever(dashboardPrefs.isPrivateKeyIntroSeen).thenReturn(true)
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any())).thenReturn(Single.just(true))
        val account: CryptoNonCustodialAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(asset, account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountActionSheet &&
                it.actions.find { it.action == AssetAction.Sell }?.state != ActionState.LockedDueToAvailability
        }
    }

    @Test
    fun `getting actions when pair is not supported should have sell disabled`() {
        val actions = setOf(StateAwareAction(ActionState.Available, AssetAction.Sell))
        whenever(dashboardPrefs.isPrivateKeyIntroSeen).thenReturn(true)
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any())).thenReturn(Single.just(false))
        val account: CryptoNonCustodialAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(asset, account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountActionSheet &&
                it.actions.find { it.action == AssetAction.Sell }?.state == ActionState.LockedDueToAvailability
        }
    }

    @Test
    fun `getting explainer sheet should work`() {
        val actions = setOf<StateAwareAction>()
        whenever(dashboardPrefs.isPrivateKeyIntroSeen).thenReturn(false)
        whenever(custodialWalletManager.isCurrencyAvailableForTradingLegacy(any())).thenReturn(Single.just(true))
        val account: CryptoNonCustodialAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(asset, account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountExplainerSheet &&
                it.actions.find { it.action == AssetAction.InterestDeposit } == null
        }
    }
}
