package piuk.blockchain.android.simplebuy

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemCheckoutComplexInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutSimpleExpandableInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutSimpleInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.visibleIf

class CheckoutAdapterDelegate : DelegationAdapter<SimpleBuyCheckoutItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(SimpleCheckoutItemDelegate())
            addAdapterDelegate(ComplexCheckoutItemDelegate())
            addAdapterDelegate(ExpandableCheckoutItemDelegate())
        }
    }
}

sealed class SimpleBuyCheckoutItem {
    data class SimpleCheckoutItem(val label: String, val title: String, val isImportant: Boolean = false) :
        SimpleBuyCheckoutItem()

    data class ComplexCheckoutItem(val label: String, val title: String, val subtitle: String) :
        SimpleBuyCheckoutItem()

    data class ExpandableCheckoutItem(
        val label: String,
        val title: String,
        val expandableContent: CharSequence,
        val promoLayout: View? = null
    ) :
        SimpleBuyCheckoutItem()
}

class SimpleCheckoutItemDelegate : AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.SimpleCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SimpleCheckoutItemViewHolder(
            ItemCheckoutSimpleInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as SimpleCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.SimpleCheckoutItem
    )
}

private class SimpleCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.SimpleCheckoutItem) {
        with(binding) {
            simpleItemTitle.text = item.title
            simpleItemLabel.text = item.label

            if (item.isImportant) {
                simpleItemLabel.setTextAppearance(R.style.Text_Semibold_16)
                simpleItemTitle.setTextAppearance(R.style.Text_Semibold_16)
            } else {
                simpleItemLabel.setTextAppearance(R.style.Text_Standard_14)
                simpleItemTitle.setTextAppearance(R.style.Text_Standard_14)
            }
        }
    }
}

class ComplexCheckoutItemDelegate : AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ComplexCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ComplexCheckoutItemItemViewHolder(
            ItemCheckoutComplexInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ComplexCheckoutItemItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ComplexCheckoutItem
    )
}

private class ComplexCheckoutItemItemViewHolder(
    val binding: ItemCheckoutComplexInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.ComplexCheckoutItem) {
        with(binding) {
            complexItemLabel.text = item.label
            complexItemTitle.text = item.title
            complexItemSubtitle.text = item.subtitle
        }
    }
}

class ExpandableCheckoutItemDelegate : AdapterDelegate<SimpleBuyCheckoutItem> {
    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ExpandableCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ExpandableCheckoutItemViewHolder(
            ItemCheckoutSimpleExpandableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ExpandableCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ExpandableCheckoutItem
    )
}

private class ExpandableCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleExpandableInfoBinding
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            expandableItemExpansion.movementMethod = LinkMovementMethod.getInstance()
            expandableItemLabel.setOnClickListener {
                isExpanded = !isExpanded
                expandableItemExpansion.visibleIf { isExpanded }
                if (isExpanded) {
                    expandableItemLabel.compoundDrawables[DRAWABLE_END_POSITION].setTint(
                        expandableItemTitle.context.getResolvedColor(R.color.blue_600)
                    )
                } else {
                    expandableItemLabel.compoundDrawables[DRAWABLE_END_POSITION].setTint(
                        expandableItemTitle.context.getResolvedColor(R.color.grey_300)
                    )
                }
            }
        }
    }

    fun bind(item: SimpleBuyCheckoutItem.ExpandableCheckoutItem) {
        with(binding) {
            expandableItemLabel.text = item.label
            expandableItemTitle.text = item.title
            expandableItemExpansion.text = item.expandableContent
            item.promoLayout?.let { view ->
                addPromoView(view)
            }
        }
    }

    private fun addPromoView(
        view: View
    ) {
        view.id = View.generateViewId()
        binding.root.addView(
            view, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)
        constraintSet.connect(
            view.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            view.resources.getDimensionPixelOffset(R.dimen.standard_margin)
        )
        constraintSet.connect(
            view.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            view.resources.getDimensionPixelOffset(R.dimen.standard_margin)
        )
        constraintSet.connect(
            view.id,
            ConstraintSet.TOP,
            binding.expandableItemTitle.id,
            ConstraintSet.BOTTOM,
            view.resources.getDimensionPixelOffset(R.dimen.tiny_margin)
        )
        constraintSet.connect(
            binding.expandableItemExpansion.id,
            ConstraintSet.TOP,
            view.id,
            ConstraintSet.BOTTOM,
            view.resources.getDimensionPixelOffset(R.dimen.smallest_margin)
        )
        constraintSet.applyTo(binding.root)
    }

    companion object {
        const val DRAWABLE_END_POSITION = 2
    }
}
