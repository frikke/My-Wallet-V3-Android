package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CustodialActiveRewardsActivitySummaryItem
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
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.toObservable
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.EarnRewardsState
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class CustodialActiveRewardsAccount(
    override val currency: AssetInfo,
    override val label: String,
    private val internalAccountLabel: String,
    private val activeRewardsService: ActiveRewardsService,
    override val exchangeRates: ExchangeRatesDataManager,
    private val identity: UserIdentity,
    private val kycService: KycService,
    private val custodialWalletManager: CustodialWalletManager
) : CryptoAccountBase(), EarnRewardsAccount.Active {

    override val baseActions: Single<Set<AssetAction>> = Single.just(emptySet()) // Not used by this class

    override val receiveAddress: Single<ReceiveAddress>
        get() = rxSingleOutcome { activeRewardsService.getAccountAddress(currency) }.map { address ->
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
                    product = Product.EARN_CC1W
                )
            }
        }

    override val directions: Set<TransferDirection>
        get() = emptySet()

    override fun matches(other: CryptoAccount): Boolean =
        other is CustodialActiveRewardsAccount && other.currency == currency

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> =
        Observable.combineLatest(
            activeRewardsService.getBalanceForAsset(
                currency = currency,
                refreshStrategy = freshnessStrategy
            ).toObservable(),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            AccountBalance(
                total = balance.totalBalance,
                withdrawable = balance.earningBalance,
                pending = balance.pendingDeposit,
                exchangeRate = rate
            )
        }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> =
        activeRewardsService.getActivity(
            asset = currency,
            refreshStrategy = freshnessStrategy
        ).toObservable()
            .onErrorResumeNext { Observable.just(emptyList()) }
            .map { activeRewardsActivity ->
                activeRewardsActivity.map { activity ->
                    activeRewardsActivityToSummary(asset = currency, activity = activity)
                }.filter {
                    it is CustodialActiveRewardsActivitySummaryItem && displayedStates.contains(it.state)
                }
            }

    private fun activeRewardsActivityToSummary(asset: AssetInfo, activity: EarnRewardsActivity): ActivitySummaryItem =
        CustodialActiveRewardsActivitySummaryItem(
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

    // No swaps on active rewards accounts, so just return the activity list unmodified
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
            identity.userAccessForFeature(Feature.DepositActiveRewards),
            balanceRx(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)).firstOrError()
        ) { tier, depositInterestEligibility, balance ->
            return@zip when (tier) {
                KycTier.BRONZE,
                KycTier.SILVER -> emptySet()

                KycTier.GOLD -> setOf(
                    StateAwareAction(
                        when (depositInterestEligibility) {
                            is FeatureAccess.Blocked -> depositInterestEligibility.toActionState()
                            else -> ActionState.Available
                        },
                        AssetAction.ActiveRewardsDeposit
                    ),
                    StateAwareAction(
                        ActionState.Available, // TODO (labreu): check Feature.WithdrawActiveRewards!!!
                        AssetAction.ActiveRewardsWithdraw
                    ),
                    StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
                    StateAwareAction(ActionState.Available, AssetAction.ViewActivity)
                )
            }.exhaustive
        }

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        return when (assetAction) {
            AssetAction.ViewActivity,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.ViewStatement -> Single.just(ActionState.Available)
            AssetAction.Send,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Buy,
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit,
            AssetAction.Sign,
            AssetAction.StakingWithdraw,
            AssetAction.InterestWithdraw,
            AssetAction.StakingDeposit,
            AssetAction.Receive -> Single.just(ActionState.Unavailable)

            AssetAction.InterestDeposit,
            AssetAction.ActiveRewardsDeposit -> stateAwareActions.map { set ->
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
