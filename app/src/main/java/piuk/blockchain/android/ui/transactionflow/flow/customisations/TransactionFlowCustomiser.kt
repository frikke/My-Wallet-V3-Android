package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.widget.FrameLayout
import android.widget.ImageView
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialStakingAccount
import com.blockchain.coincore.impl.txEngine.fiat.WITHDRAW_LOCKS
import com.blockchain.componentlib.utils.AnnotatedStringUtils
import com.blockchain.componentlib.utils.StringAnnotationClickEvent
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.core.limits.TxLimit
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.ui.customviews.account.AccountInfoBank
import piuk.blockchain.android.ui.customviews.account.AccountInfoCrypto
import piuk.blockchain.android.ui.customviews.account.AccountInfoFiat
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.swap.SwapAccountSelectSheetFeeDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.ui.transactionflow.plugin.AccountLimitsView
import piuk.blockchain.android.ui.transactionflow.plugin.AvailableBalanceView
import piuk.blockchain.android.ui.transactionflow.plugin.BalanceAndFeeView
import piuk.blockchain.android.ui.transactionflow.plugin.ConfirmSheetWidget
import piuk.blockchain.android.ui.transactionflow.plugin.EmptyHeaderView
import piuk.blockchain.android.ui.transactionflow.plugin.EnterAmountWidget
import piuk.blockchain.android.ui.transactionflow.plugin.FromAndToView
import piuk.blockchain.android.ui.transactionflow.plugin.QuickFillRowView
import piuk.blockchain.android.ui.transactionflow.plugin.SimpleInfoHeaderView
import piuk.blockchain.android.ui.transactionflow.plugin.SmallBalanceView
import piuk.blockchain.android.ui.transactionflow.plugin.SwapInfoHeaderView
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import piuk.blockchain.android.urllinks.CHECKOUT_REFUND_POLICY
import piuk.blockchain.android.urllinks.TRADING_ACCOUNT_LOCKS
import piuk.blockchain.android.util.StringLocalizationUtil
import piuk.blockchain.android.util.StringUtils
import timber.log.Timber

interface TransactionFlowCustomiser :
    EnterAmountCustomisations,
    SourceSelectionCustomisations,
    TargetSelectionCustomisations,
    TransactionConfirmationCustomisations,
    TransactionProgressCustomisations,
    TransactionFlowCustomisations

