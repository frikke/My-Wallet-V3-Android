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
        if (highlight is PeakOrTroughHighlight) {
            date.text = "${highlight.fiatSymbol}${e.y}"
        } else {
            date.text = simpleDateFormat.format(e.x * 1000L)
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

        if (::currentHighlight.isInitialized && currentHighlight is PeakOrTroughHighlight) {
            // translate to the correct position and draw
            canvas.translate(xPos, posY - VERTICAL_ADJUSTMENT)
            draw(canvas)
            canvas.translate(-xPos, -posY + VERTICAL_ADJUSTMENT)
        } else {
            // translate to the correct position and draw
            canvas.translate(xPos, 0f)
            draw(canvas)
            canvas.translate(-xPos, 0f)
        }
    }

    companion object {
        private const val VERTICAL_ADJUSTMENT = 20
    }
}
