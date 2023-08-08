package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TrendingPair
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.presentation.customviews.BlockchainListDividerDecor
import com.blockchain.presentation.getResolvedDrawable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemTrendingPairRowBinding
import piuk.blockchain.android.databinding.ViewTrendingPairsBinding
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context

class TrendingPairsView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    private val binding: ViewTrendingPairsBinding =
        ViewTrendingPairsBinding.inflate(LayoutInflater.from(context), this, true)
    private var viewType: TrendingType = TrendingType.OTHER

    init {
        setupView(context, attrs)
        binding.trendingList.addItemDecoration(
            BlockchainListDividerDecor(context)
        )
    }

    fun initialise(
        pairs: List<TrendingPair>,
        onSwapPairClicked: (TrendingPair) -> Unit,
        assetResources: AssetResources
    ) {
        setupPairs(pairs, onSwapPairClicked, assetResources)
    }

    private fun setupView(context: Context, attrs: AttributeSet) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.TrendingPairsView, 0, 0)

        viewType =
            TrendingType.fromInt(attributes.getInt(R.styleable.TrendingPairsView_trending_type, 1))

        attributes.recycle()
    }

    private fun setupPairs(
        pairs: List<TrendingPair>,
        onSwapPairClicked: (TrendingPair) -> Unit,
        assetResources: AssetResources
    ) {
        with(binding) {
            if (pairs.isEmpty()) {
                trendingEmpty.visible()
                trendingList.gone()
            } else {
                trendingEmpty.gone()
                trendingList.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                    adapter = TrendingPairsAdapter(
                        type = viewType,
                        itemClicked = {
                            onSwapPairClicked(it)
                        },
                        items = pairs,
                        assetResources = assetResources
                    )
                    visible()
                }
            }
        }
    }

    enum class TrendingType {
        SWAP,
        OTHER;

        companion object {
            fun fromInt(value: Int) = values()[value]
        }
    }
}

private class TrendingPairsAdapter(
    val type: TrendingPairsView.TrendingType,
    val itemClicked: (TrendingPair) -> Unit,
    private val items: List<TrendingPair>,
    private val assetResources: AssetResources
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val compositeDisposable = CompositeDisposable()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        TrendingPairViewHolder(
            ItemTrendingPairRowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            itemClicked,
            compositeDisposable
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as TrendingPairViewHolder).bind(type, items[position], assetResources)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        compositeDisposable.clear()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = items.size

    private class TrendingPairViewHolder(
        private val binding: ItemTrendingPairRowBinding,
        private val itemClicked: (TrendingPair) -> Unit,
        private val compositeDisposable: CompositeDisposable
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(type: TrendingPairsView.TrendingType, item: TrendingPair, assetResources: AssetResources) {
            binding.apply {
                assetResources.loadAssetIcon(trendingIconIn, item.sourceAccount.currency)
                assetResources.loadAssetIcon(trendingIconOut, item.destinationAccount.currency)
                trendingRoot.apply {
                    contentDescription =
                        "$TRENDING_PAIR_SWAP_VIEW_ID${item.sourceAccount.currency.networkTicker}" +
                        "$FOR_CONTENT_DESCRIPTION${item.destinationAccount.currency.networkTicker}"
                    compositeDisposable += item.enabled.doOnSubscribe {
                        setOnClickListener(null)
                        alpha = 0.6f
                    }.subscribeBy(onSuccess = { enabled ->
                        alpha = if (enabled) {
                            setOnClickListener { itemClicked(item) }
                            1f
                        } else {
                            setOnClickListener(null)
                            0.6f
                        }
                    }, onError = {
                            setOnClickListener(null)
                            alpha = 0.6f
                        })
                }

                when (type) {
                    TrendingPairsView.TrendingType.SWAP -> {
                        trendingTitle.text = context.getString(
                            com.blockchain.stringResources.R.string.trending_swap,
                            item.sourceAccount.currency.name
                        )
                        trendingSubtitle.text = context.getString(
                            com.blockchain.stringResources.R.string.common_receive_to,
                            item.destinationAccount.currency.name
                        )
                        trendingIconType.setImageDrawable(context.getResolvedDrawable(R.drawable.ic_swap_light_blue))
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }

        companion object {
            private const val TRENDING_PAIR_SWAP_VIEW_ID = "TrendingPairViewSwap_"
            private const val FOR_CONTENT_DESCRIPTION = "_FOR_"
        }
    }
}
