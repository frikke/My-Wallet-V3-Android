package piuk.blockchain.blockchain_component_library_catalog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.airbnb.android.showkase.models.Showkase
import com.google.android.material.button.MaterialButton
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ActionTableRowActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.AlertButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.BalanceTableRowActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.BottomNavigationActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ColorsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DefaultTableRowActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DividerActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DoubleMinimalButtonsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DoublePrimaryButtonsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ExchangeBuyButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ExchangeSellButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ExchangeSplitButtonsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.MinimalButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.PrimaryButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SecondaryButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SmallMinimalButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SmallPrimaryButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SmallSecondaryButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SpacingActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SplitButtonsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.TagsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.TextInputActivity
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
        findViewById<MaterialButton>(R.id.bottom_navigation).setOnClickListener {
            startActivity(Intent(this@MainActivity, BottomNavigationActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.primary_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, PrimaryButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.alert_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, AlertButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.exchange_buy_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, ExchangeBuyButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.exchange_sell_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, ExchangeSellButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.exchange_split_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, ExchangeSplitButtonsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.minimal_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, MinimalButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.secondary_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, SecondaryButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.small_minimal_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, SmallMinimalButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.small_primary_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, SmallPrimaryButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.small_secondary_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, SmallSecondaryButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.split_buttons).setOnClickListener {
            startActivity(Intent(this@MainActivity, SplitButtonsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.double_primary_buttons).setOnClickListener {
            startActivity(Intent(this@MainActivity, DoublePrimaryButtonsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.double_minimal_buttons).setOnClickListener {
            startActivity(Intent(this@MainActivity, DoubleMinimalButtonsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.text_input).setOnClickListener {
            startActivity(Intent(this@MainActivity, TextInputActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.showkase_button).setOnClickListener {
            startActivity(Showkase.getBrowserIntent(this@MainActivity))
        }
    }
}
