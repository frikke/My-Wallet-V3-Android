package com.blockchain.componentlib.carousel

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.ViewCarouselListBinding
import com.blockchain.componentlib.databinding.ViewCarouselValueBinding

class CarouselView : RecyclerView {

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

    @LayoutRes
    var layout = R.layout.view_carousel_value

    private val listAdapter = CarouselAdapter()
    private var carouselIndicatorView: CarouselIndicatorView? = null

    private fun initWithAttributes() {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        adapter = listAdapter
        val helper: SnapHelper = PagerSnapHelper()
        helper.attachToRecyclerView(this)
    }

    fun submitList(carouselItems: List<CarouselViewType>) {
        listAdapter.submitList(carouselItems)
        carouselIndicatorView?.numberOfIndicators = listAdapter.itemCount
    }

    fun setCarouselIndicator(carouselIndicator: CarouselIndicatorView) {
        carouselIndicatorView = carouselIndicator

        this.setOnScrollChangeListener { _, _, _, _, _ ->
            val currentItem =
                (layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()
            if (currentItem != NO_POSITION) {
                carouselIndicatorView?.selectedIndicator = currentItem ?: 0
            }
        }

        carouselIndicatorView?.numberOfIndicators = listAdapter.itemCount
    }
}

sealed class CarouselViewType {
    data class ValueProp(@DrawableRes val image: Int, val text: String) : CarouselViewType()
    data class PriceList(val text: String, val secondaryText: String) :
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        return when (viewType) {
            ViewType.ValueProp.ordinal -> CarouselViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_carousel_value, parent, false)
            )
            ViewType.List.ordinal -> CarouselViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_carousel_list, parent, false)
            )
            else -> throw java.lang.RuntimeException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CarouselViewType.PriceList -> {
                val itemBinding = ViewCarouselListBinding.bind(holder.itemView)
                itemBinding.title.text = item.text
                itemBinding.livePriceText.text = item.secondaryText
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

    enum class ViewType {
        ValueProp, List
    }
}