package piuk.blockchain.android.ui.coinview.presentation.interstitials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.earn.EarnAnalytics
import com.blockchain.nabu.BlockedReason
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.putAccount
import com.blockchain.presentation.koin.scopedInject
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.coinview.presentation.CoinViewAnalytics
import piuk.blockchain.android.ui.coinview.presentation.CoinViewNetwork
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent

class AccountActionsBottomSheet : ThemedBottomSheetFragment() {

    private val analytics: Analytics by inject()
    private val assetCatalogue: AssetCatalogue by scopedInject()

    private val selectedAccount by lazy {
        arguments?.getAccount(SELECTED_ACCOUNT) as CryptoAccount
    }

    private val stateAwareActions by lazy { arguments?.getSerializable(STATE_AWARE_ACTIONS) as Array<StateAwareAction> }
    private val balanceFiat by lazy { arguments?.getSerializable(BALANCE_FIAT) as Money? }
    private val balanceCrypto by lazy { arguments?.getSerializable(BALANCE_CRYPTO) as Money }
    private val interestRate: Double by lazy { arguments?.getDouble(INTEREST_RATE) ?: 0.0 }
    private val stakingRate: Double by lazy { arguments?.getDouble(STAKING_RATE) ?: 0.0 }
    private val activeRewardsRate: Double by lazy { arguments?.getDouble(ACTIVE_REWARDS_RATE) ?: 0.0 }

    interface Host {
        fun navigateToAction(
            action: AssetAction,
            selectedAccount: BlockchainAccount,
            assetInfo: AssetInfo
        )

        fun showUpgradeKycSheet()
        fun showSanctionsSheet(reason: BlockedReason.Sanctions)
        fun showBalanceUpsellSheet(item: AssetActionItem)
    }

