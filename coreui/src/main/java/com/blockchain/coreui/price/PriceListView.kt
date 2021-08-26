package com.blockchain.coreui.price

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PriceListView : RecyclerView {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    val listAdapter = PriceAdapter()

    private fun init() {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        adapter = listAdapter
    }
}

class PriceAdapter(
    diffCallback: DiffUtil.ItemCallback<PriceView.Price> = object :
        DiffUtil.ItemCallback<PriceView.Price>() {
        override fun areItemsTheSame(
            oldItem: PriceView.Price,
            newItem: PriceView.Price
        ): Boolean {
            return oldItem.ticker == newItem.ticker
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: PriceView.Price,
            newItem: PriceView.Price
        ): Boolean {
            return oldItem == newItem
        }
    }
) : ListAdapter<PriceView.Price, PriceAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(val priceView: PriceView) : RecyclerView.ViewHolder(priceView.rootView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PriceView(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.priceView.price = getItem(position)
    }
}