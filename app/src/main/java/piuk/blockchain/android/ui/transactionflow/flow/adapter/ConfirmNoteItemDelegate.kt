package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.text.Editable
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmNoteBinding
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.activity.detail.adapter.MAX_NOTE_LENGTH
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.AfterTextChangedWatcher

class ConfirmNoteItemDelegate<in T>(
    private val model: TransactionModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxConfirmationValue.Description
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        NoteItemViewHolder(
            ItemSendConfirmNoteBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            model
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as NoteItemViewHolder).bind(
        items[position] as TxConfirmationValue.Description
    )
}

private class NoteItemViewHolder(private val binding: ItemSendConfirmNoteBinding, private val model: TransactionModel) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.confirmDetailsNoteInput.apply {
            inputType = INPUT_FIELD_FLAGS
            filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))
            addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    model.process(
                        TransactionIntent.ModifyTxOption(TxConfirmationValue.Description(text = s.toString()))
                    )
                }
            })
        }
    }

    fun bind(
        item: TxConfirmationValue.Description
    ) {
        binding.confirmDetailsNoteInput.setText(item.text, TextView.BufferType.EDITABLE)
    }
}