class TransactionFlowCustomiserImpl(
    private val resources: Resources,
    private val assetResources: AssetResources,
) : TransactionFlowCustomiser {
    override fun enterAmountActionIcon(state: TransactionState): Int {
        return when (state.action) {
            AssetAction.Send -> R.drawable.ic_tx_sent
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> R.drawable.ic_tx_deposit_arrow
            AssetAction.FiatDeposit -> R.drawable.ic_tx_deposit_w_green_bkgd
            AssetAction.Swap -> R.drawable.ic_swap_light_blue
            AssetAction.Sell -> R.drawable.ic_tx_sell
            AssetAction.FiatWithdraw -> R.drawable.ic_tx_withdraw_w_green_bkgd
            AssetAction.InterestWithdraw -> R.drawable.ic_tx_withdraw
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun shouldDisableInput(errorState: TransactionErrorState): Boolean =
        errorState == TransactionErrorState.PENDING_ORDERS_LIMIT_REACHED

    override fun enterAmountActionIconCustomisation(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap,
            AssetAction.FiatDeposit,
            AssetAction.FiatWithdraw -> false
            else -> true
        }

    override fun selectSourceAddressTitle(state: TransactionState): String = "Select Source Address"

    override fun selectTargetAddressInputHint(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_enter_asset_address_or_domain_hint
            )
            AssetAction.Sell -> ""
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun selectTargetAddressInputWarning(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_address_warning,
                state.sendingAsset.displayTicker,
                state.networkName
            )
            else -> ""
        }

    override fun selectTargetShouldShowInputWarning(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Send -> true
            else -> false
        }

    override fun selectTargetAddressTitlePick(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_transfer_accounts_title
            )
            else -> ""
        }

    override fun selectTargetShouldShowTargetPickTitle(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Send -> true
            else -> false
        }

    override fun selectTargetNoAddressMessageText(state: TransactionState): String? =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_internal_transfer_message_1_1,
                state.sendingAsset.name,
                state.sendingAsset.displayTicker
            )
            else -> null
        }

    override fun selectTargetAddressTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.InterestDeposit -> resources.getString(R.string.common_transfer)
            AssetAction.StakingDeposit -> resources.getString(R.string.common_stake)
            AssetAction.Swap -> resources.getString(R.string.swap_select_target_title)
            AssetAction.FiatWithdraw -> resources.getString(R.string.common_withdraw)
            AssetAction.InterestWithdraw -> resources.getString(R.string.select_withdraw_target_title)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun selectTargetShouldShowSubtitle(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap -> true
            else -> false
        }

    override fun selectTargetSubtitle(state: TransactionState): String =
        resources.getString(
            when (state.action) {
                AssetAction.Swap -> R.string.swap_select_target_subtitle
                else -> R.string.empty
            }
        )

    override fun selectTargetAddressWalletsCta(state: TransactionState) =
        resources.getString(
            when (state.action) {
                AssetAction.FiatWithdraw -> R.string.select_a_bank
                else -> R.string.select_a_wallet
            }
        )

    override fun selectTargetSourceLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.common_swap)
            else -> resources.getString(R.string.common_from)
        }

    override fun selectTargetDestinationLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.common_receive)
            else -> resources.getString(R.string.common_to)
        }

    override fun selectTargetStatusDecorator(state: TransactionState, walletMode: WalletMode): StatusDecorator =
        when (state.action) {
            AssetAction.Swap -> {
                {
                    SwapAccountSelectSheetFeeDecorator(account = it, walletMode = walletMode)
                }
            }
            else -> {
                {
                    DefaultCellDecorator()
                }
            }
        }

    override fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean =
        state.action == AssetAction.Send

    override fun enterAmountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_enter_amount_title, state.sendingAsset.displayTicker
            )
            AssetAction.Swap -> resources.getString(
                R.string.tx_title_swap,
                state.sendingAsset.displayTicker,
                (state.selectedTarget as CryptoAccount).currency.displayTicker
            )
            AssetAction.InterestDeposit -> resources.getString(
                R.string.tx_title_add_with_ticker,
                state.sendingAsset.displayTicker
            )
            AssetAction.StakingDeposit -> resources.getString(
                R.string.tx_title_stake_with_ticker,
                state.sendingAsset.displayTicker
            )
            AssetAction.Sell -> resources.getString(
                R.string.tx_title_sell,
                state.sendingAsset.displayTicker
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.tx_title_deposit,
                (state.selectedTarget as FiatAccount).currency.displayTicker
            )
            AssetAction.FiatWithdraw -> resources.getString(
                R.string.tx_title_withdraw,
                (state.sendingAccount as FiatAccount).currency.displayTicker
            )
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.tx_title_withdraw, state.sendingAsset.displayTicker
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun enterAmountMaxButton(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_enter_amount_max)
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> resources.getString(R.string.send_enter_amount_deposit_max)
            AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_max)
            AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_max)
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_enter_amount_max)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun enterAmountSourceLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(
                R.string.swap_enter_amount_source,
                state.amount.toStringWithSymbol()
            )
            else -> resources.getString(R.string.send_enter_amount_from, state.sendingAccount.label)
        }

    override fun enterAmountTargetLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> {
                val amount = state.targetRate?.convert(state.amount) ?: Money.zero(
                    (state.selectedTarget as CryptoAccount).currency
                )
                resources.getString(
                    R.string.swap_enter_amount_target,
                    amount.toStringWithSymbol()
                )
            }
            else -> resources.getString(R.string.send_enter_amount_to, state.selectedTargetLabel)
        }

    override fun enterAmountLoadSourceIcon(imageView: ImageView, state: TransactionState) {
        assetResources.loadAssetIcon(imageView, state.sendingAsset)
    }

    override fun shouldShowMaxLimit(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.FiatDeposit -> false
            else -> true
        }

    override fun enterAmountLimitsViewTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_enter_amount_limit_title)
            AssetAction.FiatWithdraw -> state.sendingAccount.label
            else -> throw java.lang.IllegalStateException("Limits title view not configured for ${state.action}")
        }

    override fun enterAmountLimitsViewInfo(state: TransactionState): String =
        when (state.action) {
            AssetAction.FiatDeposit ->
                resources.getString(
                    R.string.deposit_enter_amount_limit_label,
                    (state.pendingTx?.limits?.max as? TxLimit.Limited)?.amount?.toStringWithSymbol() ?: ""
                )
            AssetAction.FiatWithdraw -> state.availableBalance.toStringWithSymbol()
            else -> throw java.lang.IllegalStateException("Limits info view not configured for ${state.action}")
        }

    override fun enterAmountMaxNetworkFeeLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.Sell,
            AssetAction.Swap,
            AssetAction.Send -> resources.getString(R.string.send_enter_amount_max_fee)
            else -> throw java.lang.IllegalStateException("Max network fee label not configured for ${state.action}")
        }

    override fun shouldNotDisplayNetworkFee(state: TransactionState): Boolean =
        state.action == AssetAction.Swap &&
            state.sendingAccount is NonCustodialAccount && state.selectedTarget is NonCustodialAccount

    override fun enterAmountGetNoBalanceMessage(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.enter_amount_not_enough_balance)
            else -> "--"
        }

    override fun enterAmountCtaText(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.tx_enter_amount_send_cta)
            AssetAction.Swap -> resources.getString(R.string.tx_enter_amount_swap_cta)
            AssetAction.Sell -> resources.getString(R.string.tx_enter_amount_sell_cta)
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.tx_enter_amount_withdraw_cta)
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> resources.getString(R.string.tx_enter_amount_transfer_cta)
            AssetAction.FiatDeposit -> resources.getString(R.string.tx_enter_amount_deposit_cta)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun getFeeSheetTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.tx_enter_amount_swap_fees_title)
            AssetAction.Sell -> resources.getString(R.string.tx_enter_amount_sell_fees_title)
            else -> throw IllegalStateException("${state.action} is not supported for fee sheet title")
        }

    override fun getFeeSheetAvailableLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.tx_enter_amount_fee_sheet_swap_available_label)
            AssetAction.Sell -> resources.getString(R.string.tx_enter_amount_fee_sheet_sell_available_label)
            else -> throw IllegalStateException("${state.action} is not supported for fee sheet label")
        }

    override fun getFraudFlowForTransaction(state: TransactionState): FraudFlow? {
        val action = state.action
        val sendingAccount = state.sendingAccount

        if (action == AssetAction.FiatDeposit && sendingAccount is LinkedBankAccount) {
            return if (sendingAccount.isOpenBankingCurrency()) {
                FraudFlow.OB_DEPOSIT
            } else {
                FraudFlow.ACH_DEPOSIT
            }
        } else if (action == AssetAction.FiatWithdraw) {
            return FraudFlow.WITHDRAWAL
        }
        return null
    }

    override fun confirmTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.common_parametrised_confirm, resources.getString(R.string.send_confirmation_title)
            )
            AssetAction.Swap -> resources.getString(
                R.string.common_parametrised_confirm, resources.getString(R.string.common_swap)
            )
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> resources.getString(
                R.string.common_parametrised_confirm,
                resources.getString(
                    R.string.common_transfer
                )
            )
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.common_parametrised_confirm,
                resources.getString(
                    R.string.common_withdraw
                )
            )
            AssetAction.Sign -> resources.getString(R.string.signature_request)
            AssetAction.Sell -> resources.getString(
                R.string.common_parametrised_confirm, resources.getString(R.string.common_sell)
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.common_parametrised_confirm, resources.getString(R.string.common_deposit)
            )
            AssetAction.FiatWithdraw -> resources.getString(
                R.string.common_parametrised_confirm, resources.getString(R.string.common_withdraw)
            )
            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Buy,
            AssetAction.Receive,
            -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun confirmCtaText(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_confirmation_cta_button)
            AssetAction.Swap -> resources.getString(R.string.swap_confirmation_cta_button)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_cta_button)
            AssetAction.Sign -> resources.getString(R.string.common_sign)
            AssetAction.InterestDeposit -> resources.getString(R.string.send_confirmation_deposit_cta_button)
            AssetAction.StakingDeposit -> resources.getString(R.string.send_confirmation_stake_cta_button)
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_confirmation_cta_button)
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_confirmation_cta_button)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun confirmListItemTitle(assetAction: AssetAction): String {
        return when (assetAction) {
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.InterestDeposit -> resources.getString(R.string.common_transfer)
            AssetAction.StakingDeposit -> resources.getString(R.string.common_transfer)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.FiatDeposit -> resources.getString(R.string.common_deposit)
            AssetAction.FiatWithdraw -> resources.getString(R.string.common_withdraw)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun cancelButtonText(action: AssetAction): String =
        resources.getString(R.string.common_cancel)

    override fun cancelButtonVisible(action: AssetAction): Boolean =
        action == AssetAction.Sign

    override fun confirmDisclaimerBlurb(state: TransactionState, context: Context): CharSequence =
        when (state.action) {
            AssetAction.Swap,
            AssetAction.Sell -> {
                val map = mapOf(
                    "refund_policy" to StringAnnotationClickEvent.OpenUri(Uri.parse(CHECKOUT_REFUND_POLICY))
                )
                AnnotatedStringUtils.getStringWithMappedAnnotations(
                    stringId = when (state.action) {
                        AssetAction.Swap -> R.string.swap_confirmation_disclaimer_1
                        AssetAction.Sell -> R.string.sell_confirmation_disclaimer
                        else -> throw IllegalStateException("Invalid action for refund policy")
                    },
                    linksMap = map,
                    context = context
                )
            }
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.checkout_rewards_confirmation_disclaimer, state.sendingAsset.displayTicker,
                state.selectedTarget.label
            )
            AssetAction.FiatDeposit -> {
                if (state.pendingTx?.engineState?.containsKey(WITHDRAW_LOCKS) == true) {
                    val days = resources.getString(
                        R.string.funds_locked_warning_days,
                        state.pendingTx.engineState[WITHDRAW_LOCKS]
                    )
                    StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                        resources.getString(
                            R.string.funds_locked_warning,
                            days
                        ),
                        R.string.common_linked_learn_more,
                        TRADING_ACCOUNT_LOCKS, context, R.color.blue_600
                    )
                } else ""
            }
            else -> throw IllegalStateException("Disclaimer not set for asset action ${state.action}")
        }

    override fun confirmDisclaimerVisibility(state: TransactionState, assetAction: AssetAction): Boolean {
        val showAchDisclaimer = assetAction == AssetAction.FiatDeposit && state.sendingAccount is LinkedBankAccount &&
            state.ffImprovedPaymentUxEnabled && state.sendingAccount.isAchCurrency()
        return if (showAchDisclaimer) {
            false
        } else {
            when (assetAction) {
                AssetAction.Swap,
                AssetAction.FiatDeposit,
                AssetAction.InterestWithdraw,
                AssetAction.Sell -> true
                else -> false
            }
        }
    }

    override fun transactionProgressTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_progress_sending_title, amount
            )
            AssetAction.Swap -> {
                val receivingAmount = state.targetRate?.convert(state.amount) ?: Money.zero(
                    (state.selectedTarget as CryptoAccount).currency
                )
                resources.getString(
                    R.string.swap_progress_title,
                    state.amount.toStringWithSymbol(), receivingAmount.toStringWithSymbol()
                )
            }
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_confirmation_progress_title,
                amount
            )
            AssetAction.StakingDeposit -> resources.getString(
                R.string.staking_confirmation_progress_title,
                amount
            )
            AssetAction.Sell -> resources.getString(
                R.string.sell_confirmation_progress_title,
                amount
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_progress_title,
                amount
            )
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.withdraw_confirmation_progress_title,
                amount
            )
            AssetAction.Sign -> resources.getString(
                R.string.signing_confirmation_progress_title
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun transactionProgressMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_progress_sending_subtitle)
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> resources.getString(
                R.string.send_confirmation_progress_message,
                state.sendingAsset.displayTicker
            )
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_message)
            AssetAction.Swap -> resources.getString(R.string.swap_confirmation_progress_message)
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_confirmation_progress_message)
            AssetAction.Sign -> resources.getString(R.string.sign_confirmation_progress_message)
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_confirmation_progress_message)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun transactionCompleteTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""
        return when (state.action) {
            AssetAction.Send -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(R.string.send_progress_awaiting_complete_title)
                } else {
                    resources.getString(R.string.send_progress_complete_title, amount)
                }
            }
            AssetAction.Swap -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(R.string.swap_progress_awaiting_complete_title)
                } else {
                    resources.getString(R.string.swap_progress_complete_title)
                }
            }
            AssetAction.Sell -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(R.string.sell_progress_awaiting_complete_title)
                } else {
                    resources.getString(
                        R.string.sell_progress_complete_title, amount
                    )
                }
            }
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(R.string.transfer_confirmation_awaiting_success_title)
                } else {
                    resources.getString(R.string.send_confirmation_success_title, amount)
                }
            }
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_success_title,
                amount
            )
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw,
            -> resources.getString(R.string.withdraw_confirmation_success_title, amount)
            AssetAction.Sign -> resources.getString(R.string.signed)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun transactionCompleteIcon(state: TransactionState): Int {
        return when (state.action) {
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit,
            AssetAction.Sell -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    R.drawable.ic_pending_clock
                } else {
                    R.drawable.ic_check_circle
                }
            }
            AssetAction.Swap -> {
                when {
                    state.sendingAccount is NonCustodialAccount &&
                        state.selectedTarget is CryptoNonCustodialAccount -> {
                        R.drawable.ic_pending_clock
                    }
                    state.sendingAccount is NonCustodialAccount -> R.drawable.ic_pending_clock
                    else -> R.drawable.ic_check_circle
                }
            }
            AssetAction.FiatDeposit,
            AssetAction.FiatWithdraw,
            AssetAction.Sign,
            AssetAction.InterestWithdraw -> R.drawable.ic_check_circle
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun transactionCompleteMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(
                        R.string.send_progress_awaiting_subtitle, state.sendingAsset.name
                    )
                } else {
                    resources.getString(
                        R.string.send_progress_complete_subtitle, state.sendingAsset.displayTicker
                    )
                }
            }
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(
                        R.string.transfer_confirmation_awaiting_success_message, state.sendingAsset.name
                    )
                } else {
                    resources.getString(
                        R.string.send_confirmation_success_message,
                        state.sendingAsset.displayTicker
                    )
                }
            }
            AssetAction.Sell -> {
                if (state.sendingAccount is NonCustodialAccount) {
                    resources.getString(
                        R.string.sell_confirmation_awaiting_success_message, state.sendingAsset.name
                    )
                } else {
                    resources.getString(
                        R.string.sell_confirmation_success_message,
                        (state.selectedTarget as? FiatAccount)?.currency
                    )
                }
            }
            AssetAction.Swap -> {
                when {
                    state.sendingAccount is NonCustodialAccount &&
                        state.selectedTarget is CryptoNonCustodialAccount -> {
                        resources.getString(
                            R.string.swap_confirmation_awaiting_nc_receiving_success_message,
                            state.sendingAsset.name,
                            state.selectedTarget.currency.name
                        )
                    }
                    state.sendingAccount is NonCustodialAccount -> {
                        resources.getString(
                            R.string.swap_confirmation_awaiting_nc_sending_success_message, state.sendingAsset.name
                        )
                    }
                    else -> {
                        resources.getString(
                            R.string.swap_confirmation_success_message,
                            (state.selectedTarget as CryptoAccount).currency.displayTicker
                        )
                    }
                }
            }
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_success_message,
                state.pendingTx?.amount?.toStringWithSymbol() ?: "",
                (state.sendingAccount as? FiatAccount)?.currency ?: "",
                getEstimatedTransactionCompletionTime()
            )
            AssetAction.Sign -> resources.getString(
                R.string.message_signed
            )
            AssetAction.FiatWithdraw -> resources.getString(
                R.string.withdraw_confirmation_success_message,
                getEstimatedTransactionCompletionTime()
            )
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.withdraw_rewards_confirmation_success_message,
                state.sendingAsset.displayTicker,
                state.selectedTarget.label
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun selectTargetAccountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap,
            AssetAction.Send -> resources.getString(R.string.common_receive)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.FiatDeposit -> resources.getString(R.string.common_deposit)
            AssetAction.FiatWithdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_target_select_title)
            else -> resources.getString(R.string.select_a_wallet)
        }
    }

    override fun selectSourceAccountTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap_select_target_title)
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_source_select_title)
            AssetAction.InterestDeposit -> resources.getString(R.string.select_interest_deposit_source_title)
            AssetAction.StakingDeposit -> resources.getString(R.string.select_staking_deposit_source_title)
            else -> resources.getString(R.string.select_a_wallet)
        }

    override fun selectSourceAccountSubtitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap_account_select_subtitle)
            else -> ""
        }

    override fun selectSourceShouldShowSubtitle(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap -> true
            else -> false
        }

    override fun selectSourceShouldShowAddNew(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.FiatDeposit -> true
            else -> false
        }

    override fun selectSourceShouldShowDepositTooltip(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.FiatDeposit -> true
            else -> false
        }

    override fun selectTargetAccountDescription(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.select_target_account_for_swap)
            else -> ""
        }
    }

    override fun enterTargetAddressFragmentState(state: TransactionState): TargetAddressSheetState {
        return if (state.selectedTarget == NullAddress) {
            TargetAddressSheetState.SelectAccountWhenWithinMaxLimit(
                state.availableTargets.map { it as BlockchainAccount }
            )
        } else {
            TargetAddressSheetState.TargetAccountSelected(state.selectedTarget)
        }
    }

    override fun selectIssueType(state: TransactionState): IssueType =
        when (state.errorState) {
            TransactionErrorState.OVER_SILVER_TIER_LIMIT -> IssueType.INFO
            else -> IssueType.ERROR
        }

    override fun issueFlashMessage(state: TransactionState, input: CurrencyType?): String {
        return when (state.errorState) {
            TransactionErrorState.NONE -> ""
            TransactionErrorState.INSUFFICIENT_FUNDS -> resources.getString(
                R.string.not_enough_funds,
                state.amount.currencyCode
            )
            TransactionErrorState.INVALID_AMOUNT -> resources.getString(
                R.string.send_enter_amount_error_invalid_amount_1,
                state.pendingTx?.limits?.minAmount?.formatOrSymbolForZero() ?: throw IllegalStateException(
                    "Missing limit for ${state.sourceAccountType} --" +
                        " ${state.sendingAccount.currency} -- " +
                        "${state.action} --" +
                        " ${state.pendingTx?.amount?.toStringWithSymbol()}"
                )
            )
            TransactionErrorState.INVALID_ADDRESS -> resources.getString(
                R.string.send_error_not_valid_asset_address,
                (state.sendingAccount as SingleAccount).uiCurrency()
            )
            TransactionErrorState.INVALID_DOMAIN -> resources.getString(
                R.string.send_error_invalid_domain
            )
            TransactionErrorState.ADDRESS_IS_CONTRACT -> resources.getString(
                R.string.send_error_address_is_eth_contract
            )
            TransactionErrorState.INVALID_PASSWORD -> resources.getString(
                R.string.send_enter_invalid_password
            )
            TransactionErrorState.NOT_ENOUGH_GAS -> resources.getString(
                R.string.send_enter_insufficient_gas,
                state.sendingAsset.asAssetInfoOrThrow().l1chainTicker ?: state.sendingAsset.displayTicker
            )
            TransactionErrorState.BELOW_MIN_PAYMENT_METHOD_LIMIT,
            TransactionErrorState.BELOW_MIN_LIMIT -> {
                val fiatRate = state.fiatRate ?: return ""
                val amount =
                    input?.let {
                        state.pendingTx?.limits?.minAmount?.toEnteredCurrency(
                            it, fiatRate, RoundingMode.CEILING
                        )
                    } ?: state.pendingTx?.limits?.minAmount?.toStringWithSymbol()
                resources.getString(
                    R.string.minimum_with_value, amount
                )
            }
            TransactionErrorState.ABOVE_MAX_PAYMENT_METHOD_LIMIT,
            TransactionErrorState.OVER_SILVER_TIER_LIMIT,
            TransactionErrorState.OVER_GOLD_TIER_LIMIT -> input?.let {
                aboveMaxErrorMessage(state, it)
            } ?: ""
            TransactionErrorState.TRANSACTION_IN_FLIGHT -> resources.getString(R.string.send_error_tx_in_flight)
            TransactionErrorState.TX_OPTION_INVALID -> resources.getString(R.string.send_error_tx_option_invalid)
            TransactionErrorState.PENDING_ORDERS_LIMIT_REACHED ->
                resources.getString(R.string.too_many_pending_orders_error_message, state.sendingAsset.displayTicker)
        }
    }

    override fun issueFeesTooHighMessage(state: TransactionState): String? {
        return when (state.action) {
            AssetAction.Send ->
                resources.getString(
                    R.string.send_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            AssetAction.Swap ->
                resources.getString(
                    R.string.swap_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            AssetAction.Sell ->
                resources.getString(
                    R.string.sell_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit ->
                resources.getString(
                    R.string.rewards_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            else -> {
                Timber.e("Transaction doesn't support high fees warning message")
                null
            }
        }
    }

    override fun shouldDisplayFeesErrorMessage(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Send,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.InterestDeposit -> true
            AssetAction.StakingDeposit -> true
            else -> false
        }

    override fun installEnterAmountLowerSlotView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState,
    ): EnterAmountWidget =
        when (state.action) {
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.StakingDeposit -> BalanceAndFeeView(ctx).also { frame.addView(it) }
            AssetAction.Sell,
            AssetAction.Swap -> QuickFillRowView(ctx).also {
                frame.addView(it)
            }
            AssetAction.Receive -> SmallBalanceView(ctx).also { frame.addView(it) }
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit -> AccountInfoBank(ctx).also { frame.addView(it) }
            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Sign,
            AssetAction.Buy -> throw IllegalStateException(
                "Enter amount action ${state.action} does not support a bottom widget"
            )
        }

    override fun installEnterAmountUpperSlotView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState,
    ): EnterAmountWidget =
        when (state.action) {
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit -> AccountLimitsView(ctx).also {
                frame.addView(it)
            }
            else -> FromAndToView(ctx).also {
                frame.addView(it)
            }
        }

    override fun installEnterAmountUpperSecondSlotView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState
    ): EnterAmountWidget? =
        when (state.action) {
            AssetAction.Sell,
            AssetAction.Swap -> AvailableBalanceView(ctx).also { balanceView ->
                frame.addView(balanceView)
            }
            else -> {
                frame.gone()
                null
            }
        }

    override fun balanceRowLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Sell -> resources.getString(R.string.enter_amount_balance_row_sell_label)
            AssetAction.Swap -> resources.getString(R.string.enter_amount_balance_row_swap_label)
            else -> throw IllegalStateException("Enter amount balance row label not configured for ${state.action}")
        }

    override fun quickFillRowMaxButtonLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_max)
            AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_max)
            else -> throw IllegalStateException("Enter amount quick fill - ${state.action} not supported")
        }

    override fun installAddressSheetSource(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState,
    ): TxFlowWidget =
        when (state.action) {
            AssetAction.FiatWithdraw -> AccountInfoFiat(ctx).also {
                frame.addView(it)
            }
            else -> AccountInfoCrypto(ctx).also {
                frame.addView(it)
            }
        }

    private fun aboveMaxErrorMessage(state: TransactionState, input: CurrencyType): String {
        if (state.limits.suggestedUpgrade != null) {
            return resources.getString(R.string.over_your_limit)
        } else {
            val fiatRate = state.fiatRate ?: return ""
            val amount = input.let {
                state.pendingTx?.limits?.maxAmount?.toEnteredCurrency(
                    it, fiatRate, RoundingMode.FLOOR
                )
            } ?: state.pendingTx?.limits?.maxAmount?.toStringWithSymbol()
            return resources.getString(
                R.string.maximum_with_value, amount
            )
        }
    }

    override fun showTargetIcon(state: TransactionState): Boolean =
        state.action == AssetAction.Swap

    override fun transactionProgressStandardIcon(state: TransactionState): Int? =
        when (state.action) {
            AssetAction.Swap -> R.drawable.swap_masked_asset
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit,
            -> {
                val sendingCurrency = (state.sendingAccount as? FiatAccount)?.currency?.networkTicker
                when (sendingCurrency) {
                    "GBP" -> R.drawable.ic_funds_gbp_masked
                    "EUR" -> R.drawable.ic_funds_euro_masked
                    "USD" -> R.drawable.ic_funds_usd_masked
                    else -> R.drawable.ic_funds_usd_masked
                }
            }
            else -> null
        }

    override fun transactionProgressExceptionTitle(state: TransactionState): String {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)
        val error = state.executionStatus.exception

        return when (error) {
            TransactionError.OrderLimitReached -> resources.getString(
                R.string.trading_order_limit, getActionStringResource(state.action)
            )
            TransactionError.OrderNotCancelable -> resources.getString(
                R.string.trading_order_not_cancelable, getActionStringResource(state.action)
            )
            TransactionError.WithdrawalAlreadyPending -> resources.getString(
                R.string.trading_pending_withdrawal
            )
            TransactionError.WithdrawalBalanceLocked -> resources.getString(
                R.string.trading_withdrawal_balance_locked
            )
            TransactionError.WithdrawalInsufficientFunds -> resources.getString(
                R.string.trading_withdrawal_insufficient_funds
            )
            TransactionError.InternalServerError -> resources.getString(R.string.trading_internal_server_error)
            TransactionError.TradingTemporarilyDisabled -> resources.getString(
                R.string.trading_service_temp_disabled
            )
            TransactionError.InsufficientBalance -> {
                resources.getString(
                    R.string.trading_insufficient_balance, getActionStringResource(state.action)
                )
            }
            TransactionError.OrderBelowMin -> resources.getString(
                R.string.trading_amount_below_min, getActionStringResource(state.action)
            )
            TransactionError.OrderAboveMax -> resources.getString(
                R.string.trading_amount_above_max, getActionStringResource(state.action)
            )
            TransactionError.SwapDailyLimitExceeded -> resources.getString(
                R.string.trading_daily_limit_exceeded, getActionStringResource(state.action)
            )
            TransactionError.SwapWeeklyLimitExceeded -> resources.getString(
                R.string.trading_weekly_limit_exceeded, getActionStringResource(state.action)
            )
            TransactionError.SwapYearlyLimitExceeded -> resources.getString(
                R.string.trading_yearly_limit_exceeded, getActionStringResource(state.action)
            )
            TransactionError.InvalidCryptoAddress -> resources.getString(R.string.trading_invalid_address)
            TransactionError.InvalidCryptoCurrency -> resources.getString(R.string.trading_invalid_currency)
            TransactionError.InvalidFiatCurrency -> resources.getString(R.string.trading_invalid_fiat)
            TransactionError.OrderDirectionDisabled -> resources.getString(R.string.trading_direction_disabled)
            TransactionError.InvalidOrExpiredQuote -> resources.getString(
                R.string.trading_quote_invalid_or_expired
            )
            TransactionError.IneligibleForSwap -> resources.getString(R.string.trading_ineligible_for_swap)
            TransactionError.InvalidDestinationAmount -> resources.getString(
                R.string.trading_invalid_destination_amount
            )
            TransactionError.InvalidPostcode -> resources.getString(
                R.string.address_verification_postcode_error
            )
            is TransactionError.ExecutionFailed -> resources.getString(
                R.string.executing_transaction_error, state.sendingAsset.displayTicker
            )
            is TransactionError.InternetConnectionError -> resources.getString(
                R.string.executing_connection_error
            )
            is TransactionError.HttpError -> handleNabuApiException(error.nabuApiException)
            TransactionError.InvalidDomainAddress -> resources.getString(
                R.string.invalid_domain_address
            )
            TransactionError.TransactionDenied -> resources.getString(R.string.transaction_denied)
            is TransactionError.FiatDepositError -> getFiatDepositError(error.errorCode).title
            TransactionError.SettlementStaleBalance -> resources.getString(R.string.trading_deposit_title_stale_balance)
            TransactionError.SettlementInsufficientBalance -> resources.getString(
                R.string.bank_transfer_payment_insufficient_funds_title
            )
            TransactionError.SettlementGenericError -> resources.getString(
                R.string.common_oops_bank
            )
            is TransactionError.SettlementRefreshRequired -> resources.getString(R.string.trading_confirm_deposit)
        }
    }

    private fun handleNabuApiException(exception: NabuApiException) =
        exception.getServerSideErrorInfo()?.let {
            it.title
        } ?: run {
            resources.getString(
                R.string.common_http_error_with_message,
                exception.getErrorDescription()
            )
        }

    private fun getFiatDepositError(error: String): FiatDepositErrorContent {
        val errorContent = when (error) {
            BuySellOrderResponse.APPROVAL_ERROR_INVALID,
            BuySellOrderResponse.APPROVAL_ERROR_ACCOUNT_INVALID ->
                Pair(
                    R.string.bank_transfer_payment_invalid_title,
                    R.string.bank_transfer_payment_invalid_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_FAILED ->
                Pair(
                    R.string.bank_transfer_payment_failed_title,
                    R.string.bank_transfer_payment_failed_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_DECLINED ->
                Pair(
                    R.string.bank_transfer_payment_declined_title,
                    R.string.bank_transfer_payment_declined_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_REJECTED ->
                Pair(
                    R.string.bank_transfer_payment_rejected_title,
                    R.string.bank_transfer_payment_rejected_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_EXPIRED ->
                Pair(
                    R.string.bank_transfer_payment_expired_title,
                    R.string.bank_transfer_payment_expired_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_EXCEEDED ->
                Pair(
                    R.string.bank_transfer_payment_limited_exceeded_title,
                    R.string.bank_transfer_payment_limited_exceeded_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_FAILED_INTERNAL ->
                Pair(
                    R.string.bank_transfer_payment_failed_title,
                    R.string.bank_transfer_payment_failed_subtitle
                )
            BuySellOrderResponse.APPROVAL_ERROR_INSUFFICIENT_FUNDS -> Pair(
                R.string.bank_transfer_payment_insufficient_funds_title,
                R.string.bank_transfer_payment_insufficient_funds_subtitle
            )
            else -> Pair(
                R.string.common_oops_bank,
                R.string.send_progress_error_subtitle
            )
        }

        return FiatDepositErrorContent(
            title = resources.getString(errorContent.first),
            message = resources.getString(errorContent.second)
        )
    }

    override fun transactionProgressExceptionDescription(state: TransactionState): String {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)

        return when (val error = state.executionStatus.exception) {
            is TransactionError.HttpError ->
                error.nabuApiException.getServerSideErrorInfo()?.description
                    ?: resources.getString(R.string.send_progress_error_subtitle)
            is TransactionError.FiatDepositError ->
                getFiatDepositError(error.errorCode).message
            is TransactionError.SettlementInsufficientBalance ->
                resources.getString(R.string.trading_deposit_description_insufficient)
            is TransactionError.SettlementStaleBalance ->
                resources.getString(R.string.trading_deposit_description_stale)
            is TransactionError.SettlementGenericError ->
                resources.getString(R.string.trading_deposit_description_generic)
            is TransactionError.SettlementRefreshRequired ->
                resources.getString(R.string.trading_deposit_description_requires_update)
            else ->
                resources.getString(R.string.send_progress_error_subtitle)
        }
    }

    override fun transactionProgressExceptionIcon(state: TransactionState): ErrorStateIcon {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)
        val error = state.executionStatus.exception

        return if (error is TransactionError.HttpError) {
            error.nabuApiException.getServerSideErrorInfo()?.let { nabuError ->
                when {
                    // we have been provided both icon and status
                    nabuError.iconUrl.isNotEmpty() && nabuError.statusUrl.isNotEmpty() -> {
                        ErrorStateIcon.RemoteIconWithStatus(nabuError.iconUrl, nabuError.statusUrl)
                    }
                    // we only have one icon
                    nabuError.iconUrl.isNotEmpty() && nabuError.statusUrl.isEmpty() -> {
                        ErrorStateIcon.RemoteIcon(nabuError.iconUrl)
                    }
                    // no icons provided
                    else -> ErrorStateIcon.Local(R.drawable.ic_alert_white_bkgd)
                }
            } ?: run {
                // no server side error present
                ErrorStateIcon.Local(R.drawable.ic_alert_white_bkgd)
            }
        } else {
            // not an HTTP exception error
            ErrorStateIcon.Local(R.drawable.ic_alert_white_bkgd)
        }
    }

    override fun transactionSettlementExceptionAction(state: TransactionState): SettlementErrorStateAction {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)

        return when (val error = state.executionStatus.exception) {
            is TransactionError.SettlementRefreshRequired ->
                SettlementErrorStateAction.RelinkBank(
                    resources.getString(R.string.trading_deposit_relink_bank_account), error.accountId
                )
            else ->
                SettlementErrorStateAction.None
        }
    }

    override fun transactionProgressExceptionActions(state: TransactionState): List<ServerErrorAction> {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)

        val error = state.executionStatus.exception
        return if (error is TransactionError.HttpError) {
            error.nabuApiException.getServerSideErrorInfo()?.actions ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getActionStringResource(action: AssetAction): String =
        resources.getString(
            when (action) {
                AssetAction.Send -> R.string.common_send
                AssetAction.FiatWithdraw,
                AssetAction.InterestWithdraw,
                -> R.string.common_withdraw
                AssetAction.Swap -> R.string.common_swap
                AssetAction.Sell -> R.string.common_sell
                AssetAction.Sign -> R.string.common_sign
                AssetAction.InterestDeposit,
                AssetAction.StakingDeposit,
                AssetAction.FiatDeposit -> R.string.common_deposit
                AssetAction.ViewActivity -> R.string.common_activity
                AssetAction.Receive -> R.string.common_receive
                AssetAction.ViewStatement -> R.string.common_summary
                AssetAction.Buy -> R.string.common_buy
            }
        )

    override fun amountHeaderConfirmationVisible(state: TransactionState): Boolean =
        state.action != AssetAction.Swap && state.action != AssetAction.Sign

    override fun confirmInstallHeaderView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState,
    ): ConfirmSheetWidget =
        when (state.action) {
            AssetAction.Swap -> SwapInfoHeaderView(ctx).also { frame.addView(it) }
            AssetAction.FiatDeposit,
            AssetAction.FiatWithdraw,
            ->
                SimpleInfoHeaderView(ctx).also {
                    frame.addView(it)
                    it.shouldShowExchange = false
                }
            AssetAction.Sign -> EmptyHeaderView()
            else -> SimpleInfoHeaderView(ctx).also { frame.addView(it) }
        }

    override fun confirmAvailableToTradeBlurb(
        state: TransactionState,
        assetAction: AssetAction,
        context: Context
    ): String? {
        return if (assetAction == AssetAction.FiatDeposit && state.sendingAccount is LinkedBankAccount &&
            state.ffImprovedPaymentUxEnabled
        ) {
            state.depositTerms?.let { depositTerms ->
                StringLocalizationUtil.getFormattedDepositTerms(
                    resources = context.resources,
                    displayMode = depositTerms.availableToTradeDisplayMode,
                    min = depositTerms.availableToTradeMinutesMin,
                    max = depositTerms.availableToTradeMinutesMax
                )
            }
        } else null
    }

    override fun confirmAvailableToWithdrawBlurb(
        state: TransactionState,
        assetAction: AssetAction,
        context: Context
    ): String? {
        return if (assetAction == AssetAction.FiatDeposit && state.sendingAccount is LinkedBankAccount &&
            state.ffImprovedPaymentUxEnabled
        ) {
            state.depositTerms?.let { depositTerms ->
                StringLocalizationUtil.getFormattedDepositTerms(
                    resources = context.resources,
                    displayMode = depositTerms.availableToWithdrawDisplayMode,
                    min = depositTerms.availableToWithdrawMinutesMin,
                    max = depositTerms.availableToWithdrawMinutesMax
                )
            }
        } else null
    }

    override fun confirmAchDisclaimerBlurb(
        state: TransactionState,
        assetAction: AssetAction,
        context: Context
    ): AchDisclaimerBlurb? {
        return if (assetAction == AssetAction.FiatDeposit && state.sendingAccount is LinkedBankAccount &&
            state.ffImprovedPaymentUxEnabled && state.sendingAccount.isAchCurrency()
        ) {
            val amount = state.amount.toStringWithSymbol()
            val bankLabel = state.sendingAccount.label
            val infoText = String.format(context.getString(R.string.deposit_terms_ach_info), amount, bankLabel)
            val withdrawalLock = if (state.pendingTx?.engineState?.containsKey(WITHDRAW_LOCKS) == true) {
                state.pendingTx.engineState[WITHDRAW_LOCKS].toString()
            } else {
                "7"
            }

            AchDisclaimerBlurb(
                value = infoText,
                amount = amount,
                bankLabel = bankLabel,
                withdrawalLock = withdrawalLock
            )
        } else null
    }

    override fun defInputType(state: TransactionState, fiatCurrency: Currency): Currency =
        when (state.action) {
            AssetAction.Swap,
            AssetAction.Sell -> fiatCurrency
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit -> state.amount.currency
            else -> state.sendingAsset
        }

    override fun sourceAccountSelectionStatusDecorator(
        state: TransactionState,
        walletMode: WalletMode
    ): StatusDecorator =
        when (state.action) {
            AssetAction.Swap -> {
                {
                    SwapAccountSelectSheetFeeDecorator(
                        account = it,
                        walletMode = walletMode
                    )
                }
            }
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit,
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit,
            AssetAction.Sell -> {
                {
                    DefaultCellDecorator()
                }
            }
            else -> throw IllegalStateException("Action is not supported")
        }

    override fun getLinkingSourceForAction(state: TransactionState): BankAuthSource =
        when (state.action) {
            AssetAction.FiatDeposit -> {
                BankAuthSource.DEPOSIT
            }
            AssetAction.FiatWithdraw -> {
                BankAuthSource.WITHDRAW
            }
            else -> {
                throw IllegalStateException("Attempting to link from an unsupported action")
            }
        }

    override fun selectSourceShouldHaveSearch(action: AssetAction): Boolean =
        when (action) {
            AssetAction.Swap -> true
            else -> false
        }

    override fun shouldShowSourceAccountWalletsSwitch(action: AssetAction): Boolean =
        action in listOf(
            AssetAction.StakingDeposit, AssetAction.InterestDeposit
        )

    override fun getBackNavigationAction(state: TransactionState): BackNavigationState =
        when (state.currentStep) {
            TransactionStep.ENTER_ADDRESS -> BackNavigationState.ClearTransactionTarget
            TransactionStep.ENTER_AMOUNT -> {
                if (state.sendingAccount is LinkedBankAccount ||
                    (state.selectedTarget is CustodialInterestAccount && state.action == AssetAction.InterestDeposit) ||
                    (state.selectedTarget is CustodialStakingAccount && state.action == AssetAction.StakingDeposit)
                ) {
                    BackNavigationState.ResetPendingTransactionKeepingTarget
                } else {
                    BackNavigationState.ResetPendingTransaction
                }
            }
            else -> BackNavigationState.NavigateToPreviousScreen
        }

    override fun getScreenTitle(state: TransactionState): String =
        when (state.currentStep) {
            TransactionStep.ENTER_PASSWORD -> resources.getString(R.string.transfer_second_pswd_title)
            TransactionStep.FEATURE_BLOCKED -> when (state.featureBlockedReason) {
                is BlockedReason.TooManyInFlightTransactions,
                is BlockedReason.NotEligible,
                is BlockedReason.Sanctions,
                is BlockedReason.ShouldAcknowledgeStakingWithdrawal -> selectTargetAddressTitle(state)
                is BlockedReason.InsufficientTier -> resources.getString(R.string.kyc_upgrade_now_toolbar)
                null -> throw IllegalStateException(
                    "No featureBlockedReason provided for TransactionStep.FEATURE_BLOCKED, state $state"
                )
            }
            TransactionStep.SELECT_SOURCE -> selectSourceAccountTitle(state)
            TransactionStep.ENTER_ADDRESS -> selectTargetAddressTitle(state)
            TransactionStep.SELECT_TARGET_ACCOUNT -> selectTargetAccountTitle(state)
            TransactionStep.ENTER_AMOUNT -> enterAmountTitle(state)
            TransactionStep.CONFIRM_DETAIL -> confirmTitle(state)
            TransactionStep.IN_PROGRESS,
            TransactionStep.ZERO,
            TransactionStep.CLOSED,
            -> ""
        }

    override fun sendToDomainCardTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_domain_alert_title)
            else -> ""
        }
    }

    override fun sendToDomainCardDescription(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_domain_alert_description)
            else -> ""
        }
    }

    override fun shouldShowSendToDomainBanner(state: TransactionState): Boolean {
        return when (state.action) {
            AssetAction.Send -> state.shouldShowSendToDomainBanner
            else -> false
        }
    }

    override fun selectTargetNetworkDescription(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> if (state.selectedTarget is MultiChainAccount) {
                resources.getString(
                    R.string.send_select_wallet_warning_sheet_desc,
                    state.sendingAsset.displayTicker,
                    state.selectedTarget.l1Network.networkName
                )
            } else {
                ""
            }
            else -> ""
        }
    }

    override fun shouldShowSelectTargetNetworkDescription(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Send -> state.selectedTarget is MultiChainAccount
            else -> false
        }

    companion object {
        private const val FIVE_DAYS = 5

        fun getEstimatedTransactionCompletionTime(daysInFuture: Int = FIVE_DAYS): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysInFuture)
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(cal.time)
        }
    }
}

