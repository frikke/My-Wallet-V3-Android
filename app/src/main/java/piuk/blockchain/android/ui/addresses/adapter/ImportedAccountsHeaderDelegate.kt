package piuk.blockchain.android.ui.addresses.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemAccountsRowHeaderBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class ImportedAccountsHeaderDelegate(
    val listener: AccountAdapter.Listener
) : AdapterDelegate<AccountListItem> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        HeaderViewHolder(
            ItemAccountsRowHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AccountListItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) {
        val headerViewHolder = holder as HeaderViewHolder
        headerViewHolder.bind(items[position] as AccountListItem.ImportedHeader, listener)
    }

    override fun isForViewType(items: List<AccountListItem>, position: Int): Boolean =
        items[position] is AccountListItem.ImportedHeader

    private class HeaderViewHolder constructor(
        binding: ItemAccountsRowHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val header: TextView = binding.headerName
        private val plus: ImageView = binding.imageviewPlus

        fun bind(item: AccountListItem.ImportedHeader, listener: AccountAdapter.Listener) {
            header.setText(com.blockchain.stringResources.R.string.imported_addresses)

            if (item.enableImport) {
                itemView.setOnClickListener { listener.onImportAddressClicked() }
                plus.visible()
            } else {
                itemView.setOnClickListener(null)
                plus.gone()
            }
            itemView.contentDescription = header.text
        }
    }
}
