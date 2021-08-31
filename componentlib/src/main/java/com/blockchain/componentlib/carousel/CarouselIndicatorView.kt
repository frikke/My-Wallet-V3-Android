package com.blockchain.componentlib.carousel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.ViewCarouselIndicatorBinding

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

    private val baseAdapter = CarouselIndicatorAdapter()

    var selectedIndicator = 0
        set(value) {
            field = value
            baseAdapter.selectedIndicator = selectedIndicator
        }

    var numberOfIndicators = 4
        set(value) {
            field = value
            baseAdapter.numberOfIndicators = numberOfIndicators
        }

    @ColorRes
    var indicatorColor = R.color.paletteBaseWhite
        set(value) {
            field = value
            baseAdapter.numberOfIndicators = numberOfIndicators
        }

    private fun initWithAttributes(attrs: AttributeSet?) {
        setupUi()

        context.obtainStyledAttributes(attrs, R.styleable.CarouselIndicatorView).apply {
            numberOfIndicators = getInteger(R.styleable.CarouselIndicatorView_numberOfIndicators, numberOfIndicators)
            indicatorColor = getColor(R.styleable.CarouselIndicatorView_indicatorColor, indicatorColor)
        }.recycle()
    }

    private fun setupUi() {
        baseAdapter.numberOfIndicators = numberOfIndicators
        baseAdapter.indicatorColor = indicatorColor
        adapter = baseAdapter
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }
}

private class CarouselIndicatorAdapter : RecyclerView.Adapter<CarouselIndicatorAdapter.ViewHolder>() {

    var selectedIndicator: Int = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var numberOfIndicators: Int = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    @ColorRes
    var indicatorColor: Int? = null
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
        indicatorColor?.let {
            holder.itemView.background.setTint(ContextCompat.getColor(holder.itemView.context, it))
        }
        holder.itemView.alpha = if (position != selectedIndicator) {
            0.4f
        } else {
            1f
        }
    }

    override fun getItemCount(): Int = numberOfIndicators
}