package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.defaultFilter
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.data.map
import com.blockchain.earn.domain.models.staking.StakingRates
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.flatMapData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.rx3.await
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
    private val stakingService: StakingService
) {
    suspend operator fun invoke(asset: CryptoAsset): Flow<DataResource<CoinviewAssetDetail>> {

        val accountsFlow = walletModeService.walletModeSingle
            .flatMapMaybe { asset.accountGroup(it.defaultFilter()) }
            .map { it.accounts }
            .switchIfEmpty(Single.just(emptyList()))
            .await()
            .run { extractAccountDetails(this) }

        val interestFlow = interestService.isAssetAvailableForInterestFlow(asset.currency).flatMapData {
            if (it) {
                interestService.getInterestRateFlow(asset.currency)
            } else
                flow {
                    emit(DataResource.Data(0.toDouble()))
                }
        }

        val stakingFlow =
            stakingService.getAvailabilityForAsset(asset.currency).flatMapData { available ->
                if (available) {
                    stakingService.getRatesForAsset(asset.currency)
                } else
                    flow {
                        emit(DataResource.Data(StakingRates(0.0, 0.0)))
                    }
            }

        return combine(
            walletModeService.walletMode,
            accountsFlow,
            exchangeRatesDataManager.exchangeRateToUserFiatFlow(
                asset.currency,
                freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
            ),
            interestFlow,
            stakingFlow
        ) { wMode, accounts, price, interestRate, stakingRate ->
            // while we wait for a BE flag on whether an asset is tradeable or not, we can check the
            // available accounts to see if we support custodial or PK balances as a guideline to asset support

            combineDataResources(
                price,
                interestRate,
                stakingRate
            ) { pricesData, interestRateData, stakingRateData ->
                val isTradeableAsset = accounts.any {
                    it.account is NonCustodialAccount || it.account is CustodialTradingAccount
                }

                if (isTradeableAsset) {
                    val accountsList = mapAccounts(
                        walletMode = wMode,
                        accounts = accounts,
                        exchangeRate = pricesData,
                        interestRate = interestRateData,
                        stakingRate = stakingRateData.rate
                    )

                    var totalCryptoMoneyAll = Money.zero(asset.currency)
                    val totalCryptoBalance = hashMapOf<AssetFilter, Money>()
                    var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)

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
                        totalFiatBalance = totalFiatBalance.plus(
                            (account.fiatBalance as? DataResource.Data<Money>)?.data ?: Money.zero(
                                currencyPrefs.selectedFiatCurrency
                            )
                        )
                    }

                    totalCryptoBalance[AssetFilter.All] = totalCryptoMoneyAll

                    CoinviewAssetDetail.Tradeable(
                        accounts = accountsList,
                        totalBalance = CoinviewAssetTotalBalance(
                            totalCryptoBalance = totalCryptoBalance,
                            totalFiatBalance = totalFiatBalance
                        ),
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
                    account.balance().map { DataResource.Data(it) as DataResource<AccountBalance> }.catch {
                        emit(DataResource.Error(it as Exception))
                    },
                    flowOf(account.stateAwareActions.await()).map { DataResource.Data(it) }.catch {
                        emit(DataResource.Data(emptySet()))
                    }
                ) { balance, actions ->
                    CoinviewAccountDetail(
                        account = account,
                        balance = balance.map { it.total },
                        isAvailable = actions.data.any { it.state == ActionState.Available },
                        isDefault = account.isDefault
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
        exchangeRate: ExchangeRate,
        interestRate: Double = Double.NaN,
        stakingRate: Double = Double.NaN
    ): CoinviewAccounts {

        val sortedAccounts = accounts.sorted()

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
                                fiatBalance = it.balance.map {
                                    exchangeRate.convert(it)
                                },
                            )
                        }
                        is InterestAccount -> {
                            CoinviewAccount.Custodial.Interest(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = it.balance.map {
                                    exchangeRate.convert(it)
                                },
                                interestRate = interestRate
                            )
                        }
                        is StakingAccount -> {
                            CoinviewAccount.Custodial.Staking(
                                isEnabled = it.isAvailable,
                                account = it.account,
                                cryptoBalance = it.balance,
                                fiatBalance = it.balance.map {
                                    exchangeRate.convert(it)
                                },
                                stakingRate = stakingRate
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
                        fiatBalance = it.balance.map {
                            exchangeRate.convert(it)
                        },
                        isEnabled = it.isAvailable
                    )
                }.run { CoinviewAccounts.Defi(this) }
            }
        }
    }
}
