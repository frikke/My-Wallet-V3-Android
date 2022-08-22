package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.defaultFilter
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.anyError
import com.blockchain.data.anyLoading
import com.blockchain.data.getFirstError
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.ui.dashboard.coinview.AssetDisplayInfo
import piuk.blockchain.android.ui.dashboard.coinview.AssetInformation
import piuk.blockchain.android.ui.dashboard.coinview.DetailsItem

class GetAssetAccountsUseCase(
    private val walletModeService: WalletModeService,
    private val interestService: InterestService,
    private val watchlistDataManager: WatchlistDataManager,
    private val currencyPrefs: CurrencyPrefs
) {
    suspend operator fun invoke(asset: CryptoAsset): Flow<DataResource<AssetInformation>> {

        val accountsFlow = asset.accountGroup(walletModeService.enabledWalletMode().defaultFilter())
            .map { it.accounts }
            .switchIfEmpty(Single.just(emptyList()))
            .await()
            .run { extractAccountDetails(this) }

        return combine(
            accountsFlow,
            asset.getPricesWith24hDelta(),
            interestService.getInterestRateFlow(asset.currency),
            flowOf(DataResource.Data(watchlistDataManager.isAssetInWatchlist(asset.currency).await())),
        ) { accounts, prices, interestRate, isAddedToWatchlist ->
            // while we wait for a BE flag on whether an asset is tradeable or not, we can check the
            // available accounts to see if we support custodial or PK balances as a guideline to asset support
            val results = listOf(accounts, prices, interestRate, isAddedToWatchlist)

            when {
                results.anyLoading() -> {
                    DataResource.Loading
                }

                results.anyError() -> {
                    DataResource.Error(results.getFirstError().error)
                }

                else -> {
                    accounts as DataResource.Data
                    prices as DataResource.Data
                    interestRate as DataResource.Data
                    isAddedToWatchlist as DataResource.Data

                    val isTradeableAsset = accounts.data.any {
                        it.account is NonCustodialAccount || it.account is CustodialTradingAccount
                    }

                    if (isTradeableAsset) {
                        val accountsList = mapAccounts(
                            walletMode = walletModeService.enabledWalletMode(),
                            accounts = accounts.data,
                            exchangeRate = prices.data.currentRate,
                            interestRate = interestRate.data
                        )

                        var totalCryptoMoneyAll = Money.zero(asset.currency)
                        val totalCryptoBalance = hashMapOf<AssetFilter, Money>()
                        var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)

                        accountsList.forEach { account ->
                            totalCryptoBalance[account.filter] =
                                (totalCryptoBalance[account.filter] ?: Money.zero(asset.currency)).plus(account.amount)
                            totalCryptoMoneyAll = totalCryptoMoneyAll.plus(account.amount)
                            totalFiatBalance = totalFiatBalance.plus(account.fiatValue)
                        }

                        totalCryptoBalance[AssetFilter.All] = totalCryptoMoneyAll

                        DataResource.Data(
                            AssetInformation.AccountsInfo(
                                isAddedToWatchlist = isAddedToWatchlist.data,
                                prices = prices.data,
                                accountsList = accountsList,
                                totalCryptoBalance = totalCryptoBalance,
                                totalFiatBalance = totalFiatBalance
                            )
                        )
                    } else {
                        DataResource.Data(
                            AssetInformation.NonTradeable(
                                isAddedToWatchlist = isAddedToWatchlist.data,
                                prices = prices.data
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun extractAccountDetails(accounts: List<SingleAccount>): Flow<DataResource<List<DetailsItem>>> {
        return accounts
            .filter {
                (it as? CryptoNonCustodialAccount)?.isArchived?.not() ?: true
            }
            .map { account ->
                combine(
                    account.balance.asFlow(),
                    flowOf(account.stateAwareActions.await())
                ) { balance, actions ->
                    DetailsItem(
                        account = account,
                        balance = balance.total,
                        pendingBalance = balance.pending,
                        actions = actions,
                        isDefault = account.isDefault
                    )
                }
            }
            .run {
                combine(this) {
                    DataResource.Data(it.toList())
                }
            }
    }

    private fun mapAccounts(
        walletMode: WalletMode,
        accounts: List<DetailsItem>,
        exchangeRate: ExchangeRate,
        interestRate: Double = Double.NaN
    ): List<AssetDisplayInfo> {

        val accountComparator = object : Comparator<DetailsItem> {
            override fun compare(o1: DetailsItem, o2: DetailsItem): Int {
                return getAssignedValue(o1).compareTo(getAssignedValue(o2))
            }

            fun getAssignedValue(detailItem: DetailsItem): Int {
                return when {
                    detailItem.account is NonCustodialAccount && detailItem.isDefault -> 0
                    detailItem.account is TradingAccount -> 1
                    detailItem.account is InterestAccount -> 2
                    detailItem.account is NonCustodialAccount && detailItem.isDefault.not() -> 3
                    else -> Int.MAX_VALUE
                }
            }
        }

        val sortedAccounts = accounts.sortedWith(accountComparator)

        return sortedAccounts.map {
            when (walletMode) {
                WalletMode.UNIVERSAL,
                WalletMode.CUSTODIAL_ONLY -> {
                    AssetDisplayInfo.BrokerageDisplayInfo(
                        account = it.account,
                        filter = when (it.account) {
                            is TradingAccount -> AssetFilter.Trading
                            is InterestAccount -> AssetFilter.Interest
                            // todo (othman) should be removed once universal mode is removed
                            is NonCustodialAccount -> AssetFilter.NonCustodial
                            else -> error("account type not supported")
                        },
                        amount = it.balance,
                        fiatValue = exchangeRate.convert(it.balance),
                        pendingAmount = it.pendingBalance,
                        actions = it.actions.filter { action ->
                            action.action != AssetAction.InterestDeposit
                        }.toSet(),
                        interestRate = interestRate
                    )
                }
                WalletMode.NON_CUSTODIAL_ONLY -> {
                    AssetDisplayInfo.DefiDisplayInfo(
                        account = it.account,
                        amount = it.balance,
                        fiatValue = exchangeRate.convert(it.balance),
                        pendingAmount = it.pendingBalance,
                        actions = it.actions.filter { action ->
                            action.action != AssetAction.InterestDeposit
                        }.toSet()
                    )
                }
            }
        }
    }
}