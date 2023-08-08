package piuk.blockchain.android.simplebuy

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.updateItemBackground
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.getResolvedColor
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemCheckoutClickableInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutComplexInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutCtaInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutSimpleExpandableInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutSimpleInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutToggleInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.animateColor

class CheckoutAdapterDelegate(
    onToggleChanged: (Boolean) -> Unit,
    onAction: (ActionType) -> Unit
) :
    DelegationAdapter<SimpleBuyCheckoutItem>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<SimpleBuyCheckoutItem> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(SimpleBuyCheckoutItemDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        with(delegatesManager) {
            addAdapterDelegate(SimpleCheckoutItemDelegate())
            addAdapterDelegate(ComplexCheckoutItemDelegate())
            addAdapterDelegate(ExpandableCheckoutItemDelegate(onAction))
            addAdapterDelegate(ToggleCheckoutItemDelegate(onToggleChanged))
            addAdapterDelegate(ClickableCheckoutItemDelegate(onAction))
            addAdapterDelegate(ReadMoreDisclaimerCheckoutItemDelegate(onAction))
        }
    }
}

class SimpleBuyCheckoutItemDiffUtil(
    private val oldItems: List<SimpleBuyCheckoutItem>,
    private val newItems: List<SimpleBuyCheckoutItem>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldListSize == newListSize

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}

sealed class ActionType {
    object Price : ActionType()
    object Fee : ActionType()
    object WithdrawalHold : ActionType()
    data class TermsAndConditions(
        val bankLabel: String,
        val amount: String,
        val withdrawalLock: String,
        val isRecurringBuyEnabled: Boolean
    ) : ActionType()

    object Unknown : ActionType()
}

sealed class SimpleBuyCheckoutItem {
    data class SimpleCheckoutItem(
        val label: String,
        val title: String,
        val isImportant: Boolean = false,
        val hasChanged: Boolean
    ) :
        SimpleBuyCheckoutItem()

    data class ComplexCheckoutItem(val label: String, val title: String, val subtitle: String) :
        SimpleBuyCheckoutItem()

    data class ToggleCheckoutItem(
        val title: String,
        val subtitle: String
    ) : SimpleBuyCheckoutItem()

    data class ClickableCheckoutItem(
        val label: String,
        val title: String,
        val actionType: ActionType
    ) : SimpleBuyCheckoutItem()

    data class ReadMoreCheckoutItem(
        val text: String,
        val cta: String,
        val actionType: ActionType
    ) : SimpleBuyCheckoutItem()

    data class ExpandableCheckoutItem(
        val label: String,
        val title: String,
        val expandableContent: CharSequence,
        val promoLayout: View? = null,
        val hasChanged: Boolean,
        val actionType: ActionType
    ) : SimpleBuyCheckoutItem() {
        override fun equals(other: Any?) =
            (other as? ExpandableCheckoutItem)?.let { EssentialData(this) == EssentialData(it) } ?: false

        override fun hashCode() = EssentialData(this).hashCode()

        override fun toString() = EssentialData(this).toString().replaceFirst(
            "EssentialData",
            "ExpandableCheckoutItem"
        )

        private data class EssentialData constructor(
            private val label: String,
            private val title: String,
            private val hasChanged: Boolean
        ) {
            constructor(item: ExpandableCheckoutItem) : this(
                label = item.label,
                title = item.title,
                hasChanged = item.hasChanged
            )
        }
    }
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
        items[position] as SimpleBuyCheckoutItem.SimpleCheckoutItem,
        isFirstItemInList = position == 0,
        isLastItemInList = items.itemsLastIndex() == position
    )
}

private fun List<SimpleBuyCheckoutItem>.itemsLastIndex() = filter {
    it !is SimpleBuyCheckoutItem.ReadMoreCheckoutItem
}.lastIndex

private class SimpleCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.SimpleCheckoutItem, isFirstItemInList: Boolean, isLastItemInList: Boolean) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            simpleItemTitle.text = item.title
            simpleItemLabel.text = item.label

            if (item.isImportant) {
                simpleItemLabel.setTextAppearance(com.blockchain.common.R.style.Text_Semibold_16)
                simpleItemTitle.setTextAppearance(com.blockchain.common.R.style.Text_Semibold_16)
            } else {
                simpleItemLabel.setTextAppearance(com.blockchain.common.R.style.Text_Standard_14)
                simpleItemTitle.setTextAppearance(com.blockchain.common.R.style.Text_Standard_14)
            }

            if (item.hasChanged) {
                simpleItemTitle.animateColor {
                    simpleItemTitle.setTextColor(
                        ContextCompat.getColor(simpleItemTitle.context, com.blockchain.componentlib.R.color.body)
                    )
                }
            } else {
                simpleItemTitle.setTextColor(
                    ContextCompat.getColor(simpleItemTitle.context, com.blockchain.componentlib.R.color.body)
                )
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
        items[position] as SimpleBuyCheckoutItem.ComplexCheckoutItem,
        isFirstItemInList = position == 0,
        isLastItemInList = items.itemsLastIndex() == position
    )
}

private class ComplexCheckoutItemItemViewHolder(
    val binding: ItemCheckoutComplexInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.ComplexCheckoutItem, isFirstItemInList: Boolean, isLastItemInList: Boolean) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            complexItemLabel.text = item.label
            complexItemTitle.text = item.title
            complexItemSubtitle.text = item.subtitle
        }
    }
}

