package com.blockchain.componentlib.price

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class DummyPriceListView : RecyclerView {
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

    private val listAdapter = PriceAdapter()

    private fun init() {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        adapter = listAdapter
    }

    fun submitList(priceViews: List<PriceView.Price>) {
        listAdapter.submitList(priceViews)
    }
}

private class PriceAdapter(
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
) : ListAdapter<PriceView.Price, PriceAdapter.PriceViewHolder>(diffCallback) {

    class PriceViewHolder(val priceView: PriceView) : RecyclerView.ViewHolder(priceView.rootView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceViewHolder {
        return PriceViewHolder(PriceView(parent.context))
    }

    override fun onBindViewHolder(holder: PriceViewHolder, position: Int) {
        holder.priceView.price = getItem(position)
    }
}