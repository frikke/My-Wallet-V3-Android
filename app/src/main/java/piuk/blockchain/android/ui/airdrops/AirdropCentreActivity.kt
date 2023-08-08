package piuk.blockchain.android.ui.airdrops

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.setOnClickListenerDebounced
import com.blockchain.presentation.koin.scopedInject
import java.text.DateFormat
import kotlin.math.max
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAirdropsBinding
import piuk.blockchain.android.databinding.ItemAirdropHeaderBinding
import piuk.blockchain.android.databinding.ItemAirdropStatusBinding
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context

class AirdropCentreActivity :
    MvpActivity<AirdropCentreView, AirdropCentrePresenter>(),
    AirdropCentreView,
    SlidingModalBottomDialog.Host {

    override val presenter: AirdropCentrePresenter by scopedInject()
    override val view: AirdropCentreView = this

    private val assetResources: AssetResources by inject()
    private val binding: ActivityAirdropsBinding by lazy {
        ActivityAirdropsBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.airdrop_activity_title),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )
        binding.airdropList.layoutManager = LinearLayoutManager(this)
    }

    override fun renderList(statusList: List<Airdrop>) {
        val itemList: MutableList<ListItem> = statusList.sortedBy { !it.isActive }
            .map { ListItem.AirdropItem(it) }
            .toMutableList()

        val i = max(statusList.indexOfFirst { !it.isActive }, 0)
        itemList.add(i, ListItem.HeaderItem("Ended"))
        itemList.add(0, ListItem.HeaderItem("Active"))

        binding.airdropList.adapter = Adapter(itemList, assetResources) { airdropName -> onItemClicked(airdropName) }
    }

    private fun onItemClicked(airdropName: String) {
        showBottomSheet(AirdropStatusSheet.newInstance(airdropName))
    }

    override fun renderListUnavailable() {
        finish()
    }

    companion object {
        fun newIntent(context: Context?) =
            Intent(context, AirdropCentreActivity::class.java)
    }

    override fun onSheetClosed() {
        // no-op
    }
}

sealed class ListItem {
    data class AirdropItem(val airdrop: Airdrop) : ListItem()
    data class HeaderItem(val heading: String) : ListItem()
}

abstract class AirdropViewHolder<out T : ListItem>(itemView: View) : RecyclerView.ViewHolder(itemView)

class HeadingViewHolder(private val binding: ItemAirdropHeaderBinding) :
    AirdropViewHolder<ListItem.HeaderItem>(binding.root) {

    fun bind(item: ListItem.HeaderItem) {
        binding.heading.text = item.heading
    }
}

class StatusViewHolder(private val binding: ItemAirdropStatusBinding) :
    AirdropViewHolder<ListItem.AirdropItem>(binding.root) {

    fun bind(item: ListItem.AirdropItem, assetResources: AssetResources, onClick: (String) -> Unit) {
        with(binding) {
            assetResources.loadAssetIcon(icon, item.airdrop.asset)
            currency.text = item.airdrop.asset.displayTicker
            val formatted = DateFormat.getDateInstance(DateFormat.SHORT).format(item.airdrop.date)
            binding.root.setOnClickListenerDebounced { onClick(item.airdrop.name) }
            date.text = context.resources.getString(
                if (item.airdrop.isActive) {
                    com.blockchain.stringResources.R.string.airdrop_status_date_active
                } else {
                    com.blockchain.stringResources.R.string.airdrop_status_date_inactive
                },
                formatted
            )
        }
    }
}

private class Adapter(
    private val itemList: List<ListItem>,
    private val assetResources: AssetResources,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AirdropViewHolder<ListItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirdropViewHolder<ListItem> =
        when (viewType) {
            1 -> HeadingViewHolder(ItemAirdropHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            2 -> StatusViewHolder(ItemAirdropStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("View type out of range")
        }

    override fun getItemCount(): Int = itemList.size

    override fun getItemViewType(position: Int): Int =
        when (itemList[position]) {
            is ListItem.HeaderItem -> 1
            is ListItem.AirdropItem -> 2
        }

    override fun onBindViewHolder(holder: AirdropViewHolder<ListItem>, position: Int) {
        when (val o = itemList[position]) {
            is ListItem.HeaderItem -> (holder as HeadingViewHolder).bind(o)
            is ListItem.AirdropItem -> (holder as StatusViewHolder).bind(o, assetResources, onClick)
        }
    }
}
