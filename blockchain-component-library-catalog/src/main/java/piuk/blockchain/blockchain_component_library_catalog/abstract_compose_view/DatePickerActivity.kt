package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.pickers.DateCalendarView
import com.blockchain.componentlib.pickers.DateRowData
import com.blockchain.componentlib.pickers.DateRowView
import com.blockchain.componentlib.pickers.DoubleDateRowView
import piuk.blockchain.blockchain_component_library_catalog.R
import java.util.Date

class DatePickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_date_picker)

        findViewById<DateRowView>(R.id.date_row_view).apply {
            dateRowData = DateRowData(
                "Date", "Sep 21, 2021", false
            ) {
                dateRowData = dateRowData.copy(isActive = !dateRowData.isActive)
            }
        }

        findViewById<DoubleDateRowView>(R.id.double_date_row_view).apply {
            topDateRowData = DateRowData(
                "Begin", "Sep 21, 2021", false
            ) {
                topDateRowData = topDateRowData.copy(isActive = !topDateRowData.isActive)
            }

            bottomDateRowData = DateRowData(
                "End", "Sep 21, 2021", false
            ) {
                bottomDateRowData = bottomDateRowData.copy(isActive = !bottomDateRowData.isActive)
            }
        }

        findViewById<DateCalendarView>(R.id.date_calendar).apply {
            minimumDate = Date()
            onDateSelected = {
            }
        }
    }
}