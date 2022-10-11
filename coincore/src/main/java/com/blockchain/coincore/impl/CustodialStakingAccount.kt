package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxSourceState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.staking.domain.StakingActivity
import com.blockchain.core.staking.domain.StakingService
import com.blockchain.core.staking.domain.StakingState
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.store.asObservable
import com.blockchain.store.asSingle
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.utils.extensions.mapList

class CustodialStakingAccount(
    override val currency: AssetInfo,
    override val label: String,
    private val internalAccountLabel: String,
    private val stakingService: StakingService,
    override val exchangeRates: ExchangeRatesDataManager,
) : CryptoAccountBase(), StakingAccount {

    override val baseActions: Set<AssetAction> = emptySet() // Not used by this class

    private val hasFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError())

    // TODO(dserrano) - STAKING - add deposit & withdraw
    /*stakingService.getAddress(currency).map { address ->
            makeExternalAssetAddress(
                asset = currency,
                address = address,
                label = label,
                postTransactions = onTxCompleted
            )
        }*/

    override val onTxCompleted: (TxResult) -> Completable
        get() = { Completable.error(NotImplementedError()) } /*{ txResult ->
            require(txResult.amount is CryptoValue)
            require(txResult is TxResult.HashedTxResult)
            receiveAddress.flatMapCompletable { receiveAddress ->
                custodialWalletManager.createPendingDeposit(
                    crypto = txResult.amount.currency,
                    address = receiveAddress.address,
                    hash = txResult.txId,
                    amount = txResult.amount,
                    product = Product.SAVINGS
                )
            }
        }*/

    override val directions: Set<TransferDirection>
        get() = emptySet()

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is CustodialStakingAccount && other.currency == currency

    override val balanceRx: Observable<AccountBalance>
        get() = Observable.combineLatest(
            stakingService.getBalanceForAsset(
                currency = currency,
                refreshStrategy = FreshnessStrategy.Cached(forceRefresh = false)
            ).asObservable(),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            AccountBalance.from(balance, rate)
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override val activity: Single<ActivitySummaryList>
        get() = stakingService.getActivity(
            currency = currency,
            refreshStrategy = FreshnessStrategy.Cached(forceRefresh = false)
        ).asSingle()
            .onErrorReturn { emptyList() }
            .mapList { stakingActivity ->
                stakingActivityToSummary(asset = currency, stakingActivity = stakingActivity)
            }
            .filterActivityStates()
            .doOnSuccess {
                setHasTransactions(it.isNotEmpty())
            }

    private fun stakingActivityToSummary(asset: AssetInfo, stakingActivity: StakingActivity): ActivitySummaryItem =
        CustodialStakingActivitySummaryItem(
            exchangeRates = exchangeRates,
            asset = asset,
            txId = stakingActivity.id,
            timeStampMs = stakingActivity.insertedAt.time,
            value = stakingActivity.value,
            account = this,
            status = stakingActivity.state,
            type = stakingActivity.type,
            confirmations = stakingActivity.extraAttributes?.confirmations ?: 0,
            accountRef = stakingActivity.extraAttributes?.address
                ?: stakingActivity.extraAttributes?.transferType?.takeIf { it == "INTERNAL" }?.let {
                    internalAccountLabel
                } ?: "",
            recipientAddress = stakingActivity.extraAttributes?.address ?: ""
        )

    private fun Single<ActivitySummaryList>.filterActivityStates(): Single<ActivitySummaryList> {
        return flattenAsObservable { list ->
            list.filter {
                it is CustodialStakingActivitySummaryItem && displayedStates.contains(it.status)
            }
        }.toList()
    }

    // No swaps on staking accounts, so just return the activity list unmodified
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> = activity

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean = false // Default is, presently, only ever a non-custodial account.

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.CAN_TRANSACT)

    // TODO(dserrano) - STAKING - add deposit & withdraw
    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.just(setOf(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)))

    companion object {
        private val displayedStates = setOf(
            StakingState.COMPLETE,
            StakingState.PROCESSING,
            StakingState.PENDING,
            StakingState.MANUAL_REVIEW,
            StakingState.FAILED
        )
    }
}
