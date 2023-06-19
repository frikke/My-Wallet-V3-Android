package piuk.blockchain.android.ui.addresses.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagView
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemAccountsRowBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.account.AccountListViewItem
import piuk.blockchain.android.ui.customviews.account.CellDecorator

class AccountDelegate(
    val listener: AccountAdapter.Listener
) : AdapterDelegate<AccountListItem> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AccountViewHolder(
            ItemAccountsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AccountListItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) {
        val accountViewHolder = holder as AccountViewHolder
        accountViewHolder.bind(items[position] as AccountListItem.Account, listener, position)
    }

    override fun isForViewType(items: List<AccountListItem>, position: Int) =
        (items[position] is AccountListItem.Account)

    private class AccountViewHolder(
        private val binding: ItemAccountsRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: AccountListItem.Account,
            listener: AccountAdapter.Listener,
            position: Int
        ) {
            with(binding) {
                if (item.account.isArchived) {
                    // Show the archived item
                    accountDetails.gone()
                    accountDetailsArchived.visible()
                    accountDetailsArchived.updateAccount(
                        item.account
                    ) { listener.onAccountClicked(it) }
                    accountDetails.contentDescription = ""
                    accountDetailsArchived.contentDescription = item.account.label
                } else {
                    // Show the normal item
                    accountDetailsArchived.gone()
                    accountDetails.visible()
                    accountDetails.updateItem(
                        AccountListViewItem(item.account),
                        { listener.onAccountClicked(it) },
                        DefaultAccountCellDecorator(item.account)
                    )
                    accountDetailsArchived.contentDescription = ""
                    accountDetails.contentDescription = item.account.label
                }
            }
        }
    }
}

class DefaultAccountCellDecorator(private val account: CryptoAccount) : CellDecorator {
    override fun view(context: Context): Maybe<View> =
        if (account.isDefault) {
            defaultLabel(context)
        } else {
            Maybe.empty()
        }

    private fun defaultLabel(context: Context): Maybe<View> =
        Maybe.just(
            TagView(context).apply {
                tag = TagViewState(
                    value = context.getString(com.blockchain.stringResources.R.string.default_label),
                    type = TagType.Success()
                )
            }
        )

    override fun isEnabled(): Single<Boolean> = Single.just(true)
}
