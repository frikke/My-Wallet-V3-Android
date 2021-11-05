package piuk.blockchain.blockchain_component_library_catalog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.airbnb.android.showkase.models.Showkase
import com.google.android.material.button.MaterialButton
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ActionTableRowActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.BalanceTableRowActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ColorsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DefaultTableRowActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DividerActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SpacingActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.TagsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.TypographyActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.colors).setOnClickListener {
            startActivity(Intent(this@MainActivity, ColorsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.typography).setOnClickListener {
            startActivity(Intent(this@MainActivity, TypographyActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.spacing).setOnClickListener {
            startActivity(Intent(this@MainActivity, SpacingActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.tags).setOnClickListener {
            startActivity(Intent(this@MainActivity, TagsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.default_table_row).setOnClickListener {
            startActivity(Intent(this@MainActivity, DefaultTableRowActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.action_table_row).setOnClickListener {
            startActivity(Intent(this@MainActivity, ActionTableRowActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.balance_table_row).setOnClickListener {
            startActivity(Intent(this@MainActivity, BalanceTableRowActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.dividers).setOnClickListener {
            startActivity(Intent(this@MainActivity, DividerActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.showkase_button).setOnClickListener {
            startActivity(Showkase.getBrowserIntent(this@MainActivity))
        }
    }
}
