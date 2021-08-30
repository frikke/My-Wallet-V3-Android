package com.blockchain.componentlib.price

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.R

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

    @Deprecated("Remove logos when changing DummyPriceListView to use dynamic assets")
    private val dummyList = listOf(
        PriceView.Price(R.drawable.logo_aave, "Aave", "AAVE", "$11.00", 0.052),
        PriceView.Price(R.drawable.logo_algorand, "Algorand", "ALGO", "$4.23", -0.02),
        PriceView.Price(R.drawable.logo_bat, "Basic Attention Token", "BAT", "$0.52", 0.42),
        PriceView.Price(R.drawable.logo_bitclout, "BitClout", "CLOUT", "$4523.11", 0.2134),
        PriceView.Price(R.drawable.logo_bitcoin, "Bitcoin", "BTC", "$3.40", -0.0523),
        PriceView.Price(R.drawable.logo_blockstack, "Stacks", "STX", "$4.00", -0.4),
        PriceView.Price(
            R.drawable.logo_bch, "Bitcoin Cash", "BCH",
            "$42114.23", 0.21
        ),
        PriceView.Price(R.drawable.logo_chainlink, "Chainlink", "LINK", "$4540.21", 0.05),
        PriceView.Price(R.drawable.logo_compound, "Compound", "COMP", "$4540.21", 0.05)
    )

    private fun init() {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        adapter = listAdapter
        submitList(dummyList)
    }

    private fun submitList(priceViews: List<PriceView.Price>) {
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