package com.blockchain.charts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import java.text.DecimalFormat
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
        val entryFormatted = DecimalFormat("0.00").format(e.y)

        when (highlight) {
            is PeakHighlight -> date.text = "${highlight.fiatSymbol}$entryFormatted"
            is TroughHighlight -> date.text = "${highlight.fiatSymbol}$entryFormatted"
            else -> date.text = simpleDateFormat.format(e.x * 1000L)
        }
        super.refreshContent(e, highlight)
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        if (canvas == null) return
        // Check marker position and update offsets.=
        var xPos = posX
        if (uiScreenWidth - posX - width < width) {
            xPos -= width
        }

        // Margins
        if (isStartMarginRequired(xPos)) {
            xPos += MARGIN_SIZE
        } else if (isEndMarginRequired(xPos)) {
            xPos -= MARGIN_SIZE
        }

        when {
            ::currentHighlight.isInitialized && currentHighlight is PeakHighlight -> {
                canvas.translate(xPos, posY - MARGIN_SIZE - height)
                draw(canvas)
                canvas.translate(-xPos, -posY + MARGIN_SIZE + height)
            }
            ::currentHighlight.isInitialized && currentHighlight is TroughHighlight -> {
                canvas.translate(xPos, posY - MARGIN_SIZE)
                draw(canvas)
                canvas.translate(-xPos, -posY + MARGIN_SIZE)
            }
            else -> {
                canvas.translate(xPos, 0f)
                draw(canvas)
                canvas.translate(-xPos, 0f)
            }
        }
    }

    private fun isStartMarginRequired(xPos: Float): Boolean = (xPos - MARGIN_SIZE <= 0)

    private fun isEndMarginRequired(xPos: Float): Boolean = (xPos + width + MARGIN_SIZE >= uiScreenWidth)

    companion object {
        private const val MARGIN_SIZE = 8
    }
}
