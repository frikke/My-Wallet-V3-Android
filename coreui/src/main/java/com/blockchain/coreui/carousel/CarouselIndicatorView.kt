package com.blockchain.coreui.carousel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coreui.R
import com.blockchain.coreui.databinding.ViewCarouselIndicatorBinding

class CarouselIndicatorView : RecyclerView {

    constructor(context: Context) : super(context) {
        initWithAttributes(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initWithAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initWithAttributes(attrs)
    }

    @LayoutRes
    var layout = R.layout.view_carousel_indicator
    private var numberOfRows = 4
    private var indicatorColor = ContextCompat.getColor(context, R.color.paletteBaseWhite)
    private lateinit var baseAdapter: CarouselIndicatorAdapter
    var selectedIndicator = 0
        set(value) {
            field = value
            baseAdapter.selectedIndicator = selectedIndicator
        }

    private fun initWithAttributes(attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.CarouselIndicatorView).apply {
            numberOfRows = getInteger(R.styleable.CarouselIndicatorView_numberOfIndicators, numberOfRows)
            indicatorColor = getColor(R.styleable.CarouselIndicatorView_indicatorColor, indicatorColor)
        }.recycle()

        setupUi()
    }

    private fun setupUi() {
        baseAdapter = CarouselIndicatorAdapter(numberOfRows)
        adapter = baseAdapter
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }
}

private class CarouselIndicatorAdapter(private val numberOfIndicators: Int) :
    RecyclerView.Adapter<CarouselIndicatorAdapter.ViewHolder>() {

    var selectedIndicator: Int = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(binding: ViewCarouselIndicatorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ViewCarouselIndicatorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.alpha = if (position == selectedIndicator) {
            0.4f
        } else {
            1f
        }
    }

    override fun getItemCount(): Int = numberOfIndicators
}