package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.DefaultTableRowView
import com.blockchain.componentlib.tablerow.ToggleTableRowView
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import piuk.blockchain.blockchain_component_library_catalog.R

class DefaultTableRowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_table_row)

        findViewById<DefaultTableRowView>(R.id.default_table_row).apply {
            primaryText = "Trading"
            secondaryText = "Buy & Sell"
            onClick = {
                Toast.makeText(this@DefaultTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<DefaultTableRowView>(R.id.end_tag).apply {
            primaryText = "Trading"
            secondaryText = "Buy & Sell"
            onClick = {
                Toast.makeText(this@DefaultTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
            endTag = TagViewState("Completed", TagType.Success())
        }

        findViewById<DefaultTableRowView>(R.id.default_image_table_row).apply {
            primaryText = "Trading"
            secondaryText = "Buy & Sell"
            onClick = {
                Toast.makeText(this@DefaultTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
            startImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
        }

        findViewById<DefaultTableRowView>(R.id.tag_table_row).apply {
            primaryText = "Email Address"
            secondaryText = "satoshi@blockchain.com"
            tags = listOf(TagViewState("Confirmed", TagType.Success()))
            onClick = {
                Toast.makeText(this@DefaultTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<DefaultTableRowView>(R.id.large_table_row).apply {
            primaryText = "Link a bank"
            secondaryText = "Instant connection"
            paragraphText = "Securely link a bank to buy crypto, deposit cash " +
                "and withdraw back to your bank at anytime."
            tags = listOf(TagViewState("Fastest", TagType.Success()))
        }

        findViewById<DefaultTableRowView>(R.id.large_table_row_with_image).apply {
            primaryText = "Link a bank"
            secondaryText = "Instant connection"
            paragraphText = "Securely link a bank to buy crypto, deposit cash " +
                "and withdraw back to your bank at anytime."
            startImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
            tags = listOf(TagViewState("Fastest", TagType.Success()))
        }

        findViewById<ToggleTableRowView>(R.id.toggle_table_row).apply {
            primaryText = "Cloud backup"
            secondaryText = "Buy & Sell"
            onCheckedChange = {
                Toast.makeText(this@DefaultTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}