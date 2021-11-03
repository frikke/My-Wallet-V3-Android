package com.blockchain.componentlib.carousel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.ViewCarouselBinding
import com.blockchain.componentlib.databinding.ViewCarouselListBinding
import com.blockchain.componentlib.databinding.ViewCarouselValueBinding
import com.blockchain.componentlib.price.PriceListView
import com.blockchain.componentlib.price.PriceView
import com.google.android.material.appbar.AppBarLayout

class CarouselView : ConstraintLayout {

    private val binding = ViewCarouselBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context) {
        initWithAttributes()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initWithAttributes()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initWithAttributes()
    }

    private val listAdapter = CarouselAdapter()
    private var carouselIndicatorView: CarouselIndicatorView? = null

    @SuppressLint("WrongConstant")
    private fun initWithAttributes() {
        binding.viewPager.apply {
            offscreenPageLimit = OFFSCREEN_LIMIT
            adapter = listAdapter
        }
    }

    fun submitList(carouselItems: List<CarouselViewType>) {
        listAdapter.submitList(carouselItems)
        carouselIndicatorView?.numberOfIndicators = listAdapter.currentList.size
    }

    fun setCarouselIndicator(carouselIndicator: CarouselIndicatorView) {
        carouselIndicatorView = carouselIndicator
        val currentList = listAdapter.currentList
        carouselIndicatorView?.numberOfIndicators = currentList.size

        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                carouselIndicatorView?.selectedIndicator = (position ?: 0) % currentList.size
                super.onPageSelected(position)
            }
        })
    }


    fun onLoadPrices(prices: List<PriceView.Price>) {
        listAdapter.priceList?.submitList(prices)
    }

    fun setOnPricesRequest(onPriceRequest: (PriceView.Price) -> Unit) {
        listAdapter.onPriceRequest = onPriceRequest
    }

    fun setOnPricesAlphaChangeListener(alphaChangeListener: (Float) -> Unit) {
        listAdapter.onAlphaChangeListener = alphaChangeListener
    }

    companion object {
        private const val OFFSCREEN_LIMIT = 3
    }

}

sealed class CarouselViewType {
    data class ValueProp(@DrawableRes val image: Int, val text: String) : CarouselViewType()
    data class PriceList(val text: String, val secondaryText: String, val prices: List<PriceView.Price> = emptyList()) :
        CarouselViewType()
}

private class CarouselAdapter(
    diffCallback: DiffUtil.ItemCallback<CarouselViewType> = object :
        DiffUtil.ItemCallback<CarouselViewType>() {
        override fun areItemsTheSame(
            oldItem: CarouselViewType,
            newItem: CarouselViewType
        ): Boolean {
            if (oldItem is CarouselViewType.ValueProp && newItem is CarouselViewType.ValueProp) {
                return oldItem.text == newItem.text
            }

            return oldItem == newItem
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: CarouselViewType,
            newItem: CarouselViewType
        ): Boolean {
            return oldItem == newItem
        }
    }
) : ListAdapter<CarouselViewType, CarouselAdapter.CarouselViewHolder>(diffCallback) {

    class CarouselViewHolder(view: View) : RecyclerView.ViewHolder(view)

    var onPriceRequest: ((PriceView.Price) -> Unit) = {}
    var onAlphaChangeListener: ((Float) -> Unit) = {}

    var priceList: PriceListView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        return when (viewType) {
            ViewType.ValueProp.ordinal -> CarouselViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_carousel_value, parent, false)
            )
            ViewType.List.ordinal -> {
                val carousel = CarouselViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.view_carousel_list, parent, false)
                )
                val itemBinding = ViewCarouselListBinding.bind(carousel.itemView)
                priceList = itemBinding.priceList

                itemBinding.appbarLayout.addOnOffsetChangedListener(
                    AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                        val alpha = (itemBinding.headerContainer.height - itemBinding.headerContainer.minHeight + verticalOffset) / 100f
                        itemBinding.title.alpha = alpha
                        onAlphaChangeListener.invoke(alpha)
                    }
                )
                carousel
            }
            else -> throw java.lang.RuntimeException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CarouselViewType.PriceList -> {
                val itemBinding = ViewCarouselListBinding.bind(holder.itemView)
                itemBinding.title.text = item.text
                itemBinding.livePriceText.text = item.secondaryText
                itemBinding.priceList.setOnPriceRequest(onPriceRequest)
            }
            is CarouselViewType.ValueProp -> {
                val itemBinding = ViewCarouselValueBinding.bind(holder.itemView)
                itemBinding.valueImage.setImageResource(item.image)
                itemBinding.valueText.text = item.text
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CarouselViewType.PriceList -> ViewType.List.ordinal
            is CarouselViewType.ValueProp -> ViewType.ValueProp.ordinal
        }
    }

    override fun getItemCount(): Int {
        return Integer.MAX_VALUE
    }

    override fun getItem(position: Int): CarouselViewType {
        val positionInList = position % currentList.size
        return super.getItem(positionInList)
    }

    enum class ViewType {
        ValueProp, List
    }
}