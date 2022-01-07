package piuk.blockchain.android.ui.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * An abstract class which delegates all important functions to registered [AdapterDelegate]
 * objects. Extend this class as necessary and simply add [AdapterDelegate] objects to the
 * [AdapterDelegatesManager] to handle the different [View] types.
 *
 * @param T The type of object being held in the adapter's [List], must implement [Diffable],
 * for @see [DelegationAdapter] for non DiffUtil usages
 */
abstract class DelegationDiffAdapter<T : Diffable<T>>(
    protected val delegatesManager: AdapterDelegatesManager<T>
) : ListAdapter<T, RecyclerView.ViewHolder>(createDiffUtil()) {

    protected val compositeDisposable = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegatesManager.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        delegatesManager.onBindViewHolder(currentList, position, holder, null)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) = delegatesManager.onBindViewHolder(currentList, position, holder, payloads)

    override fun getItemViewType(position: Int): Int =
        delegatesManager.getItemViewType(currentList, position)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        compositeDisposable.clear()
    }

    companion object {
        private fun <T : Diffable<T>> createDiffUtil() = object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = oldItem.areItemsTheSame(newItem)
            override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem.areContentsTheSame(newItem)
        }
    }
}
