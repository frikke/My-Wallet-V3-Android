package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.tablerow.ActionStackedIconTableRowView
import com.blockchain.componentlib.tablerow.ActionTableRowView
import com.blockchain.componentlib.tablerow.DefaultStackedIconTableRowView
import com.blockchain.componentlib.tablerow.DefaultTableRowView
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import piuk.blockchain.blockchain_component_library_catalog.R

class ActionTableRowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_table_row)

        findViewById<ActionTableRowView>(R.id.default_table_row).apply {
            startIconUrl = "https://www.blockchain.com/static/img/prices/prices-btc.svg"
            primaryText = "Trading"
            secondaryText = "Buy & Sell"
            onClick = {
                Toast.makeText(this@ActionTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<ActionTableRowView>(R.id.tag_table_row).apply {
            startIconUrl = "https://www.blockchain.com/static/img/prices/prices-btc.svg"
            primaryText = "Email Address"
            secondaryText = "satoshi@blockchain.com"
            tags = listOf(TagViewState("Confirmed", TagType.Success))
            onClick = {
                Toast.makeText(this@ActionTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<ActionStackedIconTableRowView>(R.id.stacked_icon_table_row).apply {
            primaryText = "Primary text"
            secondaryText = "Secondary text"
            iconTopUrl = "https://www.blockchain.com/static/img/prices/prices-btc.svg"
            iconBottomUrl = "https://www.blockchain.com/static/img/prices/prices-eth.svg"
            onClick = {
                Toast.makeText(this@ActionTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<ActionTableRowView>(R.id.large_table_row).apply {
            startIconUrl = "https://www.blockchain.com/static/img/prices/prices-btc.svg"
            primaryText = "Link a bank"
            secondaryText = "Instant connection"
            paragraphText = "Securely link a bank to buy crypto, deposit cash " +
                "and withdraw back to your bank at anytime."
            tags = listOf(TagViewState("Fastest", TagType.Success))
        }
    }
}