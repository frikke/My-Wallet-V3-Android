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
import com.blockchain.componentlib.viewextensions.visibleIf
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
import piuk.blockchain.android.util.getResolvedColor

class CheckoutAdapterDelegate(
    onToggleChanged: (Boolean) -> Unit,
    onAction: (ActionType) -> Unit,
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
    private val newItems: List<SimpleBuyCheckoutItem>,
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true

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
        val hasChanged: Boolean,
    ) :
        SimpleBuyCheckoutItem()

    data class ComplexCheckoutItem(val label: String, val title: String, val subtitle: String) :
        SimpleBuyCheckoutItem()

    data class ToggleCheckoutItem(val title: String, val subtitle: String) : SimpleBuyCheckoutItem()

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
            "EssentialData", "ExpandableCheckoutItem"
        )

        private data class EssentialData constructor(
            private val label: String,
            private val title: String,
            private val hasChanged: Boolean,
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
        holder: RecyclerView.ViewHolder,
    ) = (holder as SimpleCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.SimpleCheckoutItem
    )
}

private class SimpleCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleInfoBinding,
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

            if (item.hasChanged) {
                simpleItemTitle.animateColor {
                    simpleItemTitle.setTextColor(
                        ContextCompat.getColor(simpleItemTitle.context, R.color.grey_800)
                    )
                }
            } else {
                simpleItemTitle.setTextColor(
                    ContextCompat.getColor(simpleItemTitle.context, R.color.grey_800)
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
        holder: RecyclerView.ViewHolder,
    ) = (holder as ComplexCheckoutItemItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ComplexCheckoutItem
    )
}

private class ComplexCheckoutItemItemViewHolder(
    val binding: ItemCheckoutComplexInfoBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.ComplexCheckoutItem) {
        with(binding) {
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
        holder: RecyclerView.ViewHolder,
    ) = (holder as ToggleCheckoutItemItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ToggleCheckoutItem,
        onToggleChanged
    )
}

private class ToggleCheckoutItemItemViewHolder(
    val binding: ItemCheckoutToggleInfoBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SimpleBuyCheckoutItem.ToggleCheckoutItem,
        onToggleChanged: (Boolean) -> Unit
    ) {
        with(binding) {
            toggleRow.apply {
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
        holder: RecyclerView.ViewHolder,
    ) = (holder as ClickableCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ClickableCheckoutItem,
        onTooltipClicked
    )
}

private class ClickableCheckoutItemViewHolder(
    val binding: ItemCheckoutClickableInfoBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SimpleBuyCheckoutItem.ClickableCheckoutItem,
        onTooltipClicked: (ActionType) -> Unit
    ) {
        with(binding) {
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
        holder: RecyclerView.ViewHolder,
    ) = (holder as ReadMoreDisclaimerCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ReadMoreCheckoutItem,
        onCtaClicked
    )
}

private class ReadMoreDisclaimerCheckoutItemViewHolder(
    val binding: ItemCheckoutCtaInfoBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SimpleBuyCheckoutItem.ReadMoreCheckoutItem,
        onCtaClicked: (ActionType) -> Unit
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
    private val onTooltipClicked: (ActionType) -> Unit,
) : AdapterDelegate<SimpleBuyCheckoutItem> {
    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ExpandableCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ExpandableCheckoutItemViewHolder(
            ItemCheckoutSimpleExpandableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
    ) = (holder as ExpandableCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ExpandableCheckoutItem,
        onTooltipClicked
    )
}

private class ExpandableCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleExpandableInfoBinding,
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
    ) {
        with(binding) {
            expandableItemLabel.text = item.label
            expandableItemTitle.text = item.title
            expandableItemExpansion.text = item.expandableContent
            item.promoLayout?.let { view ->
                addPromoView(view)
            }
            if (item.hasChanged) {
                expandableItemTitle.animateColor {
                    expandableItemTitle.setTextColor(
                        ContextCompat.getColor(expandableItemTitle.context, R.color.grey_800)
                    )
                }
            } else {
                expandableItemTitle.setTextColor(
                    ContextCompat.getColor(expandableItemTitle.context, R.color.grey_800)
                )
            }
            expandableItemLabel.setOnClickListener {
                onTooltipClicked(item.actionType)
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

    private fun addPromoView(
        view: View,
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
            view.resources.getDimensionPixelOffset(R.dimen.standard_spacing)
        )
        constraintSet.connect(
            view.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            view.resources.getDimensionPixelOffset(R.dimen.standard_spacing)
        )
        constraintSet.connect(
            view.id,
            ConstraintSet.TOP,
            binding.expandableItemTitle.id,
            ConstraintSet.BOTTOM,
            view.resources.getDimensionPixelOffset(R.dimen.tiny_spacing)
        )
        constraintSet.connect(
            binding.expandableItemExpansion.id,
            ConstraintSet.TOP,
            view.id,
            ConstraintSet.BOTTOM,
            view.resources.getDimensionPixelOffset(R.dimen.smallest_spacing)
        )
        constraintSet.applyTo(binding.root)
    }

    companion object {
        const val DRAWABLE_END_POSITION = 2
    }
}
