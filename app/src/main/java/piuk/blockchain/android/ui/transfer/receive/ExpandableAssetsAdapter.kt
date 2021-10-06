package piuk.blockchain.android.ui.transfer.receive

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemAssetExpandableBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.visibleIf

data class ExpandableCryptoItem(
    val assetInfo: AssetInfo,
    val loadAccountsForAsset: (AssetInfo) -> Single<List<CryptoAccount>>,
    val onAccountClicked: (CryptoAccount) -> Unit,
    var isExpanded: Boolean = false
)

class ExpandableAssetsAdapter(
    private val assetResources: AssetResources,
    private val compositeDisposable: CompositeDisposable
) : RecyclerView.Adapter<ExpandableAssetViewHolder>() {

    private val uiScheduler = AndroidSchedulers.mainThread()

    var items: List<ExpandableCryptoItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpandableAssetViewHolder =
        ExpandableAssetViewHolder(
            assetResources,
            compositeDisposable,
            ItemAssetExpandableBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ExpandableAssetViewHolder, position: Int) {
        holder.bind(
            items[position],
            uiScheduler
        )
    }
}

class ExpandableAssetViewHolder(
    private val assetResources: AssetResources,
    private val compositeDisposable: CompositeDisposable,
    private val binding: ItemAssetExpandableBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val accountsAdapter = ExpandedAccountsAdapter(compositeDisposable, assetResources)

    fun bind(
        expandableItem: ExpandableCryptoItem,
        uiScheduler: Scheduler
    ) {
        with(binding) {
            val assetInfo = expandableItem.assetInfo
            assetResources.loadAssetIcon(icon, assetInfo)
            assetName.text = assetInfo.name
            assetSubtitle.text = assetInfo.displayTicker
            root.setOnClickListener {
                expandableItem.isExpanded = !expandableItem.isExpanded
                if (expandableItem.isExpanded) {
                    compositeDisposable.clear()
                    compositeDisposable += expandableItem.loadAccountsForAsset(assetInfo)
                        .observeOn(uiScheduler)
                        .subscribeBy { accounts ->
                            val items = accounts.map { cryptoAccount ->
                                ExpandedCryptoItem(
                                    account = cryptoAccount,
                                    onAccountClicked = expandableItem.onAccountClicked
                                )
                            }
                            accountsAdapter.items = items
                        }
                }
                updateExpandedState(expandableItem.isExpanded)
            }
            walletList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = accountsAdapter
                addItemDecoration(BlockchainListDividerDecor(context))
            }
        }
    }

    private fun ItemAssetExpandableBinding.updateExpandedState(isExpanded: Boolean) {
        if (isExpanded) {
            expandableChevron.setImageResource(R.drawable.expand_animated)
            expandableChevron.setColorFilter(context.getResolvedColor(R.color.blue_600))
        } else {
            expandableChevron.setImageResource(R.drawable.collapse_animated)
            expandableChevron.setColorFilter(context.getResolvedColor(R.color.grey_600))
        }
        val arrow = expandableChevron.drawable as Animatable
        arrow.start()
        walletList.visibleIf { isExpanded }
    }
}
