package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.res.Resources
import android.graphics.Typeface.BOLD
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.impl.txEngine.interest.TransferData
import com.blockchain.coincore.toFiat
import com.blockchain.componentlib.viewextensions.updateItemBackground
import com.blockchain.core.price.ExchangeRates
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemSendConfirmAgreementCheckboxBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.context

class ConfirmAgreementToTransferItemDelegate<in T>(
    private val model: TransactionModel,
    private val exchangeRates: ExchangeRates,
    private val selectedCurrency: FiatCurrency
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue.TxBooleanConfirmation<*>)?.data?.let {
            it is TransferData
        } ?: false

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementTextItemViewHolder(
            ItemSendConfirmAgreementCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            exchangeRates,
            selectedCurrency
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementTextItemViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<TransferData>,
        model,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class AgreementTextItemViewHolder(
    private val binding: ItemSendConfirmAgreementCheckboxBinding,
    private val exchangeRates: ExchangeRates,
    private val selectedCurrency: FiatCurrency
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<TransferData>,
        model: TransactionModel,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            item.data?.let { data ->
                val text = when (data) {
                    is TransferData.Interest -> interestText(
                        data.amount,
                        exchangeRates,
                        selectedCurrency,
                        context.resources
                    )

                    is TransferData.Staking -> stakingText(
                        data,
                        exchangeRates,
                        selectedCurrency,
                        context.resources
                    )

                    is TransferData.ActiveRewards -> activeRewardsText(
                        data,
                        exchangeRates,
                        selectedCurrency,
                        context.resources
                    )
                }

                confirmDetailsCheckboxText.setText(
                    text,
                    TextView.BufferType.SPANNABLE
                )
            }

            confirmDetailsCheckbox.setOnCheckedChangeListener { _, isChecked ->
                model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            }
        }
    }

    private fun stakingText(
        data: TransferData.Staking,
        exchangeRates: ExchangeRates,
        selectedCurrency: FiatCurrency,
        resources: Resources
    ): SpannableStringBuilder {
        val withdrawalsDisabled = data.stakingLimits.withdrawalsDisabled
        val agree = resources.getString(com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_1)
        val amountInFiat =
            data.amount.toFiat(selectedCurrency, exchangeRates).toStringWithSymbol()
        val amountInBold =
            resources.getString(
                com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_2,
                amountInFiat
            )
        val stakingAcc = resources.getString(
            com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_2_1
        )
        val lockedOnNetwork = resources.getString(
            com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_3,
            data.amount.currency.networkTicker
        )
        val fundsSubject = resources.getString(
            com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_4
        )
        val bondingInBold =
            resources.getString(
                com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_5,
                data.stakingLimits.bondingDays
            )
        val daysInBold = resources.getQuantityString(
            com.blockchain.stringResources.R.plurals.staking_confirmation_bonding_period_6,
            data.stakingLimits.bondingDays
        )
        val end = resources.getString(com.blockchain.stringResources.R.string.staking_confirmation_bonding_period_7)
        val sb = SpannableStringBuilder().run {
            append(agree)
            append(amountInBold)
            append(stakingAcc)
            if (withdrawalsDisabled) {
                append(lockedOnNetwork)
            }
            append(fundsSubject)
            append(bondingInBold)
            append(daysInBold)
            append(end)
        }

        sb.setSpan(
            StyleSpan(BOLD),
            agree.length,
            agree.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        sb.setSpan(
            StyleSpan(BOLD),
            agree.length + amountInBold.length + stakingAcc.length +
                if (withdrawalsDisabled) {
                    lockedOnNetwork.length
                } else 0 + fundsSubject.length,
            agree.length + amountInBold.length + stakingAcc.length +
                if (withdrawalsDisabled) {
                    lockedOnNetwork.length
                } else 0 + fundsSubject.length + bondingInBold.length + daysInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return sb
    }

    private fun activeRewardsText(
        data: TransferData.ActiveRewards,
        exchangeRates: ExchangeRates,
        selectedCurrency: FiatCurrency,
        resources: Resources
    ): SpannableStringBuilder {
        val agree = resources.getString(
            com.blockchain.stringResources.R.string.active_rewards_confirmation_bonding_period_1
        )
        val amountInFiat =
            data.amount.toFiat(selectedCurrency, exchangeRates).toStringWithSymbol()
        val amountInBold =
            resources.getString(
                com.blockchain.stringResources.R.string.active_rewards_confirmation_bonding_period_2,
                amountInFiat
            )
        val account = resources.getString(
            com.blockchain.stringResources.R.string.active_rewards_confirmation_bonding_period_3
        )
        val balanceChange = resources.getString(
            com.blockchain.stringResources.R.string.active_rewards_confirmation_bonding_period_4,
            data.amount.currency.displayTicker
        )
        val sb = SpannableStringBuilder().run {
            append(agree)
            append(amountInBold)
            append(account)
            append(balanceChange)
        }

        sb.setSpan(
            StyleSpan(BOLD),
            agree.length,
            agree.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return sb
    }

    private fun interestText(
        amount: Money,
        exchangeRates: ExchangeRates,
        selectedCurrency: FiatCurrency,
        resources: Resources
    ): SpannableStringBuilder {
        val introToHolding = resources.getString(
            com.blockchain.stringResources.R.string.send_confirmation_rewards_holding_period_1
        )
        val amountInBold =
            amount.toFiat(selectedCurrency, exchangeRates).toStringWithSymbol()
        val outroToHolding = context.resources.getString(
            com.blockchain.stringResources.R.string.send_confirmation_rewards_holding_period_2,
            amount.toStringWithSymbol(),
            (amount as CryptoValue).currency.name
        )
        val sb = SpannableStringBuilder()
            .append(introToHolding)
            .append(amountInBold)
            .append(outroToHolding)
        sb.setSpan(
            StyleSpan(BOLD),
            introToHolding.length,
            introToHolding.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return sb
    }
}

class ConfirmAgreementToWithdrawalBlockedItemDelegate<in T>(
    private val model: TransactionModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue.TxBooleanConfirmation<*>)?.let {
            it.data is TransferData.ActiveRewards &&
                it.confirmation == TxConfirmation.AGREEMENT_ACTIVE_REWARDS_WITHDRAWAL_DISABLED
        } ?: false

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementWithdrawalBlockedTextItemViewHolder(
            ItemSendConfirmAgreementCheckboxBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementWithdrawalBlockedTextItemViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<TransferData.ActiveRewards>,
        model,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}
private class AgreementWithdrawalBlockedTextItemViewHolder(
    private val binding: ItemSendConfirmAgreementCheckboxBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<TransferData.ActiveRewards>,
        model: TransactionModel,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            item.data?.let { data ->
                val text = activeRewardsText(
                    data,
                    context.resources
                )

                confirmDetailsCheckboxText.setText(
                    text,
                    TextView.BufferType.SPANNABLE
                )
            }

            confirmDetailsCheckbox.setOnCheckedChangeListener { _, isChecked ->
                model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            }
        }
    }

    private fun activeRewardsText(
        data: TransferData.ActiveRewards,
        resources: Resources
    ): SpannableStringBuilder {
        val agree = resources.getString(
            com.blockchain.stringResources.R.string.active_rewards_agreement_withdrawal_blocked,
            data.amount.currency.displayTicker
        )
        val sb = SpannableStringBuilder().run {
            append(agree)
        }

        return sb
    }
}