private fun SingleAccount.uiCurrency(): String {
    return this.currency.displayTicker
}

enum class IssueType {
    ERROR,
    INFO
}

sealed class TargetAddressSheetState(val accounts: List<TransactionTarget>) {
    class TargetAccountSelected(account: TransactionTarget) : TargetAddressSheetState(listOf(account))
    class SelectAccountWhenWithinMaxLimit(accounts: List<BlockchainAccount>) :
        TargetAddressSheetState(accounts.map { it as TransactionTarget })
}

fun Money.toEnteredCurrency(
    input: CurrencyType,
    exchangeRate: ExchangeRate,
    roundingMode: RoundingMode,
): String =
    when {
        input == this.currency.type -> toStringWithSymbol()
        input == CurrencyType.FIAT && this is CryptoValue -> {
            Money.fromMajor(
                exchangeRate.to,
                exchangeRate.convert(this, round = false).toBigDecimal().setScale(
                    exchangeRate.to.precisionDp, roundingMode
                )
            ).toStringWithSymbol()
        }
        input == CurrencyType.CRYPTO && this.currency.type == CurrencyType.FIAT -> exchangeRate.inverse()
            .convert(this).toStringWithSymbol()
        else -> throw IllegalStateException("Not valid currency")
    }

data class FiatDepositErrorContent(
    val title: String,
    val message: String,
)
