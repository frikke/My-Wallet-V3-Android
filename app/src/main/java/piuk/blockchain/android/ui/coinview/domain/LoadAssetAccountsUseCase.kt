package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.ActionState
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
import com.blockchain.data.combineDataResources
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccountDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetInformation
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance

class LoadAssetAccountsUseCase(
    private val walletModeService: WalletModeService,
    private val interestService: InterestService,
    private val watchlistDataManager: WatchlistDataManager,
    private val currencyPrefs: CurrencyPrefs
) {
    suspend operator fun invoke(asset: CryptoAsset): Flow<DataResource<CoinviewAssetInformation>> {

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

            combineDataResources(
                accounts,
                prices,
                interestRate,
                isAddedToWatchlist
            ) { accountsData, pricesData, interestRateData, isAddedToWatchlistData ->
                val isTradeableAsset = accountsData.any {
                    it.account is NonCustodialAccount || it.account is CustodialTradingAccount
                }

                if (isTradeableAsset) {
                    val accountsList = mapAccounts(
                        walletMode = walletModeService.enabledWalletMode(),
                        accounts = accountsData,
                        exchangeRate = pricesData.currentRate,
                        interestRate = interestRateData
                    )

                    var totalCryptoMoneyAll = Money.zero(asset.currency)
                    val totalCryptoBalance = hashMapOf<AssetFilter, Money>()
                    var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)

                    accountsList.accounts.forEach { account ->
                        totalCryptoBalance[account.filter] =
                            (totalCryptoBalance[account.filter] ?: Money.zero(asset.currency)).plus(
                                account.cryptoBalance
                            )
                        totalCryptoMoneyAll = totalCryptoMoneyAll.plus(account.cryptoBalance)
                        totalFiatBalance = totalFiatBalance.plus(account.fiatBalance)
                    }

                    totalCryptoBalance[AssetFilter.All] = totalCryptoMoneyAll

                    CoinviewAssetInformation.AccountsInfo(
                        isAddedToWatchlist = isAddedToWatchlistData,
                        accounts = accountsList,
                        totalBalance = CoinviewAssetTotalBalance(
                            totalCryptoBalance = totalCryptoBalance,
                            totalFiatBalance = totalFiatBalance
                        ),
                    )
                } else {
                    CoinviewAssetInformation.NonTradeable(
                        isAddedToWatchlist = isAddedToWatchlistData
                    )
                }
            }
        }
    }

    private suspend fun extractAccountDetails(
        accounts: List<SingleAccount>
    ): Flow<DataResource<List<CoinviewAccountDetail>>> {
        return accounts
            .filter { account ->
                (account as? CryptoNonCustodialAccount)?.isArchived?.not() ?: true
            }
            .map { account ->
                combine(
                    account.balance.asFlow().map { DataResource.Data(it) },
                    flowOf(account.stateAwareActions.await()).map { DataResource.Data(it) }
                ) { balance, actions ->

                    combineDataResources(balance, actions) { balanceData, actionsData ->
                        CoinviewAccountDetail(
                            account = account,
                            balance = balanceData.total,
                            isAvailable = actionsData.any { it.state == ActionState.Available },
                            isDefault = account.isDefault
                        )
                    }
                }
            }
            .run {
                combine(this) {
                    combineDataResources(it.toList()) { accountsDetails: List<CoinviewAccountDetail> ->
                        accountsDetails
                    }
                }
            }
    }

    private fun mapAccounts(
        walletMode: WalletMode,
        accounts: List<CoinviewAccountDetail>,
        exchangeRate: ExchangeRate,
        interestRate: Double = Double.NaN
    ): CoinviewAccounts {

        val accountComparator = object : Comparator<CoinviewAccountDetail> {
            override fun compare(o1: CoinviewAccountDetail, o2: CoinviewAccountDetail): Int {
                return getAssignedValue(o1).compareTo(getAssignedValue(o2))
            }

            fun getAssignedValue(detailItem: CoinviewAccountDetail): Int {
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

        // create accounts based on wallet mode and account type
        return when (walletMode) {
            WalletMode.UNIVERSAL -> {
                sortedAccounts.map {
                    CoinviewAccount.Universal(
                        filter = when (it.account) {
                            is TradingAccount -> AssetFilter.Trading
                            is InterestAccount -> AssetFilter.Interest
                            is NonCustodialAccount -> AssetFilter.NonCustodial
                            else -> error("account type not supported")
                        },
                        account = it.account,
                        cryptoBalance = it.balance,
                        fiatBalance = exchangeRate.convert(it.balance),
                        interestRate = interestRate,
                        isEnabled = it.isAvailable
                    )
                }.run { CoinviewAccounts.Universal(this) }
            }

            WalletMode.CUSTODIAL_ONLY -> {
                sortedAccounts.map {
                    when (it.account) {
                        is TradingAccount -> {
                            CoinviewAccount.Custodial.Trading(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = exchangeRate.convert(it.balance)
                            )
                        }

                        is InterestAccount -> {
                            CoinviewAccount.Custodial.Interest(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = exchangeRate.convert(it.balance),
                                interestRate = interestRate
                            )
                        }

                        else -> error("account type not supported")
                    }
                }.run { CoinviewAccounts.Custodial(this) }
            }

            WalletMode.NON_CUSTODIAL_ONLY -> {
                sortedAccounts.map {
                    CoinviewAccount.Defi(
                        account = it.account,
                        cryptoBalance = it.balance,
                        fiatBalance = exchangeRate.convert(it.balance),
                        isEnabled = it.isAvailable
                    )
                }.run { CoinviewAccounts.Defi(this) }
            }
        }
    }
}
