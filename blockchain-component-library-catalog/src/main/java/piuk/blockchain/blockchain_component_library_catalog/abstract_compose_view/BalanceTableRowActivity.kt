package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.text.buildAnnotatedString
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.tablerow.BalanceStackedIconTableRowView
import com.blockchain.componentlib.tablerow.BalanceTableRowLargeView
import com.blockchain.componentlib.tablerow.BalanceTableRowView
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import piuk.blockchain.blockchain_component_library_catalog.R

class BalanceTableRowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_table_row)

        findViewById<BalanceTableRowView>(R.id.default_table_row).apply {
            titleStart = buildAnnotatedString { append("Bitcoin") }
            titleEnd = buildAnnotatedString { append("\$44,403.13") }
            bodyStart = buildAnnotatedString { append("BTC") }
            bodyEnd = buildAnnotatedString { append("↓ 12.32%") }
            startImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
            onClick = {
                Toast.makeText(this@BalanceTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<BalanceTableRowView>(R.id.tag_table_row).apply {
            titleStart = buildAnnotatedString { append("Bitcoin") }
            titleEnd = buildAnnotatedString { append("\$44,403.13") }
            bodyStart = buildAnnotatedString { append("BTC") }
            bodyEnd = buildAnnotatedString { append("↓ 12.32%") }
            startImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
            tags = listOf(TagViewState("Confirmed", TagType.Success))
            onClick = {
                Toast.makeText(this@BalanceTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<BalanceStackedIconTableRowView>(R.id.stacked_icon_table_row).apply {
            titleStart = buildAnnotatedString { append("Bitcoin") }
            titleEnd = buildAnnotatedString { append("\$44,403.13") }
            bodyStart = buildAnnotatedString { append("BTC") }
            bodyEnd = buildAnnotatedString { append("↓ 12.32%") }
            topImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
            bottomImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-eth.svg",
                contentDescription = null,
            )
            onClick = {
                Toast.makeText(this@BalanceTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<BalanceTableRowLargeView>(R.id.balance_large).apply {
            title = "Bitcoin"
            historicalRates = List(10) {
                object : SparkLineHistoricalRate {
                    override val timestamp: Long = it.toLong()
                    override val rate: Double = Math.random() * 1000
                }
            }
            primaryBylineStart = buildAnnotatedString { append("\$15,879.90") }
            primaryBylineEnd = buildAnnotatedString { append("\$44,403.13") }
            secondaryBylineStart = buildAnnotatedString { append("0.3576301941 BTC") }
            secondaryBylineEnd = buildAnnotatedString { append("↓ 12.32%") }
            startImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
            onClick = { }
        }
    }
}