class ToggleCheckoutItemDelegate(private val onToggleChanged: (Boolean) -> Unit) :
    AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ToggleCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ToggleCheckoutItemItemViewHolder(
            ItemCheckoutToggleInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ToggleCheckoutItemItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ToggleCheckoutItem,
        onToggleChanged
    )
}

private class ToggleCheckoutItemItemViewHolder(
    val binding: ItemCheckoutToggleInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SimpleBuyCheckoutItem.ToggleCheckoutItem,
        onToggleChanged: (Boolean) -> Unit
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList = false, isLastItemInList = true)
            toggleRow.apply {
                transparentBackground = true
                primaryText = item.title
                secondaryText = item.subtitle
                onCheckedChange = { newCheckedState ->
                    isChecked = newCheckedState
                    onToggleChanged(newCheckedState)
                }
            }
        }
    }
}

class ClickableCheckoutItemDelegate(
    private val onTooltipClicked: (ActionType) -> Unit
) : AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ClickableCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ClickableCheckoutItemViewHolder(
            ItemCheckoutClickableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ClickableCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ClickableCheckoutItem,
        onTooltipClicked,
        isFirstItemInList = position == 0,
        isLastItemInList = items.itemsLastIndex() == position
    )
}

private class ClickableCheckoutItemViewHolder(
    val binding: ItemCheckoutClickableInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SimpleBuyCheckoutItem.ClickableCheckoutItem,
        onTooltipClicked: (ActionType) -> Unit,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            clickableItemTitle.text = item.title
            clickableItemLabel.text = item.label
            clickableItemLabel.setOnClickListener {
                onTooltipClicked(item.actionType)
            }
        }
    }
}

class ReadMoreDisclaimerCheckoutItemDelegate(
    private val onCtaClicked: (ActionType) -> Unit
) : AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ReadMoreCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ReadMoreDisclaimerCheckoutItemViewHolder(
            ItemCheckoutCtaInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ReadMoreDisclaimerCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ReadMoreCheckoutItem,
        onCtaClicked,
        isFirstItemInList = position == 0,
        isLastItemInList = items.itemsLastIndex() == position
    )
}

private class ReadMoreDisclaimerCheckoutItemViewHolder(
    val binding: ItemCheckoutCtaInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SimpleBuyCheckoutItem.ReadMoreCheckoutItem,
        onCtaClicked: (ActionType) -> Unit,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            infoText.text = item.text
            ctaButton.apply {
                text = item.cta
                onClick = { onCtaClicked(item.actionType) }
            }
        }
    }
}

class ExpandableCheckoutItemDelegate(
    private val onTooltipClicked: (ActionType) -> Unit
) : AdapterDelegate<SimpleBuyCheckoutItem> {
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
        items[position] as SimpleBuyCheckoutItem.ExpandableCheckoutItem,
        onTooltipClicked,
        isFirstItemInList = position == 0,
        isLastItemInList = items.itemsLastIndex() == position
    )
}

private class ExpandableCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleExpandableInfoBinding
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            expandableItemExpansion.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    fun bind(
        item: SimpleBuyCheckoutItem.ExpandableCheckoutItem,
        onTooltipClicked: (ActionType) -> Unit,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            expandableItemLabel.text = item.label
            expandableItemTitle.text = item.title
            expandableItemExpansion.text = item.expandableContent
            item.promoLayout?.let { view ->
                addPromoView(view)
            }
            if (item.hasChanged) {
                expandableItemTitle.animateColor {
                    expandableItemTitle.setTextColor(
                        ContextCompat.getColor(expandableItemTitle.context, com.blockchain.componentlib.R.color.body)
                    )
                }
            } else {
                expandableItemTitle.setTextColor(
                    ContextCompat.getColor(expandableItemTitle.context, com.blockchain.componentlib.R.color.body)
                )
            }
            expandableItemLabel.setOnClickListener {
                onTooltipClicked(item.actionType)
                isExpanded = !isExpanded
                expandableItemExpansion.visibleIf { isExpanded }
                if (isExpanded) {
                    expandableItemLabel.compoundDrawables[DRAWABLE_END_POSITION].setTint(
                        expandableItemTitle.context.getResolvedColor(com.blockchain.componentlib.R.color.primary)
                    )
                } else {
                    expandableItemLabel.compoundDrawables[DRAWABLE_END_POSITION].setTint(
                        expandableItemTitle.context.getResolvedColor(com.blockchain.componentlib.R.color.medium)
                    )
                }
            }
        }
    }

    private fun addPromoView(
        view: View
    ) {
        view.id = View.generateViewId()
        binding.root.addView(
            view,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)
        constraintSet.connect(
            view.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            view.resources.getDimensionPixelOffset(com.blockchain.componentlib.R.dimen.standard_spacing)
        )
        constraintSet.connect(
            view.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            view.resources.getDimensionPixelOffset(com.blockchain.componentlib.R.dimen.standard_spacing)
        )
        constraintSet.connect(
            view.id,
            ConstraintSet.TOP,
            binding.expandableItemTitle.id,
            ConstraintSet.BOTTOM,
            view.resources.getDimensionPixelOffset(com.blockchain.componentlib.R.dimen.tiny_spacing)
        )
        constraintSet.connect(
            binding.expandableItemExpansion.id,
            ConstraintSet.TOP,
            view.id,
            ConstraintSet.BOTTOM,
            view.resources.getDimensionPixelOffset(com.blockchain.componentlib.R.dimen.smallest_spacing)
        )
        constraintSet.applyTo(binding.root)
    }

    companion object {
        const val DRAWABLE_END_POSITION = 2
    }
}
