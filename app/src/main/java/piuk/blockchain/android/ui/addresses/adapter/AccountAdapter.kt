package piuk.blockchain.android.ui.addresses.adapter

import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import kotlin.properties.Delegates
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.autoNotify

sealed class AccountListItem {

    data class Account(
        val account: CryptoNonCustodialAccount
    ) : AccountListItem()

    data class InternalHeader(val enableCreate: Boolean) : AccountListItem()
    data class ImportedHeader(val enableImport: Boolean) : AccountListItem()
}

class AccountAdapter(
    listener: Listener
) : DelegationAdapter<AccountListItem>(AdapterDelegatesManager(), emptyList()) {

    interface Listener {
        fun onCreateNewClicked()
        fun onImportAddressClicked()
        fun onAccountClicked(account: SingleAccount)
    }

    init {
        delegatesManager.apply {
            addAdapterDelegate(InternalAccountsHeaderDelegate(listener))
            addAdapterDelegate(ImportedAccountsHeaderDelegate(listener))
            addAdapterDelegate(AccountDelegate(listener))
        }
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<AccountListItem> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { _, _ -> false }
//        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()
}
