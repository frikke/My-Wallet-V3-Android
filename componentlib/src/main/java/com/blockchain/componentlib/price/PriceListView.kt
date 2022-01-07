package com.blockchain.componentlib.price

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
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

    private val listAdapter = PriceAdapter()

    private fun init() {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        adapter = listAdapter
    }

    fun setOnPriceRequest(onPriceRequest: ((PriceView.Price) -> Unit)?) {
        listAdapter.onPriceRequest = onPriceRequest
    }

    fun submitList(priceViews: List<PriceView.Price>) {
        (context as? Activity)?.runOnUiThread {
            listAdapter.submitList(priceViews)
        }
    }
}

private class PriceAdapter : RecyclerView.Adapter<PriceAdapter.PriceViewHolder>() {

    var onPriceRequest: ((PriceView.Price) -> Unit)? = null
    var prices: List<PriceView.Price> = emptyList()

    class PriceViewHolder(val priceView: PriceView) : RecyclerView.ViewHolder(priceView.rootView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceViewHolder {
        return PriceViewHolder(PriceView(parent.context))
    }

    override fun onBindViewHolder(holder: PriceViewHolder, position: Int) {
        val price = prices.get(position)
        holder.priceView.price = price
        onPriceRequest?.invoke(price)
    }

    override fun getItemCount(): Int {
        return prices.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(currentList: List<PriceView.Price>) {
        prices = currentList
        notifyDataSetChanged()
    }
}
