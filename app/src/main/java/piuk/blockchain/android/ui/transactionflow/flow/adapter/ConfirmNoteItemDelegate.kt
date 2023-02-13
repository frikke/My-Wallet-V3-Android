package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackground
import piuk.blockchain.android.databinding.ItemSendConfirmNoteBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.AfterTextChangedWatcher

const val MAX_NOTE_LENGTH = 255

const val INPUT_FIELD_FLAGS: Int = (
    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
        InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or
        InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
    )

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
        items[position] as TxConfirmationValue.Description,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
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
        item: TxConfirmationValue.Description,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)
            confirmDetailsNoteInput.setText(item.text, TextView.BufferType.EDITABLE)
        }
    }
}
