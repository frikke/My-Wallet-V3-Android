package com.blockchain.componentlib.pickers

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import java.util.Date

class DateCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var minimumDate by mutableStateOf(null as? Date?)
    var maximumDate by mutableStateOf(null as? Date?)
    var onDateSelected by mutableStateOf({ _: Date -> })

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                DateCalendar(
                    minimumDate = minimumDate,
                    maximumDate = maximumDate,
                    onDateSelected = onDateSelected
                )
            }
        }
    }

    fun clearState() {
        minimumDate = null
        maximumDate = null
        onDateSelected = { _: Date -> }
    }
}
