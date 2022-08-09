package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.toActionState
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestActivity
import com.blockchain.core.interest.domain.model.InterestState
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferDirection
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.utils.extensions.mapList

class CryptoInterestAccount(
    override val currency: AssetInfo,
    override val label: String,
    private val internalAccountLabel: String,
    private val interestService: InterestService,
    private val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRatesDataManager,
    private val identity: UserIdentity,
) : CryptoAccountBase(), InterestAccount {

    override val baseActions: Set<AssetAction> = emptySet() // Not used by this class

    private val hasFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = interestService.getAddress(currency).map { address ->
            makeExternalAssetAddress(
                asset = currency,
                address = address,
                label = label,
                postTransactions = onTxCompleted
            )
        }

    override val onTxCompleted: (TxResult) -> Completable
        get() = { txResult ->
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
        }

    override val directions: Set<TransferDirection>
        get() = emptySet()

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is CryptoInterestAccount && other.currency == currency

    override val balance: Observable<AccountBalance>
        get() = Observable.combineLatest(
            interestService.getBalanceFor(currency),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            AccountBalance.from(balance, rate)
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override val activity: Single<ActivitySummaryList>
        get() = interestService.getActivity(currency)
            .onErrorReturn { emptyList() }
            .mapList { interestActivity ->
                interestActivityToSummary(asset = currency, interestActivity = interestActivity)
            }
            .filterActivityStates()
            .doOnSuccess {
                setHasTransactions(it.isNotEmpty())
            }

    private fun interestActivityToSummary(asset: AssetInfo, interestActivity: InterestActivity): ActivitySummaryItem =
        CustodialInterestActivitySummaryItem(
            exchangeRates = exchangeRates,
            asset = asset,
            txId = interestActivity.id,
            timeStampMs = interestActivity.insertedAt.time,
            value = interestActivity.value,
            account = this,
            status = interestActivity.state,
            type = interestActivity.type,
            confirmations = interestActivity.extraAttributes?.confirmations ?: 0,
            accountRef = interestActivity.extraAttributes?.address
                ?: interestActivity.extraAttributes?.transferType?.takeIf { it == "INTERNAL" }?.let {
                    internalAccountLabel
                } ?: "",
            recipientAddress = interestActivity.extraAttributes?.address ?: ""
        )

    private fun Single<ActivitySummaryList>.filterActivityStates(): Single<ActivitySummaryList> {
        return flattenAsObservable { list ->
            list.filter {
                it is CustodialInterestActivitySummaryItem && displayedStates.contains(it.status)
            }
        }.toList()
    }

    // No swaps on interest accounts, so just return the activity list unmodified
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> = activity

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean = false // Default is, presently, only ever a non-custodial account.

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.CAN_TRANSACT)

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.zip(
            identity.getHighestApprovedKycTier(),
            balance.firstOrError(),
            identity.userAccessForFeature(Feature.DepositInterest)
        ) { tier, balance, depositInterestEligibility ->
            return@zip when (tier) {
                Tier.BRONZE,
                Tier.SILVER -> emptySet()
                Tier.GOLD -> setOf(
                    StateAwareAction(
                        when (depositInterestEligibility) {
                            is FeatureAccess.Blocked -> depositInterestEligibility.toActionState()
                            else -> ActionState.Available
                        },
                        AssetAction.InterestDeposit
                    ),
                    StateAwareAction(
                        if (balance.withdrawable.isPositive) ActionState.Available else ActionState.LockedForBalance,
                        AssetAction.InterestWithdraw
                    ),
                    StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
                    StateAwareAction(ActionState.Available, AssetAction.ViewActivity)
                )
            }.exhaustive
        }

    companion object {
        private val displayedStates = setOf(
            InterestState.COMPLETE,
            InterestState.PROCESSING,
            InterestState.PENDING,
            InterestState.MANUAL_REVIEW,
            InterestState.FAILED
        )
    }
}
