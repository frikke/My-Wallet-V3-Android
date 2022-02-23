package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AvailableActions
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.StateAwareAction
import com.blockchain.core.price.ExchangeRate
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class AllWalletsAccount(
    override val accounts: SingleAccountList,
    labels: DefaultLabels,
    private val currencyPrefs: CurrencyPrefs
) : AccountGroup {

    override val label: String = labels.getAllWalletLabel()

    override val balance: Observable<AccountBalance>
        get() = allAccounts().flattenAsObservable { it.filterIsInstance<SingleAccount>() }.flatMapSingle {
            it.balance.firstOrError()
        }.reduce { a, v ->
            AccountBalance(
                total = a.exchangeRate.convert(a.total) + v.exchangeRate.convert(v.total),
                withdrawable = a.exchangeRate.convert(a.withdrawable) + v.exchangeRate.convert(v.withdrawable),
                pending = a.exchangeRate.convert(a.pending) + v.exchangeRate.convert(v.pending),
                exchangeRate = ExchangeRate.identityExchangeRate(currencyPrefs.selectedFiatCurrency)
            )
        }.toObservable()

    override val activity: Single<ActivitySummaryList>
        get() = allActivities()

    override val actions: Single<AvailableActions>
        get() = Single.just(setOf(AssetAction.ViewActivity))

    override val stateAwareActions: Single<Set<StateAwareAction>> = Single.just(
        setOf(
            StateAwareAction(ActionState.Available, AssetAction.ViewActivity)
        )
    )

    override val isFunded: Boolean
        get() = true

    override val hasTransactions: Boolean
        get() = true

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("No receive address for All Wallets meta account"))

    override fun includes(account: BlockchainAccount): Boolean = true

    private fun allAccounts(): Single<List<BlockchainAccount>> =
        Single.just(accounts)

    private fun allActivities(): Single<ActivitySummaryList> =
        allAccounts().flattenAsObservable { it }
            .flatMapSingle { account ->
                account.activity
                    .onErrorResumeNext { Single.just(emptyList()) }
            }
            .reduce { a, l -> a + l }
            .defaultIfEmpty(emptyList())
            .map { it.distinct() }
            .map { it.sorted() }
}
