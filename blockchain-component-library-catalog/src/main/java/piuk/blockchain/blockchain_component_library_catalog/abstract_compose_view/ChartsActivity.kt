package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.blockchain.charts.ChartEntry
import com.blockchain.charts.ChartView
import piuk.blockchain.blockchain_component_library_catalog.R

class ChartsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        val chart = findViewById<ChartView>(R.id.default_charts)
        chart.apply {
            val entries =
                listOf(
                    ChartEntry(1.6389783E9F, 50666.23f),
                    ChartEntry(1.6389792E9F, 50697.32f),
                    ChartEntry(1.6389801E9F, 50737.23f),
                    ChartEntry(1.6389809E9F, 50620.23f),
                    ChartEntry(1.6389818E9F, 50516.23f),
                    ChartEntry(1.6389827E9F, 50424.23f),
                    ChartEntry(1.6389836E9F, 50499.23f),
                    ChartEntry(1.6389845E9F, 50454.23f),
                    ChartEntry(1.6389854E9F, 50340.35f),
                    ChartEntry(1.63898637E9F, 50263.35f)
                )
            setData(entries)
        }

        findViewById<AppCompatButton>(R.id.date_pattern_button).apply {
            setOnClickListener {
                chart.datePattern = "MM-dd-yyyy"
            }
        }

        findViewById<AppCompatButton>(R.id.toggle_live).apply {
            setOnClickListener {
                chart.isChartLive = !chart.isChartLive
            }
        }
    }
}