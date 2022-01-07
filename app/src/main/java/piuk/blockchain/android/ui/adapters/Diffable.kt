package piuk.blockchain.android.ui.adapters

/**
 * An interface which contains the methods necessary for using RecyclerView's
 * [androidx.recyclerview.widget.DiffUtil.ItemCallback]
 */
interface Diffable<T> {
    fun areItemsTheSame(otherItem: T): Boolean
    fun areContentsTheSame(otherItem: T): Boolean
}
