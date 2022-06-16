package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
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
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.testutils.rxInit
import com.blockchain.walletmode.WalletMode
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
    private val actionsComparator: StateAwareActionsComparator = mock()

    private val prices: Prices24HrWithDelta = mock {
        on { currentRate }.thenReturn(ExchangeRate(BigDecimal.ONE, assetInfo, FiatCurrency.Dollars))
    }
    private val asset: CryptoAsset = mock {
        on { this.assetInfo }.thenReturn(assetInfo)
        on { accountGroup(AssetFilter.NonCustodial) }.thenReturn(Maybe.just(nonCustodialGroup))
        on { accountGroup(AssetFilter.Trading) }.thenReturn(Maybe.just(custodialGroup))
        on { accountGroup(AssetFilter.Interest) }.thenReturn(Maybe.just(interestGroup))
        on { getPricesWith24hDelta() }.thenReturn(Single.just(prices))
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
            custodialWalletManager = custodialWalletManager,
            watchlistDataManager = watchlistDataManager,
            assetActionsComparator = actionsComparator,
            assetsManager = assetManager,
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )
    }

    @Test
    fun `load recurring buys should call endpoint`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        whenever(tradeDataService.getRecurringBuysForAsset(asset.assetInfo)).thenReturn(Single.just(emptyList()))
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(asset.assetInfo)).thenReturn(Single.just(true))
        val test = subject.loadRecurringBuys(asset.assetInfo).test()
        test.assertValue(Pair(emptyList(), true))
        verify(tradeDataService).getRecurringBuysForAsset(asset.assetInfo)
    }

    @Test
    fun `load quick actions should return valid actions for gold user with balance`() {
        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(true))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.DepositCrypto))
            .thenReturn(Single.just(FeatureAccess.Granted(mock())))

        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(CryptoCurrency.BTC)
        }
        val btcAsset = CryptoCurrency.BTC
        val account: CustodialTradingAccount = mock()
        val totalCryptoBalance =
            hashMapOf<AssetFilter, Money>(AssetFilter.Trading to CryptoValue.fromMajor(btcAsset, BigDecimal.TEN))
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(btcAsset)).thenReturn(Single.just(true))

        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.startAction == QuickActionCta.Sell &&
                it.endAction == QuickActionCta.Buy &&
                it.actionableAccount == account
        }

        verify(identity).getHighestApprovedKycTier()
        verify(identity).isEligibleFor(Feature.SimplifiedDueDiligence)
        verify(identity).userAccessForFeature(Feature.Buy)
        verify(identity).userAccessForFeature(Feature.Sell)
        verify(identity).userAccessForFeature(Feature.DepositCrypto)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `load quick actions should return valid actions for sdd user with no balance`() {
        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.SILVER))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(true))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.DepositCrypto))
            .thenReturn(Single.just(FeatureAccess.Granted(mock())))

        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(CryptoCurrency.BTC)
        }
        val btcAsset = CryptoCurrency.BTC
        val account: CustodialTradingAccount = mock()
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(btcAsset)).thenReturn(Single.just(true))

        val totalCryptoBalance =
            hashMapOf<AssetFilter, Money>(AssetFilter.Trading to CryptoValue.zero(btcAsset))
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.startAction == QuickActionCta.Receive &&
                it.endAction == QuickActionCta.Buy &&
                it.actionableAccount == account
        }

        verify(identity).getHighestApprovedKycTier()
        verify(identity).isEligibleFor(Feature.SimplifiedDueDiligence)
        verify(identity).userAccessForFeature(Feature.Buy)
        verify(identity).userAccessForFeature(Feature.Sell)
        verify(identity).userAccessForFeature(Feature.DepositCrypto)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `load quick actions should return valid actions for non sdd silver user`() {
        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.SILVER))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(false))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.DepositCrypto))
            .thenReturn(Single.just(FeatureAccess.Granted(mock())))

        val btcAsset = CryptoCurrency.BTC
        val totalCryptoBalance =
            hashMapOf<AssetFilter, Money>(AssetFilter.Trading to CryptoValue.fromMajor(btcAsset, BigDecimal.TEN))

        val account: CustodialTradingAccount = mock()
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(btcAsset)).thenReturn(Single.just(true))
        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.startAction == QuickActionCta.Receive &&
                it.endAction == QuickActionCta.Buy &&
                it.actionableAccount == account
        }

        verify(identity).getHighestApprovedKycTier()
        verify(identity).isEligibleFor(Feature.SimplifiedDueDiligence)
        verify(identity).userAccessForFeature(Feature.Buy)
        verify(identity).userAccessForFeature(Feature.Sell)
        verify(identity).userAccessForFeature(Feature.DepositCrypto)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `load quick actions should return valid actions when no custodial wallet and no buy access`() {
        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(true))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Blocked(mock())))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.DepositCrypto))
            .thenReturn(Single.just(FeatureAccess.Granted(mock())))

        val btcAsset = CryptoCurrency.BTC
        val totalCryptoBalance =
            hashMapOf<AssetFilter, Money>(AssetFilter.NonCustodial to CryptoValue.fromMajor(btcAsset, BigDecimal.TEN))

        val account: CryptoNonCustodialAccount = mock()
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(btcAsset)).thenReturn(Single.just(true))

        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.startAction == QuickActionCta.Receive &&
                it.endAction == QuickActionCta.Send &&
                it.actionableAccount == account
        }

        verify(identity).getHighestApprovedKycTier()
        verify(identity).isEligibleFor(Feature.SimplifiedDueDiligence)
        verify(identity).userAccessForFeature(Feature.Buy)
        verify(identity).userAccessForFeature(Feature.Sell)
        verify(identity).userAccessForFeature(Feature.DepositCrypto)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `load quick actions should return valid actions when not a supported pair and no balance`() {
        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(true))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.DepositCrypto))
            .thenReturn(Single.just(FeatureAccess.Granted(mock())))

        val btcAsset = CryptoCurrency.BTC
        val totalCryptoBalance =
            hashMapOf<AssetFilter, Money>(AssetFilter.Trading to CryptoValue.fromMajor(btcAsset, BigDecimal.ZERO))

        val account: CryptoNonCustodialAccount = mock()
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(btcAsset)).thenReturn(Single.just(false))

        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.startAction == QuickActionCta.Receive &&
                it.endAction == QuickActionCta.None &&
                it.actionableAccount == account
        }

        verify(identity).getHighestApprovedKycTier()
        verify(identity).isEligibleFor(Feature.SimplifiedDueDiligence)
        verify(identity).userAccessForFeature(Feature.Buy)
        verify(identity).userAccessForFeature(Feature.Sell)
        verify(identity).userAccessForFeature(Feature.DepositCrypto)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `load quick actions should return valid actions when not a supported pair and has balance`() {
        whenever(identity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(identity.isEligibleFor(Feature.SimplifiedDueDiligence)).thenReturn(Single.just(true))
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.Sell)).thenReturn(Single.just(FeatureAccess.Granted(mock())))
        whenever(identity.userAccessForFeature(Feature.DepositCrypto))
            .thenReturn(Single.just(FeatureAccess.Granted(mock())))

        val btcAsset = CryptoCurrency.BTC
        val totalCryptoBalance =
            hashMapOf<AssetFilter, Money>(
                AssetFilter.Trading to CryptoValue.fromMajor(btcAsset, BigDecimal.TEN),
                AssetFilter.NonCustodial to CryptoValue.fromMajor(btcAsset, BigDecimal.TEN)
            )

        val account: CryptoNonCustodialAccount = mock()
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(btcAsset)).thenReturn(Single.just(false))

        val test = subject.loadQuickActions(totalCryptoBalance, listOf(account), asset).test()

        test.assertValue {
            it.startAction == QuickActionCta.Receive &&
                it.endAction == QuickActionCta.Send &&
                it.actionableAccount == account
        }

        verify(identity).getHighestApprovedKycTier()
        verify(identity).isEligibleFor(Feature.SimplifiedDueDiligence)
        verify(identity).userAccessForFeature(Feature.Buy)
        verify(identity).userAccessForFeature(Feature.Sell)
        verify(identity).userAccessForFeature(Feature.DepositCrypto)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `load account details when asset is non tradeable`() {
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(FiatCurrency.Dollars)
        val asset: CryptoAsset = mock {
            on { this.assetInfo }.thenReturn(assetInfo)
            on { accountGroup(AssetFilter.NonCustodial) }.thenReturn(Maybe.empty())
            on { accountGroup(AssetFilter.Trading) }.thenReturn(Maybe.empty())
            on { accountGroup(AssetFilter.Interest) }.thenReturn(Maybe.empty())
            on { getPricesWith24hDelta() }.thenReturn(Single.just(prices))
            on { interestRate() }.thenReturn(Single.just(5.0))
        }
        whenever(watchlistDataManager.isAssetInWatchlist(asset.assetInfo)).thenReturn(Single.just(true))

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
    fun `when CheckBuyStatus then show userCanBuy Granted`() {
        whenever(identity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(FeatureAccess.Granted()))

        val test = subject.checkIfUserCanBuy().test()

        test.assertValue {
            it == FeatureAccess.Granted()
        }

        verify(identity).userAccessForFeature(Feature.Buy)

        verifyNoMoreInteractions(identity)
    }

    @Test
    fun `getting actions when account interest account is enabled or funded should show deposit`() {
        val actions = setOf<StateAwareAction>()
        whenever(dashboardPrefs.isRewardsIntroSeen).thenReturn(true)
        val account: CryptoInterestAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountActionSheet &&
                it.actions.find { it.action == AssetAction.InterestDeposit } != null
        }
    }

    @Test
    fun `getting actions when account is not interest account should not show deposit`() {
        val actions = setOf<StateAwareAction>()
        whenever(dashboardPrefs.isPrivateKeyIntroSeen).thenReturn(true)
        val account: CryptoNonCustodialAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountActionSheet &&
                it.actions.find { it.action == AssetAction.InterestWithdraw } == null &&
                it.actions.find { it.action == AssetAction.InterestDeposit } == null
        }
    }

    @Test
    fun `getting explainer sheet should work`() {
        val actions = setOf<StateAwareAction>()
        whenever(dashboardPrefs.isPrivateKeyIntroSeen).thenReturn(false)
        val account: CryptoNonCustodialAccount = mock {
            on { stateAwareActions }.thenReturn(Single.just(actions))
            on { isFunded }.thenReturn(true)
            on { balance }.thenReturn(Observable.just(AccountBalance.zero(assetInfo)))
        }

        val test = subject.getAccountActions(account).test()
        test.assertValue {
            it is CoinViewViewState.ShowAccountExplainerSheet &&
                it.actions.find { it.action == AssetAction.InterestDeposit } == null
        }
    }
}
