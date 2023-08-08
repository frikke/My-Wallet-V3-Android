package piuk.blockchain.android.util

import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * Allows us to dispatch changes to a [RecyclerView] and have the diff calculated automatically
 * regardless of the item type.
 *
 * @param oldList The old list of items
 * @param newList The new, updated list of items
 * @param compare A function which returns [Boolean], handling the comparison of the objects
 */
fun <T> RecyclerView.Adapter<*>.autoNotify(
    oldList: List<T>,
    newList: List<T>,
    compare: (T, T) -> Boolean
) {
    val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            compare(oldList[oldItemPosition], newList[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size
    })

    diff.dispatchUpdatesTo(this)
}

/**
 * Returns the current [Context] for the [RecyclerView.ViewHolder].
 */
val RecyclerView.ViewHolder.context: Context
    get() = this.itemView.context
