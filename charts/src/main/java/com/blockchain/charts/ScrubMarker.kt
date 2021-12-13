package com.blockchain.charts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Locale

internal class ScrubMarker(
    context: Context,
    datePattern: String,
    layoutResource: Int = R.layout.scrub_marker
) : MarkerView(context, layoutResource) {

    private val date = findViewById<TextView>(R.id.date)
    private val simpleDateFormat = SimpleDateFormat(datePattern, Locale.getDefault())

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry, highlight: Highlight) {
        date.text = simpleDateFormat.format(e.x * 1000L)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width).toFloat(), 0f)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        return MPPointF((-width).toFloat()/2, 0f)
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        if (canvas == null) return

        val offset = getOffsetForDrawingAtPoint(posX, posY)

        val saveId = canvas.save()
        canvas.translate(posX + offset.x, 0f)
        draw(canvas)
        canvas.restoreToCount(saveId)
    }
}
