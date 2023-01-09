package piuk.blockchain.android.ui.brokerage.sell

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.core.sell.domain.SellUserEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.utils.asFlow
import com.blockchain.utils.zipSingles
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx3.asFlow
import piuk.blockchain.android.ui.transfer.AccountsSorting

class SellRepository(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val kycService: KycService,
    private val coincore: Coincore,
    private val localSettingsPrefs: LocalSettingsPrefs,
    private val hideDustFlag: FeatureFlag,
    private val accountsSorting: AccountsSorting,
    private val simpleBuyService: SimpleBuyService,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs
) {
    private var sellEligibilityCache: DataResource<SellEligibility> = DataResource.Loading
    private var sellAvailableAccounts: Map<WalletMode, DataResource<List<CryptoAccount>>> =
        WalletMode.values().associateWith { DataResource.Loading }

    fun sellEligibility(): Flow<DataResource<SellEligibility>> =
        userFeaturePermissionService.getAccessForFeature(
            feature = Feature.Sell,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
        ).flatMapData { data ->
            checkUserEligibilityStatus(data)
        }.onEach {
            sellEligibilityCache = it
        }.onStart {
            emit(sellEligibilityCache)
        }

    fun sellSupportedAssets(
        availableAssets: List<AssetInfo>,
        walletMode: WalletMode
    ): Flow<DataResource<List<CryptoAccount>>> {
        return coincore.walletsWithAction(
            action = AssetAction.Sell,
            sorter = accountsSorting.sorter(),
            tickers = availableAssets.toSet()
        ).map { accountList ->
            accountList
                .filterUnsupportedPairs(availableAssets)
        }.zipWith(hideDustFlag.enabled.map { it && localSettingsPrefs.hideSmallBalancesEnabled })
            .flatMap { (accounts, shouldHideDust) ->
                if (shouldHideDust) {
                    accounts.filterDustBalances()
                } else {
                    Single.just(accounts.filterIsInstance<CryptoAccount>())
                }
            }.toObservable().map { DataResource.Data(it) as DataResource<List<CryptoAccount>> }.asFlow().onEach {
                sellAvailableAccounts = sellAvailableAccounts.plus(walletMode to it)
            }.onStart {
                emit(sellAvailableAccounts[walletMode] ?: DataResource.Loading)
            }
    }

    private fun checkUserEligibilityStatus(access: FeatureAccess?): Flow<DataResource<SellEligibility>> =
        when (val reason = (access as? FeatureAccess.Blocked)?.reason) {
            is BlockedReason.InsufficientTier,
            is BlockedReason.NotEligible,
            is BlockedReason.Sanctions -> flowOf(DataResource.Data(SellEligibility.NotEligible(reason)))
            is BlockedReason.TooManyInFlightTransactions,
            is BlockedReason.ShouldAcknowledgeStakingWithdrawal,
            null -> {
                loadSellEligibility().flatMapData { data ->
                    when (data) {
                        SellUserEligibility.KycRejectedUser,
                        SellUserEligibility.NonKycdUser -> flowOf(
                            DataResource.Data(SellEligibility.KycBlocked(data)) as DataResource<SellEligibility>
                        )
                        SellUserEligibility.KycdUser -> {
                            getSellAssetList().mapData { list ->
                                SellEligibility.Eligible(list)
                            }
                        }
                    }
                }
            }
        }

    private fun loadSellEligibility(): Flow<DataResource<SellUserEligibility>> =
        combine(
            kycService.getTiers(),
            simpleBuyService.isEligible()
        ) { kycTiers, isEligibleForBuy ->
            combineDataResources(kycTiers, isEligibleForBuy) { kyc, eligible ->
                when {
                    kyc.isApprovedFor(KycTier.GOLD) && eligible -> {
                        SellUserEligibility.KycdUser
                    }
                    kyc.isRejectedFor(KycTier.GOLD) -> {
                        SellUserEligibility.KycRejectedUser
                    }
                    kyc.isApprovedFor(KycTier.GOLD) && !eligible -> {
                        SellUserEligibility.KycRejectedUser
                    }
                    else -> {
                        SellUserEligibility.NonKycdUser
                    }
                }
            }
        }

    private fun getSellAssetList(): Flow<DataResource<List<AssetInfo>>> =
        combine(
            custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency),
            custodialWalletManager.getSupportedBuySellCryptoCurrencies().asFlow()
        ) { supportedFiats, supportedPairs ->
            supportedPairs
                .filter { supportedFiats.contains(it.destination) }
                .map { it.source.asAssetInfoOrThrow() }
        }.map {
            DataResource.Data(it) as DataResource<List<AssetInfo>>
        }.catch {
            emit(DataResource.Error(it as Exception))
        }

    private fun SingleAccountList.filterUnsupportedPairs(supportedAssets: List<AssetInfo>) =
        this.filter { account ->
            supportedAssets.contains(account.currency)
        }

    private fun SingleAccountList.filterDustBalances(): Single<List<CryptoAccount>> =
        map { account ->
            account.balanceRx().firstOrError()
        }.zipSingles().map {
            this.mapIndexedNotNull { index, singleAccount ->
                if (!it[index].totalFiat.isDust()) {
                    singleAccount
                } else {
                    null
                }
            }.filterIsInstance<CryptoAccount>()
        }
}
