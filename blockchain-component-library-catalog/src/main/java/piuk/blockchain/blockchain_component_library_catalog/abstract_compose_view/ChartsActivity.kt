package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.charts.BalanceView
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.charts.PercentageChangeState
import com.blockchain.componentlib.charts.PercentageChangeView
import piuk.blockchain.blockchain_component_library_catalog.R

class ChartsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        findViewById<BalanceView>(R.id.default_chart).apply {
            title = "Current Balance"
            price = "$51423.00"
            percentageChangeData = PercentageChangeData("$50.00", 0.24, "Last Hour")
        }

        findViewById<BalanceView>(R.id.today_chart).apply {
            title = "Today"
            price = "$51423.00"
            percentageChangeData = PercentageChangeData("$50.00", -0.24, "Last Hour")
        }

        findViewById<PercentageChangeView>(R.id.percentage_change_positive).apply {
            priceChange = "$50.00"
            percentChange = 0.24
            interval = "Last Hour"
            state = PercentageChangeState.Positive
        }

        findViewById<PercentageChangeView>(R.id.percentage_change_negative).apply {
            priceChange = "$50.00"
            percentChange = -0.24
            interval = "Last Hour"
            state = PercentageChangeState.Negative
        }

        findViewById<PercentageChangeView>(R.id.percentage_change_neutral).apply {
            priceChange = "$50.00"
            percentChange = 0.0
            interval = "Last Hour"
            state = PercentageChangeState.Neutral
        }
    }
}