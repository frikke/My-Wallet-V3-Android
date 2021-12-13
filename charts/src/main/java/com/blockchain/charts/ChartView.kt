package com.blockchain.charts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import kotlin.math.abs

class ChartView : FrameLayout {

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize()
    }

    private val lineChart: LineChart = LineChart(context)

    var datePattern: String = "HH:mm aa"
        set(value) {
            field = value
            lineChart.setDrawMarkers(true)
            lineChart.marker = ScrubMarker(context, datePattern)
        }

    var isChartLive: Boolean = true
        set(value) {
            field = value
            setLive(isChartLive)
        }

    var onEntryHighlighted: ((Entry) -> Unit)? = null

    private var isBlockingScroll = false
    private var currentEvent: MotionEvent? = null

    private var selectedEntry: Entry? = null
    private var entries: List<Entry> = emptyList()

    private var scrollHandler: Handler? = null
    private var mLongPressed = Runnable {
        isBlockingScroll = true
        parent.requestDisallowInterceptTouchEvent(isBlockingScroll)
        setSelectedEntry(currentEvent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialize() {
        lineChart.setOnTouchListener { view, event ->
            currentEvent = event

            if (event?.action == ACTION_DOWN) {
                scrollHandler?.removeCallbacks(mLongPressed)
                scrollHandler = Handler(Looper.getMainLooper()).apply {
                    postDelayed(mLongPressed, LONG_PRESS_DURATION)
                }
            }

            if (event?.action == ACTION_UP || event?.action == ACTION_CANCEL) {
                scrollHandler?.removeCallbacks(mLongPressed)
                isBlockingScroll = false
                selectedEntry = null
                setEntryData(entries)
            }

            if (event?.action == ACTION_MOVE && isBlockingScroll) {
                setSelectedEntry(event)
            }

            parent.requestDisallowInterceptTouchEvent(isBlockingScroll)
            true
        }
        addView(lineChart)
        setupUi()
        setLive(isChartLive)
    }

    private fun setSelectedEntry(motionEvent: MotionEvent?) {
        motionEvent?.let { me ->
            val point = lineChart.getValuesByTouchPoint(me.x, me.y, YAxis.AxisDependency.LEFT)
            selectedEntry = entries.minByOrNull { abs(it.x - point.x) }
            selectedEntry?.let {
                onEntryHighlighted?.invoke(it)
            }
            setEntryData(entries)
        }
    }

    private fun setupUi() {
        lineChart.apply {
            axisLeft.setDrawGridLines(false)
            axisLeft.setDrawAxisLine(false)
            axisLeft.setDrawLabels(false)

            axisRight.setDrawGridLines(false)
            axisRight.setDrawAxisLine(false)
            axisRight.setDrawLabels(false)

            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawLabels(false)

            description.isEnabled = false
            legend.isEnabled = false
            setDrawBorders(false)
            setDrawGridBackground(false)

            minOffset = 0f
            extraTopOffset = 20f
        }
    }

    private fun setLive(isLive: Boolean) {
        lineChart.extraRightOffset = if (isLive) {
            24f
        } else {
            0f
        }

        setEntryData(entries)
    }

    fun setData(entries: List<ChartEntry>) {
        setEntryData(entries.map {
            Entry(it.x, it.y)
        })
    }

    private fun setEntryData(entries: List<Entry>) {
        this.entries = entries

        var firstEntries: List<Entry> = emptyList()
        var secondEntries: List<Entry> = emptyList()
        var mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.chart_circle)

        selectedEntry?.let {
            val index = entries.indexOf(it)
            firstEntries = entries.slice(0 .. index)
            secondEntries = entries.slice(index until entries.size)
            mode = LineDataSet.Mode.LINEAR
            val h = Highlight(it.x, it.y, 0)
            h.dataIndex = index
            lineChart.highlightValue(h)
            icon?.alpha = 40
        } ?: run {
            firstEntries = entries
            lineChart.highlightValue(0f, -1)
        }

        val firstDataset = getLineDataSet(firstEntries, R.color.colorPrimary, mode)
        val secondDataset = getLineDataSet(secondEntries, R.color.colorScrub, mode)

        lineChart.data = LineData(
            firstDataset,
            secondDataset
        )

        if (isChartLive) {
            entries.forEach {
                it.icon = null
            }
            entries.lastOrNull()?.icon = icon
        } else {
            entries.forEach {
                it.icon = null
            }
        }

        lineChart.setDrawMarkers(true)
        lineChart.marker = ScrubMarker(context, datePattern)

        invalidate()
    }

    private fun getLineDataSet(entries: List<Entry>, @ColorRes colorRes: Int, mode: LineDataSet.Mode): LineDataSet {
        return LineDataSet(entries, null).apply {
            color = ContextCompat.getColor(context, colorRes)
            lineWidth = 2f
            this.mode = mode
            setDrawValues(false)
            setDrawCircles(false)
            isHighlightEnabled = true
            lineChart.isHighlightPerDragEnabled = false
            lineChart.isHighlightPerTapEnabled = false
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(true)
            highlightLineWidth = 1f
            highLightColor = ContextCompat.getColor(context, R.color.colorText)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(context, R.drawable.chart_background)
        }
    }

    companion object {
        private const val LONG_PRESS_DURATION = 350L
    }

}
