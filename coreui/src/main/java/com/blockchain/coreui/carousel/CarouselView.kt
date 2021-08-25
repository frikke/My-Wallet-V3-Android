package com.blockchain.coreui.carousel

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
import com.blockchain.coreui.R
import com.blockchain.coreui.databinding.ViewCarouselListBinding
import com.blockchain.coreui.databinding.ViewCarouselValueBinding
import com.blockchain.coreui.price.PriceView

class CarouselView: RecyclerView {

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

    val listAdapter = CarouselAdapter()

    private fun initWithAttributes() {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        adapter = listAdapter
        val helper: SnapHelper = PagerSnapHelper()
        helper.attachToRecyclerView(this)
    }

}

sealed class CarouselViewType {
    data class ValuePropView(@DrawableRes val image: Int, val text: String): CarouselViewType()
    data class PriceListView(val text: String, val prices: List<PriceView.Price>): CarouselViewType()
}

class CarouselAdapter(
    diffCallback: DiffUtil.ItemCallback<CarouselViewType> = object :
        DiffUtil.ItemCallback<CarouselViewType>() {
        override fun areItemsTheSame(
            oldItem: CarouselViewType,
            newItem: CarouselViewType
        ): Boolean {
            if (oldItem is CarouselViewType.ValuePropView && newItem is CarouselViewType.ValuePropView) {
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
    },
): ListAdapter<CarouselViewType, CarouselAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ViewType.ValueProp.ordinal -> ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_carousel_value, parent, false)
            )
            ViewType.List.ordinal -> ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_carousel_list, parent, false)
            )
            else -> throw java.lang.RuntimeException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CarouselViewType.PriceListView -> {
                val itemBinding = ViewCarouselListBinding.bind(holder.itemView)
                itemBinding.valueText.text = item.text
                itemBinding.priceList.listAdapter.submitList(item.prices)
            }
            is CarouselViewType.ValuePropView -> {
                val itemBinding = ViewCarouselValueBinding.bind(holder.itemView)
                itemBinding.valueImage.setImageResource(item.image)
                itemBinding.valueText.text = item.text
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CarouselViewType.PriceListView -> ViewType.List.ordinal
            is CarouselViewType.ValuePropView -> ViewType.ValueProp.ordinal
        }
    }

    enum class ViewType {
        ValueProp, List
    }

}