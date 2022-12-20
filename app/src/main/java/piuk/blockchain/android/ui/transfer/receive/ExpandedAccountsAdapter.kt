package piuk.blockchain.android.ui.transfer.receive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CryptoAccount
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.databinding.ItemAccountExpandedBinding

sealed class ExpandedCryptoItem {
    object Loading : ExpandedCryptoItem()
    data class Loaded(val account: CryptoAccount, val onAccountClicked: (CryptoAccount) -> Unit) : ExpandedCryptoItem()
}

class ExpandedAccountsAdapter(
    private val compositeDisposable: CompositeDisposable
) : RecyclerView.Adapter<ExpandedAccountViewHolder>() {

    var items: List<ExpandedCryptoItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpandedAccountViewHolder =
        ExpandedAccountViewHolder(
            compositeDisposable,
            ItemAccountExpandedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ExpandedAccountViewHolder, position: Int) {
        holder.bind(
            items[position]
        )
    }
}

class ExpandedAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    private val binding: ItemAccountExpandedBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(expandedItem: ExpandedCryptoItem) {
        when (expandedItem) {
            is ExpandedCryptoItem.Loading -> renderLoadingState()
            is ExpandedCryptoItem.Loaded -> renderLoadedState(expandedItem)
        }
    }

    private fun renderLoadingState() {
        with(binding) {
            accountProgress.visibility = View.VISIBLE
            accountGroup.visibility = View.GONE
        }
    }

    private fun renderLoadedState(expandedItem: ExpandedCryptoItem.Loaded) {
        with(binding) {
            accountProgress.visibility = View.GONE
            accountGroup.visibility = View.VISIBLE
            icon.updateIcon(expandedItem.account)
            walletName.text = expandedItem.account.label
            root.setOnClickListener { expandedItem.onAccountClicked(expandedItem.account) }
            compositeDisposable += expandedItem.account.balanceRx
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { accountBalance ->
                    walletBalance.text = accountBalance.total.toStringWithSymbol()
                }
        }
    }
}
