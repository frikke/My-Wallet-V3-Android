package piuk.blockchain.android.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SettingsPaymentMethodItemLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class SettingsPaymentMethodsAdapter(onPaymentMethodClicked: (SettingsPaymentMethod) -> Unit) :
    DelegationAdapter<SettingsPaymentMethod>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<SettingsPaymentMethod> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(SettingsPaymentMethodDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        with(delegatesManager) {
            addAdapterDelegate(CardSettingsPaymentMethodDelegate(onPaymentMethodClicked))
            addAdapterDelegate(BankSettingsPaymentMethodDelegate(onPaymentMethodClicked))
        }
    }
}

class CardSettingsPaymentMethodDelegate(private val onPaymentMethodClicked: (SettingsPaymentMethod) -> Unit) :
    AdapterDelegate<SettingsPaymentMethod> {
    override fun isForViewType(items: List<SettingsPaymentMethod>, position: Int): Boolean {
        return items[position] is CardSettingsPaymentMethod
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return CardSettingsViewHolder(
            SettingsPaymentMethodItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onPaymentMethodClicked
        )
    }

    override fun onBindViewHolder(items: List<SettingsPaymentMethod>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as CardSettingsViewHolder).bind(items[position] as CardSettingsPaymentMethod)
    }
}

private class CardSettingsViewHolder(
    private val binding: SettingsPaymentMethodItemLayoutBinding,
    private val onPaymentMethodClicked: (SettingsPaymentMethod) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(card: CardSettingsPaymentMethod) {
        binding.paymentMethod.apply {
            titleStart = buildAnnotatedString { append(card.title) }
            titleEnd = buildAnnotatedString { append(card.titleEnd) }
            startImageResource = card.icon
            bodyStart = buildAnnotatedString {
                append(
                    card.subtitle
                )
            }
            bodyEnd = buildAnnotatedString {
                append(
                    card.bodyEnd
                )
            }
            onClick = { onPaymentMethodClicked(card) }
            tags = when (card.cardRejectionState) {
                is CardRejectionState.AlwaysRejected -> {
                    listOf(
                        TagViewState(
                            card.cardRejectionState.error?.title
                                ?: binding.root.context.getString(
                                    com.blockchain.stringResources.R.string.card_issuer_always_rejects_title
                                ),
                            TagType.Error()
                        )
                    )
                }

                is CardRejectionState.MaybeRejected -> {
                    listOf(
                        TagViewState(
                            card.cardRejectionState.error.title,
                            TagType.Warning()
                        )
                    )
                }

                else -> null
            }
        }
    }
}

private class BankSettingsViewHolder(
    private val binding: SettingsPaymentMethodItemLayoutBinding,
    private val onPaymentMethodClicked: (SettingsPaymentMethod) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(bank: BankSettingsPaymentMethod) {
        binding.paymentMethod.apply {
            titleStart = buildAnnotatedString { append(bank.title) }
            titleEnd = buildAnnotatedString { append(bank.titleEnd) }
            startImageResource = bank.icon
            bodyStart = buildAnnotatedString {
                append(
                    bank.subtitle
                )
            }
            bodyEnd = buildAnnotatedString {
                append(
                    bank.bodyEnd.orEmpty()
                )
            }
            tags = if (!bank.canBeUsedToTransact) {
                listOf(
                    TagViewState(
                        binding.root.context.getString(com.blockchain.stringResources.R.string.common_unavailable),
                        TagType.Error()
                    )
                )
            } else {
                emptyList()
            }
            onClick = { onPaymentMethodClicked(bank) }
        }
    }
}

class BankSettingsPaymentMethodDelegate(private val onPaymentMethodClicked: (SettingsPaymentMethod) -> Unit) :
    AdapterDelegate<SettingsPaymentMethod> {
    override fun isForViewType(items: List<SettingsPaymentMethod>, position: Int): Boolean {
        return items[position] is BankSettingsPaymentMethod
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return BankSettingsViewHolder(
            SettingsPaymentMethodItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onPaymentMethodClicked
        )
    }

    override fun onBindViewHolder(items: List<SettingsPaymentMethod>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as BankSettingsViewHolder).bind(items[position] as BankSettingsPaymentMethod)
    }
}

interface SettingsPaymentMethod {
    val id: String
    val title: String
    val subtitle: String
    val titleEnd: String
    val canBeUsedToTransact: Boolean
    val icon: ImageResource
}

data class CardSettingsPaymentMethod(
    override val id: String,
    override val title: String,
    override val subtitle: String,
    override val titleEnd: String,
    private val iconRes: Int,
    val cardRejectionState: CardRejectionState?,
    val bodyEnd: String
) : SettingsPaymentMethod {
    override val canBeUsedToTransact: Boolean
        get() = true
    override val icon: ImageResource
        get() = ImageResource.Local(iconRes)
}

data class BankSettingsPaymentMethod(
    override val id: String,
    override val title: String,
    override val subtitle: String,
    override val canBeUsedToTransact: Boolean,
    override val titleEnd: String,
    val bodyEnd: String?,
    private val iconUrl: String?
) : SettingsPaymentMethod {
    override val icon: ImageResource
        get() = iconUrl?.let {
            ImageResource.Remote(it)
        } ?: ImageResource.Local(R.drawable.ic_bank_icon)
}

class SettingsPaymentMethodDiffUtil(
    private val oldItems: List<SettingsPaymentMethod>,
    private val newItems: List<SettingsPaymentMethod>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldItems[oldItemPosition].id == newItems[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
