package com.blockchain.charts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import java.text.SimpleDateFormat
import java.util.Locale

internal class ScrubMarker(
    context: Context,
    datePattern: String,
    layoutResource: Int = R.layout.scrub_marker
) : MarkerView(context, layoutResource) {
    private var uiScreenWidth = 0
    private lateinit var currentHighlight: Highlight

    init {
        uiScreenWidth = resources.displayMetrics.widthPixels
    }

    private val date = findViewById<TextView>(R.id.date)
    private val simpleDateFormat = SimpleDateFormat(datePattern, Locale.getDefault())

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry, highlight: Highlight) {
        currentHighlight = highlight

        when (highlight) {
            is PeakHighlight -> date.text = "${highlight.fiatSymbol}${e.y}"
            is TroughHighlight -> date.text = "${highlight.fiatSymbol}${e.y}"
            else -> date.text = simpleDateFormat.format(e.x * 1000L)
        }
        super.refreshContent(e, highlight)
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        if (canvas == null) return
        // Check marker position and update offsets.
        val w = width
        var xPos = posX
        if (uiScreenWidth - posX - w < w) {
            xPos -= w
        }

        when {
            ::currentHighlight.isInitialized && currentHighlight is PeakHighlight -> {
                canvas.translate(xPos, posY - VERTICAL_ADJUSTMENT_MAX)
                draw(canvas)
                canvas.translate(-xPos, -posY + VERTICAL_ADJUSTMENT_MAX)
            }
            ::currentHighlight.isInitialized && currentHighlight is TroughHighlight -> {
                canvas.translate(xPos, posY - VERTICAL_ADJUSTMENT_MIN)
                draw(canvas)
                canvas.translate(-xPos, -posY + VERTICAL_ADJUSTMENT_MIN)
            }
            else -> {
                canvas.translate(xPos, 0f)
                draw(canvas)
                canvas.translate(-xPos, 0f)
            }
        }
    }

    companion object {
        private const val VERTICAL_ADJUSTMENT_MIN = 20
        private const val VERTICAL_ADJUSTMENT_MAX = 60
    }
}
