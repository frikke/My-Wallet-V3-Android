package com.blockchain.componentlib.pickers

import android.content.res.ColorStateList
import android.content.res.Resources
import android.widget.CalendarView
import android.widget.ImageButton
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import java.util.Calendar
import java.util.Date

@Composable
fun DateCalendar(
    minimumDate: Date? = null,
    maximumDate: Date? = null,
    onDateSelected: (Date) -> Unit = {}
) {
    AndroidView(
        modifier = Modifier
            .background(AppTheme.colors.light, AppTheme.shapes.small)
            .wrapContentSize(),
        factory = { context ->
            CalendarView(ContextThemeWrapper(context, R.style.CalenderViewCustom)).apply {
                minimumDate?.let {
                    minDate = it.time
                }

                maximumDate?.let {
                    maxDate = it.time
                }

                /* Sets up the chevron colors */
                val prevButton =
                    findViewById<ImageButton?>(
                        Resources.getSystem().getIdentifier("prev", "id", "android")
                    )

                val nextButton =
                    findViewById<ImageButton?>(
                        Resources.getSystem().getIdentifier("next", "id", "android")
                    )

                prevButton?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.calendarAccent, null))
                nextButton?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.calendarAccent, null))
            }
        },
        update = { view ->
            view.setOnDateChangeListener { _, year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            }
        }
    )
}

@Composable
@Preview
fun DateCalendar_Preview() {
    AppTheme {
        AppSurface {
            DateCalendar()
        }
    }
}
