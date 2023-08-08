package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackground
import com.bumptech.glide.Glide
import piuk.blockchain.android.databinding.ItemWalletConnectCheckoutHeaderBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class HeaderConfirmationDelegate : AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean =
        items[position].confirmation == TxConfirmation.HEADER

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ItemWalletConnectHeaderViewHolder(
            ItemWalletConnectCheckoutHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(items: List<TxConfirmationValue>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as ItemWalletConnectHeaderViewHolder).bind(
            items[position] as TxConfirmationValue.WalletConnectHeader,
            isFirstItemInList = position == 0,
            isLastItemInList = items.lastIndex == position
        )
}

private class ItemWalletConnectHeaderViewHolder(private val binding: ItemWalletConnectCheckoutHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.WalletConnectHeader,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            Glide.with(context).load(item.dAppLogo).into(icon)
            name.text = item.dAppName
            url.text = item.dAppUrl
        }
    }
}
