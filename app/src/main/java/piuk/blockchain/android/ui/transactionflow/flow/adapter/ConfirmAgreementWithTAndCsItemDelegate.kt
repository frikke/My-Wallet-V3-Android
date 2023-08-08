package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackground
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemSendConfirmAgreementCheckboxBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.urllinks.INTEREST_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.INTEREST_TERMS_OF_SERVICE
import piuk.blockchain.android.util.StringUtils

class ConfirmAgreementWithTAndCsItemDelegate<in T>(
    private val model: TransactionModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementItemViewHolder(
            ItemSendConfirmAgreementCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementItemViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class AgreementItemViewHolder(private val binding: ItemSendConfirmAgreementCheckboxBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model: TransactionModel,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        val linksMap = mapOf<String, Uri>(
            "interest_tos" to Uri.parse(INTEREST_TERMS_OF_SERVICE),
            "interest_pp" to Uri.parse(INTEREST_PRIVACY_POLICY)
        )

        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            confirmDetailsCheckboxText.apply {
                text = StringUtils.getStringWithMappedAnnotations(
                    binding.root.context,
                    com.blockchain.stringResources.R.string.send_confirmation_rewards_tos_pp,
                    linksMap
                )
                movementMethod = LinkMovementMethod.getInstance()
            }

            confirmDetailsCheckboxText.movementMethod = LinkMovementMethod.getInstance()

            confirmDetailsCheckbox.setOnCheckedChangeListener { _, isChecked ->
                model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            }
        }
    }
}
