package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.toActionState
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.dataOrNull
import com.blockchain.data.toObservable
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.EarnRewardsState
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.rx3.asObservable

class CustodialInterestAccount(
    override val currency: AssetInfo,
    override val label: String,
    private val internalAccountLabel: String,
    private val interestService: InterestService,
    override val exchangeRates: ExchangeRatesDataManager,
    private val identity: UserIdentity,
    private val kycService: KycService,
    private val custodialWalletManager: CustodialWalletManager
) : CryptoAccountBase(), EarnRewardsAccount.Interest {

    override val baseActions: Single<Set<AssetAction>> = Single.just(emptySet()) // Not used by this class

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

    override fun matches(other: CryptoAccount): Boolean =
        other is CustodialInterestAccount && other.currency == currency

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> =
        Observable.combineLatest(
            interestService.getBalanceFor(
                asset = currency,
                refreshStrategy = freshnessStrategy
            ),
            exchangeRates.exchangeRateToUserFiatFlow(currency).asObservable()
        ) { balance, rate ->
            AccountBalance(
                total = balance.totalBalance,
                withdrawable = balance.actionableBalance,
                pending = balance.pendingDeposit,
                exchangeRate = rate.dataOrNull()
            )
        }.doOnNext { hasFunds.set(it.total.isPositive) }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> {
        return interestService.getActivityFlow(currency, freshnessStrategy)
            .toObservable()
            .onErrorResumeNext { Observable.just(emptyList()) }
            .map { interestActivity ->
                interestActivity.map {
                    interestActivityToSummary(asset = currency, activity = it)
                }.filter {
                    it is CustodialInterestActivitySummaryItem &&
                        displayedStates.contains(it.state)
                }
            }
    }

    private fun interestActivityToSummary(asset: AssetInfo, activity: EarnRewardsActivity): ActivitySummaryItem =
        CustodialInterestActivitySummaryItem(
            exchangeRates = exchangeRates,
            currency = asset,
            txId = activity.id,
            timeStampMs = activity.insertedAt.time,
            value = activity.value,
            account = this,
            state = activity.state,
            type = activity.type,
            confirmations = activity.extraAttributes?.confirmations ?: 0,
            accountRef = activity.extraAttributes?.address
                ?: activity.extraAttributes?.transferType?.takeIf { it == "INTERNAL" }?.let {
                    internalAccountLabel
                } ?: "",
            recipientAddress = activity.extraAttributes?.address ?: "",
            fiatValue = activity.fiatValue
        )

    // No swaps on interest accounts, so just return the activity list unmodified
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> = activity

    override val isDefault: Boolean = false // Default is, presently, only ever a non-custodial account.

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.CAN_TRANSACT)

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.zip(
            kycService.getHighestApprovedTierLevelLegacy(),
            balanceRx().firstOrError(),
            identity.userAccessForFeature(Feature.DepositInterest, defFreshness)
        ) { tier, balance, depositInterestEligibility ->
            return@zip when (tier) {
                KycTier.BRONZE,
                KycTier.SILVER -> emptySet()

                KycTier.GOLD -> setOf(
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

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return when (assetAction) {
            AssetAction.ViewActivity,
            AssetAction.ViewStatement -> Single.just(ActionState.Available)

            AssetAction.Send,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Buy,
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit,
            AssetAction.Sign,
            AssetAction.StakingDeposit,
            AssetAction.StakingWithdraw,
            AssetAction.ActiveRewardsDeposit,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.Receive -> Single.just(ActionState.Unavailable)
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw -> stateAwareActions.map { set ->
                set.firstOrNull { it.action == assetAction }?.state ?: ActionState.Unavailable
            }
        }
    }

    companion object {
        private val displayedStates = setOf(
            EarnRewardsState.COMPLETE,
            EarnRewardsState.PROCESSING,
            EarnRewardsState.PENDING,
            EarnRewardsState.MANUAL_REVIEW,
            EarnRewardsState.FAILED
        )
    }
}