    val host: Host by lazy {
        activity as? Host
            ?: throw IllegalStateException("Host activity is not a AccountActionsBottomSheet.Host")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        logEventWalletViewed(selectedAccount)

        val items =
            stateAwareActions.filter { it.state != ActionState.Unavailable }
                .map { action ->
                    mapAction(
                        action,
                        selectedAccount
                    )
                }

        return ComposeView(requireContext()).apply {
            setContent {
                Surface(
                    color = AppColors.background,
                    shape = AppTheme.shapes.large.topOnly()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SheetHeader(
                            title = selectedAccount.currency.name,
                            onClosePress = { dismiss() },
                            startImage = selectedAccount.l1Network()?.let {
                                StackedIcon.SmallTag(
                                    ImageResource.Remote(selectedAccount.currency.logo),
                                    ImageResource.Remote(it.logo)
                                )
                            } ?: StackedIcon.SingleIcon(
                                ImageResource.Remote(selectedAccount.currency.logo)
                            ),
                        )
                        DefaultTableRow(
                            backgroundColor = AppColors.background,
                            primaryText = balanceFiat?.toStringWithSymbol().orEmpty(),
                            secondaryText = balanceCrypto.toStringWithSymbol(),
                            endTag = when (selectedAccount) {
                                is EarnRewardsAccount.Interest -> {
                                    TagViewState(
                                        getString(
                                            com.blockchain.stringResources.R.string.actions_sheet_percentage_rate,
                                            interestRate.toString()
                                        ),
                                        TagType.Success()
                                    )
                                }

                                is EarnRewardsAccount.Staking -> {
                                    TagViewState(
                                        getString(
                                            com.blockchain.stringResources.R.string.actions_sheet_percentage_rate,
                                            stakingRate.toString()
                                        ),
                                        TagType.Success()
                                    )
                                }

                                is EarnRewardsAccount.Active -> {
                                    TagViewState(
                                        getString(
                                            com.blockchain.stringResources.R.string.actions_sheet_percentage_rate,
                                            activeRewardsRate.toString()
                                        ),
                                        TagType.Success()
                                    )
                                }

                                else -> null
                            },
                            endImageResource = ImageResource.None,
                            onClick = { }
                        )
                        ActionsList(items)
                    }
                }
            }
        }
    }

    private fun CryptoAccount.l1Network(): CoinViewNetwork? {
        return (this as? NonCustodialAccount)?.let {
            currency.takeIf { it.isLayer2Token }?.coinNetwork?.let {
                CoinViewNetwork(
                    logo = assetCatalogue.fromNetworkTicker(it.nativeAssetTicker)?.logo.orEmpty(),
                    name = it.shortName
                )
            }
        }
    }

    @Composable
    private fun ActionsList(assetActionItem: List<AssetActionItem>) {
        LazyColumn {
            paddedRoundedCornersItems(
                items = assetActionItem,
                paddingValues = {
                    PaddingValues(AppTheme.dimensions.smallSpacing)
                }
            ) {
                ActionListItem(it)
            }
        }
    }

    @Composable
    private fun ActionListItem(
        item: AssetActionItem
    ) {
        Column {
            val stateActionData = getStateActionData(item.action.state)
            val noOp = {}
            DefaultTableRow(
                startImageResource = getStartImagePerState(item.action.state, item.icon),
                primaryText = item.title,
                secondaryText = item.description,
                endImageResource = stateActionData.imageResource,
                onClick = when (stateActionData.state) {
                    ActionState.Unavailable,
                    ActionState.LockedDueToAvailability -> noOp

                    ActionState.LockedForBalance -> {
                        {
                            host.showBalanceUpsellSheet(item)
                            dismiss()
                        }
                    }

                    ActionState.LockedForTier -> {
                        {
                            host.showUpgradeKycSheet()
                            dismiss()
                        }
                    }

                    is ActionState.LockedDueToSanctions -> {
                        {
                            host.showSanctionsSheet(stateActionData.state.reason)
                            dismiss()
                        }
                    }

                    ActionState.Available -> item.actionCta
                }
            )
            Divider(color = AppTheme.colors.light, thickness = 1.dp)
        }
    }

    @Composable
    private fun getStartImagePerState(
        state: ActionState,
        @DrawableRes icon: Int
    ): ImageResource {
        return ImageResource.LocalWithBackground(
            id = icon,
            backgroundColor = AppColors.background,
            iconColor = AppColors.title,
            alpha = if (state == ActionState.Available) 1F else 0.5F
        )
    }

    @Composable
    private fun getStateActionData(state: ActionState): ActionData =
        when (state) {
            ActionState.Available -> ActionData(
                state = state,
                hasWarning = false,
                imageResource = Icons.ChevronRight.withTint(AppColors.muted)
            )

            else -> ActionData(
                state = state,
                hasWarning = false,
                imageResource = Icons.Filled.Lock.withTint(AppColors.muted)
            )
        }

    private fun mapAction(
        stateAwareAction: StateAwareAction,
        account: CryptoAccount
    ): AssetActionItem {
        val asset = account.currency
        return when (stateAwareAction.action) {
            AssetAction.ViewActivity ->
                AssetActionItem(
                    title = getString(com.blockchain.stringResources.R.string.activities_title),
                    icon = com.blockchain.common.R.drawable.ic_tx_activity_clock,
                    description = getString(com.blockchain.stringResources.R.string.fiat_funds_detail_activity_details),
                    asset = asset,
                    action = stateAwareAction
                ) {
                    logActionEvent(AssetDetailsAnalytics.ACTIVITY_CLICKED, asset)
                    processAction(AssetAction.ViewActivity)
                }

            AssetAction.Send ->
                AssetActionItem(
                    account = account,
                    title = getString(com.blockchain.stringResources.R.string.common_send),
                    icon = Icons.Send.id,
                    description = getString(
                        com.blockchain.stringResources.R.string.dashboard_asset_actions_send_dsc,
                        asset.displayTicker
                    ),
                    asset = asset,
                    action = stateAwareAction
                ) {
                    // send both events, marketing uses the first event, DS uses the second one
                    logActionEvent(AssetDetailsAnalytics.SEND_CLICKED, asset)
                    analytics.logEvent(
                        TransferAnalyticsEvent.TransferClicked(
                            LaunchOrigin.CURRENCY_PAGE,
                            type = TransferAnalyticsEvent.AnalyticsTransferType.SEND
                        )
                    )
                    processAction(AssetAction.Send)
                }

            AssetAction.Receive ->
                AssetActionItem(
                    title = getString(com.blockchain.stringResources.R.string.common_receive),
                    icon = Icons.Receive.id,
                    description = getString(
                        com.blockchain.stringResources.R.string.dashboard_asset_actions_receive_dsc,
                        asset.displayTicker
                    ),
                    asset = asset,
                    action = stateAwareAction
                ) {
                    logActionEvent(AssetDetailsAnalytics.RECEIVE_CLICKED, asset)
                    analytics.logEvent(
                        TransferAnalyticsEvent.TransferClicked(
                            LaunchOrigin.CURRENCY_PAGE,
                            type = TransferAnalyticsEvent.AnalyticsTransferType.SEND
                        )
                    )
                    processAction(AssetAction.Receive)
                }

            AssetAction.Swap -> AssetActionItem(
                account = account,
                title = getString(com.blockchain.stringResources.R.string.common_swap),
                icon = Icons.Swap.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_swap_dsc,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                logActionEvent(AssetDetailsAnalytics.SWAP_CLICKED, asset)
                processAction(AssetAction.Swap)
            }

            AssetAction.ViewStatement -> getViewStatementActionItemForAccount(asset, stateAwareAction, selectedAccount)
            AssetAction.InterestDeposit -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.dashboard_asset_actions_add_title),
                icon = Icons.Receive.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_add_dsc,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                analytics.logEvent(
                    EarnAnalytics.InterestDepositClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
                processAction(AssetAction.InterestDeposit)
            }

            AssetAction.InterestWithdraw -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.common_cash_out),
                icon = Icons.Send.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_withdraw_dsc_1,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                analytics.logEvent(
                    EarnAnalytics.InterestWithdrawalClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
                processAction(AssetAction.InterestWithdraw)
            }

            AssetAction.Sell -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.common_sell),
                icon = Icons.Minus.id,
                description = getString(com.blockchain.stringResources.R.string.convert_your_crypto_to_cash),
                asset = asset,
                action = stateAwareAction
            ) {
                logActionEvent(AssetDetailsAnalytics.SELL_CLICKED, asset)
                processAction(AssetAction.Sell)
            }

            AssetAction.Buy -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.common_buy),
                icon = Icons.Plus.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_buy_dsc,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                processAction(AssetAction.Buy)
            }

            AssetAction.StakingDeposit -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.dashboard_asset_actions_add_title),
                icon = Icons.Receive.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_add_staking_dsc,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                analytics.logEvent(
                    EarnAnalytics.StakingDepositClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
                processAction(AssetAction.StakingDeposit)
            }

            AssetAction.StakingWithdraw -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.common_cash_out),
                icon = Icons.Send.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_withdraw_dsc_1,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                analytics.logEvent(
                    EarnAnalytics.StakingWithdrawalClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
                processAction(AssetAction.StakingWithdraw)
            }

            AssetAction.ActiveRewardsDeposit -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.dashboard_asset_actions_add_title),
                icon = Icons.Receive.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_add_active_dsc,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                analytics.logEvent(
                    EarnAnalytics.ActiveRewardsDepositClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
                processAction(AssetAction.ActiveRewardsDeposit)
            }

            AssetAction.ActiveRewardsWithdraw -> AssetActionItem(
                title = getString(com.blockchain.stringResources.R.string.common_cash_out),
                icon = Icons.Send.id,
                description = getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_withdraw_dsc_1,
                    asset.displayTicker
                ),
                asset = asset,
                action = stateAwareAction
            ) {
                analytics.logEvent(
                    EarnAnalytics.ActiveRewardsWithdrawalClicked(
                        currency = asset.networkTicker,
                        origin = LaunchOrigin.CURRENCY_PAGE
                    )
                )
                processAction(AssetAction.ActiveRewardsWithdraw)
            }

            AssetAction.FiatWithdraw -> throw IllegalStateException("Cannot Withdraw a non-fiat currency")
            AssetAction.FiatDeposit -> throw IllegalStateException("Cannot Deposit a non-fiat currency to Fiat")
            AssetAction.Sign -> throw IllegalStateException("Sign action is not supported")
        }
    }

    private fun getViewStatementActionItemForAccount(
        asset: Currency,
        stateAwareAction: StateAwareAction,
        selectedAccount: CryptoAccount
    ): AssetActionItem {
        val title = getString(
            when (selectedAccount) {
                is EarnRewardsAccount.Interest ->
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_summary_title_1

                is EarnRewardsAccount.Staking ->
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_summary_staking_title

                is EarnRewardsAccount.Active ->
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_summary_active_rewards_title

                else -> com.blockchain.stringResources.R.string.empty
            }
        )
        val description =
            when (selectedAccount) {
                is EarnRewardsAccount.Interest -> getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_summary_dsc_1,
                    asset.displayTicker
                )

                is EarnRewardsAccount.Staking -> getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_summary_staking_dsc,
                    asset.displayTicker
                )

                is EarnRewardsAccount.Active -> getString(
                    com.blockchain.stringResources.R.string.dashboard_asset_actions_summary_active_rewards_dsc,
                    asset.displayTicker
                )

                else -> getString(com.blockchain.stringResources.R.string.empty)
            }

        val icon =
            when (selectedAccount) {
                is EarnRewardsAccount.Interest -> com.blockchain.common.R.drawable.ic_tx_interest
                is EarnRewardsAccount.Staking -> com.blockchain.common.R.drawable.ic_tx_interest
                is EarnRewardsAccount.Active -> com.blockchain.common.R.drawable.ic_tx_interest
                else -> com.blockchain.common.R.drawable.ic_tx_sent
            }

        return AssetActionItem(
            title = title,
            icon = icon,
            description = description,
            asset = asset,
            action = stateAwareAction
        ) {
            processAction(AssetAction.ViewStatement)
        }
    }

    private fun processAction(action: AssetAction) {
        host.navigateToAction(
            action = action,
            selectedAccount = selectedAccount,
            assetInfo = selectedAccount.currency
        )
        dismiss()
    }

    private fun logActionEvent(event: AssetDetailsAnalytics, asset: Currency) {
        analytics.logEvent(assetActionEvent(event, asset))
    }

    private fun logEventWalletViewed(selectedAccount: CryptoAccount) {
        analytics.logEvent(
            CoinViewAnalytics.WalletsAccountsViewed(
                origin = LaunchOrigin.COIN_VIEW,
                currency = selectedAccount.currency.networkTicker,
                accountType = when (selectedAccount) {
                    is TradingAccount -> CoinViewAnalytics.Companion.AccountType.CUSTODIAL
                    is NonCustodialAccount -> CoinViewAnalytics.Companion.AccountType.USERKEY
                    is EarnRewardsAccount.Interest -> CoinViewAnalytics.Companion.AccountType.REWARDS_ACCOUNT
                    // TODO(labreu): missing events for staking/active rewards
                    else -> CoinViewAnalytics.Companion.AccountType.EXCHANGE_ACCOUNT
                }
            )
        )
    }

    data class AssetActionItem(
        val account: BlockchainAccount?,
        val title: String,
        val icon: Int,
        val description: String,
        val asset: Currency,
        val action: StateAwareAction,
        val actionCta: () -> Unit
    ) {
        constructor(
            title: String,
            icon: Int,
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
            actionCta
        )

        val color: Color
            get() = Color(android.graphics.Color.parseColor(asset.colour))
    }

    companion object {
        private const val SELECTED_ACCOUNT = "selected_account"
        private const val STATE_AWARE_ACTIONS = "state_aware_actions"
        private const val BALANCE_FIAT = "balance_fiat"
        private const val BALANCE_CRYPTO = "balance_crypto"
        private const val INTEREST_RATE = "interest_rate"
        private const val STAKING_RATE = "staking_rate"
        private const val ACTIVE_REWARDS_RATE = "active_rewards_rate"

        fun newInstance(
            selectedAccount: BlockchainAccount,
            balanceFiat: Money?,
            balanceCrypto: Money,
            interestRate: Double,
            stakingRate: Double,
            activeRewardsRate: Double,
            stateAwareActions: Array<StateAwareAction>
        ): AccountActionsBottomSheet {
            return AccountActionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putAccount(SELECTED_ACCOUNT, selectedAccount)
                    putSerializable(BALANCE_FIAT, balanceFiat)
                    putSerializable(BALANCE_CRYPTO, balanceCrypto)
                    putDouble(INTEREST_RATE, interestRate)
                    putDouble(STAKING_RATE, stakingRate)
                    putDouble(ACTIVE_REWARDS_RATE, activeRewardsRate)
                    putSerializable(STATE_AWARE_ACTIONS, stateAwareActions)
                }
            }
        }
    }
}

private data class ActionData(
    val state: ActionState,
    val hasWarning: Boolean,
    val imageResource: ImageResource
)
