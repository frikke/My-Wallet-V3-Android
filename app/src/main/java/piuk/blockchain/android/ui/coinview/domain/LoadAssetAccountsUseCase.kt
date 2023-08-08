package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.defaultFilter
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.dataOrElse
import com.blockchain.data.dataOrNull
import com.blockchain.data.filterNotLoading
import com.blockchain.data.flatMapData
import com.blockchain.data.map
import com.blockchain.data.onErrorReturn
import com.blockchain.earn.domain.models.ActiveRewardsRates
import com.blockchain.earn.domain.models.StakingRewardsRates
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.awaitOutcome
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccountDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance

class LoadAssetAccountsUseCase(
    private val walletModeService: WalletModeService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val interestService: InterestService,
    private val currencyPrefs: CurrencyPrefs,
    private val stakingService: StakingService,
    private val activeRewardsService: ActiveRewardsService
) {
    suspend operator fun invoke(asset: CryptoAsset): Flow<DataResource<CoinviewAssetDetail>> {
        val accountsFlow = walletModeService.walletModeSingle
            .flatMapMaybe { asset.accountGroup(it.defaultFilter()) }
            .map { it.accounts }
            .switchIfEmpty(Single.just(emptyList()))
            .awaitOutcome().getOrDefault(emptyList())
            .run { extractAccountDetails(this) }

        val interestFlow = interestService.isAssetAvailableForInterestFlow(asset.currency).flatMapData { available ->
            if (available) {
                interestService.getInterestRateFlow(
                    asset.currency,
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
                ).onErrorReturn {
                    0.toDouble()
                }
            } else {
                flowOf(DataResource.Data(0.toDouble()))
            }
        }

        val stakingFlow =
            stakingService.getAvailabilityForAsset(asset.currency).flatMapData { available ->
                if (available) {
                    stakingService.getRatesForAsset(asset.currency).onErrorReturn {
                        StakingRewardsRates(0.0, 0.0)
                    }
                } else {
                    flowOf(DataResource.Data(StakingRewardsRates(0.0, 0.0)))
                }
            }

        val activeRewardsFlow =
            activeRewardsService.getAvailabilityForAsset(asset.currency).flatMapData { available ->
                if (available) {
                    activeRewardsService.getRatesForAsset(
                        asset.currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    ).onErrorReturn {
                        ActiveRewardsRates(0.0, 0.0, Money.zero(asset.currency))
                    }
                } else {
                    flowOf(DataResource.Data(ActiveRewardsRates(0.0, 0.0, Money.zero(asset.currency))))
                }
            }

        return walletModeService.walletMode.flatMapLatest { wMode ->
            val exchangeRate =
                exchangeRatesDataManager.exchangeRateToUserFiatFlow(asset.currency)
                    .filterNotLoading()
                    .firstOrNull()
                    ?.dataOrNull()

            combineDataResourceFlows(
                accountsFlow.map { DataResource.Data(it) },
                if (wMode == WalletMode.CUSTODIAL) {
                    interestFlow
                } else {
                    flowOf(DataResource.Data(0.toDouble()))
                },
                if (wMode == WalletMode.CUSTODIAL) {
                    stakingFlow
                } else {
                    flowOf(DataResource.Data(StakingRewardsRates(0.0, 0.0)))
                },
                if (wMode == WalletMode.CUSTODIAL) {
                    activeRewardsFlow
                } else {
                    flowOf(DataResource.Data(ActiveRewardsRates(0.0, 0.0, Money.zero(asset.currency))))
                }
            ) { accounts, interestRate, stakingRate, activeRewardsRate ->
                // while we wait for a BE flag on whether an asset is tradeable or not, we can check the
                // available accounts to see if we support custodial or PK balances as a guideline to asset support

                if (accounts.isNotEmpty()) {
                    val accountsList = mapAccounts(
                        walletMode = wMode,
                        accounts = accounts,
                        exchangeRate = exchangeRate,
                        interestRate = interestRate,
                        stakingRate = stakingRate.rate,
                        activeRewardsRate = activeRewardsRate.rate
                    )

                    var totalCryptoMoneyAll = Money.zero(asset.currency)
                    val totalCryptoBalance = hashMapOf<AssetFilter, Money>()
                    var totalFiatBalance: Money? = null

                    accountsList.accounts.forEach { account ->
                        totalCryptoBalance[account.filter] =
                            (totalCryptoBalance[account.filter] ?: Money.zero(asset.currency)).plus(
                                (account.cryptoBalance as? DataResource.Data<Money>)?.data
                                    ?: Money.zero(asset.currency)
                            )
                        totalCryptoMoneyAll = totalCryptoMoneyAll.plus(
                            (account.cryptoBalance as? DataResource.Data<Money>)?.data
                                ?: Money.zero(asset.currency)
                        )
                        totalFiatBalance = (account.fiatBalance as? DataResource.Data<Money?>)?.data?.let {
                            (totalFiatBalance ?: Money.zero(currencyPrefs.selectedFiatCurrency)).plus(
                                it
                            )
                        }
                    }

                    totalCryptoBalance[AssetFilter.All] = totalCryptoMoneyAll

                    CoinviewAssetDetail.Tradeable(
                        accounts = accountsList,
                        totalBalance = CoinviewAssetTotalBalance(
                            totalCryptoBalance = totalCryptoBalance,
                            totalFiatBalance = totalFiatBalance
                        )
                    )
                } else {
                    CoinviewAssetDetail.NonTradeable(
                        totalBalance = CoinviewAssetTotalBalance(
                            totalCryptoBalance = hashMapOf(
                                AssetFilter.All to CryptoValue.zero(asset.currency)
                            ),
                            totalFiatBalance = FiatValue.zero(currencyPrefs.selectedFiatCurrency)
                        )
                    )
                }
            }
        }
    }

    private suspend fun extractAccountDetails(
        accounts: List<SingleAccount>
    ): Flow<List<CoinviewAccountDetail>> {
        return accounts
            .filter { account ->
                (account as? CryptoNonCustodialAccount)?.isArchived?.not() ?: true
            }
            .map { account ->
                combine(
                    // balance
                    account.balance().map { DataResource.Data(it) as DataResource<AccountBalance> }.catch {
                        emit(DataResource.Error(it as Exception))
                    },
                    // address
                    flowOf(account.receiveAddress.map { it.address }.awaitOutcome().getOrDefault("")),
                    // availability
                    flowOf(account.stateAwareActions.awaitOutcome().getOrDefault(emptySet())).map {
                        DataResource.Data(
                            it
                        )
                    }.catch {
                        emit(DataResource.Data(emptySet()))
                    }.map {
                        it.data.any { action -> action.state == ActionState.Available }
                    }.onStart {
                        emit(true)
                    }
                ) { balance, address, hasAnyActionsAvailable ->
                    CoinviewAccountDetail(
                        account = account,
                        balance = balance.map { it.total },
                        address = address,
                        isAvailable = hasAnyActionsAvailable
                    )
                }
            }.run {
                combine(this) {
                    it.toList()
                }.onEmpty { emit(emptyList()) }
            }
    }

    private fun mapAccounts(
        walletMode: WalletMode,
        accounts: List<CoinviewAccountDetail>,
        exchangeRate: ExchangeRate?,
        interestRate: Double = Double.NaN,
        stakingRate: Double = Double.NaN,
        activeRewardsRate: Double = Double.NaN
    ): CoinviewAccounts {
        val sortedAccounts =
            accounts.sortedWith(
                compareBy<CoinviewAccountDetail> { it.getIndexedValue() }.thenByDescending {
                    it.balance.dataOrElse(
                        Money.zero(it.account.currency)
                    )
                }.thenBy { it.account.label }
            )

        // create accounts based on wallet mode and account type
        return when (walletMode) {
            WalletMode.CUSTODIAL -> {
                sortedAccounts.map {
                    when (it.account) {
                        is TradingAccount -> {
                            CoinviewAccount.Custodial.Trading(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = it.balance.map { balance ->
                                    exchangeRate?.convert(balance)
                                }
                            )
                        }

                        is EarnRewardsAccount.Interest -> {
                            CoinviewAccount.Custodial.Interest(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = it.balance.map { balance ->
                                    exchangeRate?.convert(balance)
                                },
                                interestRate = interestRate
                            )
                        }

                        is EarnRewardsAccount.Staking -> {
                            CoinviewAccount.Custodial.Staking(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = it.balance.map { balance ->
                                    exchangeRate?.convert(balance)
                                },
                                stakingRate = stakingRate
                            )
                        }

                        is EarnRewardsAccount.Active -> {
                            CoinviewAccount.Custodial.ActiveRewards(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = it.balance.map { balance ->
                                    exchangeRate?.convert(balance)
                                },
                                activeRewardsRate = activeRewardsRate
                            )
                        }

                        else -> error("account type not supported")
                    }
                }.run { CoinviewAccounts.Custodial(this) }
            }

            WalletMode.NON_CUSTODIAL -> {
                sortedAccounts.map {
                    CoinviewAccount.PrivateKey(
                        account = it.account,
                        cryptoBalance = it.balance,
                        fiatBalance = it.balance.map { balance ->
                            exchangeRate?.convert(balance)
                        },
                        isEnabled = it.isAvailable,
                        address = it.address
                    )
                }.run { CoinviewAccounts.Defi(this) }
            }
        }
    }
}
