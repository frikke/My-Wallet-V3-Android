package piuk.blockchain.android.ui.transfer.receive

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.CryptoAccount
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.customviews.BlockchainListDividerDecor
import com.blockchain.presentation.getResolvedColor
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemAssetExpandableBinding
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context
import timber.log.Timber

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
            ExpandedAccountsAdapter(compositeDisposable),
            ItemAssetExpandableBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            LinearLayoutManager(parent.context)
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
    private val accountsAdapter: ExpandedAccountsAdapter,
    private val binding: ItemAssetExpandableBinding,
    private val walletLayoutManager: LinearLayoutManager
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        expandableItem: ExpandableCryptoItem,
        uiScheduler: Scheduler
    ) {
        with(binding) {
            val assetInfo = expandableItem.assetInfo
            assetResources.loadAssetIcon(icon, assetInfo)
            assetName.text = assetInfo.name
            assetSubtitle.text = assetInfo.displayTicker
            updateExpandedState(expandableItem, assetInfo, uiScheduler)
            root.setOnClickListener {
                expandableItem.isExpanded = !expandableItem.isExpanded
                updateExpandedState(expandableItem, assetInfo, uiScheduler)
            }
            walletList.apply {
                layoutManager = walletLayoutManager
                adapter = accountsAdapter
                if (itemDecorationCount <= 1) {
                    addItemDecoration(BlockchainListDividerDecor(context))
                }
            }
        }
    }

    private fun updateExpandedState(
        expandableItem: ExpandableCryptoItem,
        assetInfo: AssetInfo,
        uiScheduler: Scheduler
    ) {
        if (expandableItem.isExpanded) {
            accountsAdapter.items = listOf(ExpandedCryptoItem.Loading)
            compositeDisposable += expandableItem.loadAccountsForAsset(assetInfo)
                .observeOn(uiScheduler)
                .subscribeBy(
                    onSuccess = { accounts ->
                        val items = accounts.map { cryptoAccount ->
                            ExpandedCryptoItem.Loaded(
                                account = cryptoAccount,
                                onAccountClicked = expandableItem.onAccountClicked
                            )
                        }
                        accountsAdapter.items = items
                    },
                    onError = {
                        Timber.e(it)
                        accountsAdapter.items = listOf()
                        expandableItem.isExpanded = false
                        binding.updateUI(expandableItem.isExpanded)
                    }
                )
        }
        binding.updateUI(expandableItem.isExpanded)
    }

    private fun ItemAssetExpandableBinding.updateUI(isExpanded: Boolean) {
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
