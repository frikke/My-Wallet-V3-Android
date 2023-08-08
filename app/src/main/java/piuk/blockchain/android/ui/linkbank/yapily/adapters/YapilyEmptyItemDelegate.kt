package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemBankEmptyBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class BankInfoEmptyDelegate(private val onAddNewBankClicked: () -> Unit) : AdapterDelegate<YapilyInstitution> {
    override fun isForViewType(items: List<YapilyInstitution>, position: Int): Boolean = items.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyBankEmptyViewHolder(
            ItemBankEmptyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<YapilyInstitution>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as YapilyBankEmptyViewHolder).bind(onAddNewBankClicked)
}

class YapilyBankEmptyViewHolder(val binding: ItemBankEmptyBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(listener: () -> Unit) {
        with(binding) {
            addNewButton.apply {
                text = context.getString(com.blockchain.stringResources.R.string.yapily_empty_cta)
                onClick = { listener.invoke() }
            }
        }
    }
}
