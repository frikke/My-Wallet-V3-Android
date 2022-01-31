package piuk.blockchain.android.ui.transactionflow.flow.adapter

import androidx.recyclerview.widget.DiffUtil
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.UserEditable
import com.blockchain.core.price.ExchangeRates
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout

class ConfirmTransactionDelegateAdapter(
    model: TransactionModel,
    mapper: TxConfirmReadOnlyMapperCheckout,
    exchangeRates: ExchangeRates,
    selectedCurrency: FiatCurrency
) : DelegationAdapter<TxConfirmationValue>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<TxConfirmationValue> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(TxConfirmationValueDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            // New checkout screens:
            addAdapterDelegate(SimpleConfirmationCheckoutDelegate(mapper))
            addAdapterDelegate(ComplexConfirmationCheckoutDelegate(mapper))
            addAdapterDelegate(ExpandableSimpleConfirmationCheckout(mapper))
            addAdapterDelegate(ExpandableSingleValueConfirmationAdapter(mapper))
            addAdapterDelegate(HeaderConfirmationDelegate())
            addAdapterDelegate(ExpandableComplexConfirmationCheckout(mapper))
            addAdapterDelegate(CompoundExpandableFeeConfirmationCheckoutDelegate(mapper))

            addAdapterDelegate(ConfirmNoteItemDelegate(model))
            addAdapterDelegate(ConfirmXlmMemoItemDelegate(model))
            addAdapterDelegate(ConfirmAgreementWithTAndCsItemDelegate(model))
            addAdapterDelegate(
                ConfirmAgreementToTransferItemDelegate(
                    model,
                    exchangeRates,
                    selectedCurrency
                )
            )
            addAdapterDelegate(LargeTransactionWarningItemDelegate(model))
            addAdapterDelegate(InvoiceCountdownTimerDelegate())
            addAdapterDelegate(ConfirmInfoItemValidationStatusDelegate())
        }
    }
}

/**
 * In case of something being UserEditable, we want the model to update the ui only once. Then when the user updates
 * the UI, we want only the model to get updated to avoid any racing conditions of having both the user and the model
 * updating the confirmation value
 *
 */

class TxConfirmationValueDiffUtil(
    private val oldItems: List<TxConfirmationValue>,
    private val newItems: List<TxConfirmationValue>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        when {
            oldItems[oldItemPosition] is UserEditable && newItems[newItemPosition] is UserEditable -> true
            else -> oldItems[oldItemPosition] == newItems[newItemPosition]
        }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = when {
        oldItems[oldItemPosition] is UserEditable && newItems[newItemPosition] is UserEditable -> true
        else -> oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
