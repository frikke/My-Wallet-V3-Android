package piuk.blockchain.android.ui.dashboard.assetdetails

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.service.TierService
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogAssetActionsSheetBinding
import piuk.blockchain.android.databinding.ItemAssetActionBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.customviews.account.PendingBalanceAccountDecorator
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.customviews.account.addViewToBottomWithConstraints
import piuk.blockchain.android.ui.customviews.account.removePossibleBottomView
import piuk.blockchain.android.ui.transactionflow.analytics.InterestAnalytics
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import timber.log.Timber

class AssetActionsSheet :
    MviBottomSheet<AssetDetailsModel, AssetDetailsIntent, AssetDetailsState, DialogAssetActionsSheetBinding>() {
    private val disposables = CompositeDisposable()

    private val kycTierService: TierService by scopedInject()

    override val model: AssetDetailsModel by scopedInject()

    private val itemAdapter: AssetActionAdapter by lazy {
        AssetActionAdapter(
            disposable = disposables,
            statusDecorator = ::statusDecorator
        )
    }

    override fun render(newState: AssetDetailsState) {
        if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            newState.selectedAccount?.let { accounts ->
                showAssetBalances(newState)
                val firstAccount = accounts.selectFirstAccount()
                itemAdapter.itemList = newState.actions
                    // TODO(aromano): Remove once other ActionStates are handled, probably this will never be implemented
                    //                here, but rather in CoinView
                    .filter { it.state == ActionState.Available || it.state == ActionState.LockedForTier }
                    .map { action ->
                        mapAction(
                            action,
                            action.hasWarning(newState),
                            firstAccount
                        )
                    }
            }

            if (newState.errorState != AssetDetailsError.NONE) {
                showError(newState.errorState)
            }
        }
    }

    override fun initControls(binding: DialogAssetActionsSheetBinding) {
        binding.assetActionsList.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = itemAdapter
        }

        binding.assetActionsBack.setOnClickListener {
            model.process(ClearActionStates)
            model.process(ReturnToPreviousStep)
            dispose()
        }
    }

    override fun dismiss() {
        super.dismiss()
        model.process(ClearSheetDataIntent)
        dispose()
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        if (account is CryptoAccount) {
            AssetActionsDecorator(account)
        } else {
            DefaultCellDecorator()
        }

    private fun showError(error: AssetDetailsError) =
        when (error) {
            AssetDetailsError.TX_IN_FLIGHT ->
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.dashboard_asset_actions_tx_in_progress),
                    duration = Snackbar.LENGTH_SHORT,
                    type = SnackbarType.Error
                ).show()
            else -> {
                // do nothing
            }
        }

    private fun showAssetBalances(state: AssetDetailsState) {
        with(binding.assetActionsAccountDetails) {
            updateAccount(
                state.selectedAccount.selectFirstAccount(),
                {},
                PendingBalanceAccountDecorator(state.selectedAccount.selectFirstAccount())
            )
        }
    }

    private fun mapAction(
        stateAwareAction: StateAwareAction,
        hasWarning: Boolean,
        account: CryptoAccount
    ): AssetActionItem {
        val asset = account.currency
        return when (stateAwareAction.action) {
            // using the secondary ctor ensures the action is always enabled if it is present
            AssetAction.ViewActivity ->
                AssetActionItem(
                    title = getString(R.string.activities_title),
                    icon = R.drawable.ic_tx_activity_clock,
                    hasWarning = hasWarning,
                    description = getString(R.string.fiat_funds_detail_activity_details),
                    asset = asset,
                    action = stateAwareAction
                ) {
                    logActionEvent(AssetDetailsAnalytics.ACTIVITY_CLICKED, asset)
                    processAction(stateAwareAction)
                }
            AssetAction.Send ->
                AssetActionItem(
                    account = account,
                    title = getString(R.string.common_send),
                    icon = R.drawable.ic_tx_sent,
                    hasWarning = hasWarning,
                    description = getString(
                        R.string.dashboard_asset_actions_send_dsc,
                        asset.displayTicker
                    ),
                    asset = asset,
                    action = stateAwareAction
                ) {
                    // send both events, marketing uses the first event, DS uses the second one
                    logActionEvent(AssetDetailsAnalytics.SEND_CLICKED, asset)
                    processAction(stateAwareAction)
                    analytics.logEvent(
                        TransferAnalyticsEvent.TransferClicked(
                            LaunchOrigin.CURRENCY_PAGE,
                            type = TransferAnalyticsEvent.AnalyticsTransferType.SEND
                        )
                    )
                }
            AssetAction.Receive ->
                AssetActionItem(
                    title = getString(R.string.common_receive),
                    icon = R.drawable.ic_tx_receive,
                    hasWarning = hasWarning,
                    description = getString(
                        R.string.dashboard_asset_actions_receive_dsc,
                        asset.displayTicker
                    ),
                    asset = asset,
                    action = stateAwareAction
                ) {
                    logActionEvent(AssetDetailsAnalytics.RECEIVE_CLICKED, asset)
                    processAction(stateAwareAction)
                    analytics.logEvent(
                        TransferAnalyticsEvent.TransferClicked(
                            LaunchOrigin.CURRENCY_PAGE,
                            type = TransferAnalyticsEvent.AnalyticsTransferType.SEND
                        )
                    )
                }
            AssetAction.Swap -> AssetActionItem(
                account = account,
                title = getString(R.string.common_swap),
                icon = R.drawable.ic_tx_swap,
                hasWarning = hasWarning,
                description = getString(
                    R.string.dashboard_asset_actions_swap_dsc, asset.displayTicker
                ),
                asset = asset, action = stateAwareAction
            ) {
                logActionEvent(AssetDetailsAnalytics.SWAP_CLICKED, asset)
                processAction(stateAwareAction)
            }
            AssetAction.ViewStatement -> AssetActionItem(
                title = getString(R.string.dashboard_asset_actions_summary_title_1),
                icon = R.drawable.ic_tx_interest,
                hasWarning = hasWarning,
                description = getString(R.string.dashboard_asset_actions_summary_dsc_1, asset.displayTicker),
                asset = asset, action = stateAwareAction
            ) {
                checkForKycStatus {
                    processAction(stateAwareAction)
                }
            }
            AssetAction.InterestDeposit -> AssetActionItem(
                title = getString(R.string.dashboard_asset_actions_add_title),
                icon = R.drawable.ic_tx_deposit_arrow,
                hasWarning = hasWarning,
                description = getString(R.string.dashboard_asset_actions_add_dsc, asset.displayTicker),
                asset = asset,
                action = stateAwareAction
            ) {
                processAction(stateAwareAction)
                analytics.logEvent(
                    InterestAnalytics.InterestDepositClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
            }
            AssetAction.InterestWithdraw -> AssetActionItem(
                title = getString(R.string.common_withdraw),
                icon = R.drawable.ic_tx_withdraw,
                hasWarning = hasWarning,
                description = getString(R.string.dashboard_asset_actions_withdraw_dsc_1, asset.displayTicker),
                asset = asset,
                action = stateAwareAction
            ) {
                processAction(stateAwareAction)
                analytics.logEvent(
                    InterestAnalytics.InterestWithdrawalClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
            }
            AssetAction.Sell -> AssetActionItem(
                title = getString(R.string.common_sell),
                icon = R.drawable.ic_tx_sell,
                hasWarning = hasWarning,
                description = getString(R.string.convert_your_crypto_to_cash),
                asset = asset, action = stateAwareAction
            ) {
                logActionEvent(AssetDetailsAnalytics.SELL_CLICKED, asset)
                processAction(stateAwareAction)
            }
            AssetAction.Buy -> AssetActionItem(
                title = getString(R.string.common_buy),
                icon = R.drawable.ic_tx_buy,
                hasWarning = hasWarning,
                description = getString(R.string.dashboard_asset_actions_buy_dsc, asset.displayTicker),
                asset = asset,
                action = stateAwareAction
            ) {
                processAction(stateAwareAction)
            }
            AssetAction.Withdraw -> throw IllegalStateException("Cannot Withdraw a non-fiat currency")
            AssetAction.FiatDeposit -> throw IllegalStateException("Cannot Deposit a non-fiat currency to Fiat")
            AssetAction.Sign -> throw IllegalStateException("Sign action is not supported")
        }
    }

    private fun logActionEvent(event: AssetDetailsAnalytics, asset: Currency) {
        analytics.logEvent(assetActionEvent(event, asset))
    }

    private fun checkForKycStatus(action: () -> Unit) {
        disposables += kycTierService.tiers().subscribeBy(
            onSuccess = { tiers ->
                if (tiers.isApprovedFor(KycTierLevel.GOLD)) {
                    action()
                } else {
                    model.process(ShowInterestDashboard)
                    dismiss()
                }
            },
            onError = {
                Timber.e("Error getting tiers in asset actions sheet $it")
            }
        )
    }

    private fun processAction(stateAwareAction: StateAwareAction) {
        when (stateAwareAction.state) {
            ActionState.LockedForTier -> model.process(HandleActionIntentLockedForTier)
            ActionState.Available,
            ActionState.LockedForBalance,
            ActionState.LockedDueToAvailability,
            ActionState.Unavailable -> {
                model.process(HandleActionIntent(stateAwareAction.action))
            }
        }

        dispose()
    }

    companion object {
        fun newInstance(): AssetActionsSheet = AssetActionsSheet()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogAssetActionsSheetBinding =
        DialogAssetActionsSheetBinding.inflate(inflater, container, false)
}

private fun StateAwareAction.hasWarning(newState: AssetDetailsState): Boolean =
    when (this.action) {
        AssetAction.Buy ->
            (newState.userBuyAccess as? FeatureAccess.Blocked)?.reason is BlockedReason.TooManyInFlightTransactions
        else -> false
    }

private class AssetActionAdapter(
    val disposable: CompositeDisposable,
    val statusDecorator: StatusDecorator
) : RecyclerView.Adapter<AssetActionAdapter.ActionItemViewHolder>() {
    var itemList: List<AssetActionItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionItemViewHolder =
        ActionItemViewHolder(
            compositeDisposable, ItemAssetActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ActionItemViewHolder, position: Int) =
        holder.bind(itemList[position], statusDecorator)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        compositeDisposable.clear()
    }

    private class ActionItemViewHolder(
        private val compositeDisposable: CompositeDisposable,
        private val binding: ItemAssetActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: AssetActionItem,
            statusDecorator: StatusDecorator
        ) {
            addDecorator(item, statusDecorator)

            binding.apply {
                itemActionIcon.setImageResource(item.icon)
                if (item.action.state == ActionState.LockedForTier) {
                    itemActionIcon.setAssetIconColours(
                        ContextCompat.getColor(context, R.color.grey_000),
                        ContextCompat.getColor(context, R.color.grey_600)
                    )
                } else {
                    itemActionIcon.setAssetIconColoursWithTint(item.asset)
                }
                itemActionTitle.text = item.title
                itemActionLabel.text = item.description

                if (item.action.state == ActionState.LockedForTier) {
                    itemActionWarning.setImageResource(R.drawable.ic_lock)
                    itemActionWarning.imageTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_300))
                } else if (item.hasWarning) {
                    itemActionWarning.setImageResource(R.drawable.ic_warning)
                    itemActionWarning.imageTintList = null
                }
                itemActionWarning.visibleIf { item.hasWarning || item.action.state == ActionState.LockedForTier }
            }
        }

        private fun addDecorator(
            item: AssetActionItem,
            statusDecorator: StatusDecorator
        ) {
            item.account?.let { account ->
                compositeDisposable += statusDecorator(account).isEnabled()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { enabled ->
                            with(binding.itemActionHolder) {
                                if (enabled) {
                                    alpha = 1f
                                    setOnClickListener {
                                        item.actionCta()
                                    }
                                } else {
                                    alpha = .6f
                                    setOnClickListener {}
                                }
                            }
                        },
                        onError = {
                            Timber.e("Error getting decorator info $it")
                        }
                    )
                binding.itemActionHolder.removePossibleBottomView()
                compositeDisposable += statusDecorator(account).view(context)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        binding.itemActionHolder.addViewToBottomWithConstraints(
                            it,
                            bottomOfView = binding.itemActionLabel,
                            startOfView = binding.itemActionIcon,
                            endOfView = null
                        )
                    }
            } ?: binding.itemActionHolder.setOnClickListener {
                item.actionCta()
            }
        }
    }
}

private data class AssetActionItem(
    val account: BlockchainAccount?,
    val title: String,
    val icon: Int,
    val description: String,
    val asset: Currency,
    val action: StateAwareAction,
    val hasWarning: Boolean,
    val actionCta: () -> Unit
) {
    constructor(
        title: String,
        icon: Int,
        hasWarning: Boolean,
        description: String,
        asset: Currency,
        action: StateAwareAction,
        actionCta: () -> Unit
    ) : this(
        null,
        title,
        icon,
        description,
        asset,
        action,
        hasWarning,
        actionCta
    )
}
