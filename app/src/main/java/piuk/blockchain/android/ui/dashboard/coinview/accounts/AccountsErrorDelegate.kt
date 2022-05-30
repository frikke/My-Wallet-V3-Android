package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.alert.AlertType
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewItemErrorBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem
import piuk.blockchain.android.util.context

class AccountErrorDelegate : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.AccountError

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AccountErrorCardViewHolder(
            ViewCoinviewItemErrorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AccountErrorCardViewHolder).bind()
}

private class AccountErrorCardViewHolder(
    val binding: ViewCoinviewItemErrorBinding
) : RecyclerView.ViewHolder(binding.root as View) {

    fun bind() {
        with(binding) {
            itemLabel.title = context.getString(R.string.coinview_accounts_label)

            itemErrorCard.apply {
                isDismissable = false
                title = context.getString(R.string.coinview_account_load_error_title)
                subtitle = context.getString(R.string.coinview_account_load_error_subtitle)
                isBordered = true
                alertType = AlertType.Warning
            }
        }
    }
}
