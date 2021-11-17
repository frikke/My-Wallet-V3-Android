package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.tablerow.ActionStackedIconTableRowView
import piuk.blockchain.blockchain_component_library_catalog.R

class ActionTableRowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_table_row)

        findViewById<ActionStackedIconTableRowView>(R.id.stacked_icon_table_row).apply {
            primaryText = "Primary text"
            secondaryText = "Secondary text"
            topImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                contentDescription = null,
            )
            bottomImageResource = ImageResource.Remote(
                url = "https://www.blockchain.com/static/img/prices/prices-eth.svg",
                contentDescription = null,
            )
            onClick = {
                Toast.makeText(this@ActionTableRowActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}