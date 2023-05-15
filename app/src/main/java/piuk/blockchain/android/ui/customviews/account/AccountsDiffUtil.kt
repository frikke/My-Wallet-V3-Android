package piuk.blockchain.android.ui.customviews.account

import androidx.recyclerview.widget.DiffUtil
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.SingleAccount

internal class AccountsDiffUtil(
    private val oldAccounts: List<AccountsListItem>,
    private val newAccounts: List<AccountsListItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldAccounts.size

    override fun getNewListSize(): Int = newAccounts.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldAccounts[oldItemPosition] == newAccounts[newItemPosition]) return true
        val oldItem = (oldAccounts[oldItemPosition] as? SelectableAccountItem)?.item ?: return false
        val newItem = (newAccounts[newItemPosition] as? SelectableAccountItem)?.item ?: return false
        return oldItem.account.isTheSameWith(newItem.account)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldAccounts[oldItemPosition] == newAccounts[newItemPosition]) return true
        val oldItem = (oldAccounts[oldItemPosition] as? SelectableAccountItem)?.item ?: return false
        val newItem = (newAccounts[newItemPosition] as? SelectableAccountItem)?.item ?: return false
        return oldItem.account.isTheSameWith(newItem.account) &&
            (oldAccounts[oldItemPosition] as? SelectableAccountItem)?.isSelected ==
            (newAccounts[newItemPosition] as? SelectableAccountItem)?.isSelected
    }
}

internal fun BlockchainAccount.isTheSameWith(other: BlockchainAccount): Boolean =
    this::class == other::class &&
        this.hasTheSameAsset(other) &&
        this.label == other.label

private fun BlockchainAccount.hasTheSameAsset(other: BlockchainAccount): Boolean {
    val otherCurrency = (other as? SingleAccount)?.currency ?: return false
    val thisCurrency = (this as? SingleAccount)?.currency ?: return false

    return otherCurrency == thisCurrency
}
