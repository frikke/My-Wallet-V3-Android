package piuk.blockchain.blockchain_component_library_catalog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.airbnb.android.showkase.models.Showkase
import com.google.android.material.button.MaterialButton
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.AlertButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.AsyncMediaActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.CardActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.CardAlertActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ChartsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ColorsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ControlsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DatePickerActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DialogueActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.DividerActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ExpandablesActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.NavigationActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.PagerActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.PrimaryButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.ProgressActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SectionHeadersActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SheetActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SimpleImageViewActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SimpleTextViewActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SmallSecondaryButtonActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SnackbarsActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SpacingActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.SwitcherActivity
import piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view.TabLayoutActivity
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
        findViewById<MaterialButton>(R.id.texts).setOnClickListener {
            startActivity(Intent(this@MainActivity, SimpleTextViewActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.expandables).setOnClickListener {
            startActivity(Intent(this@MainActivity, ExpandablesActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.images).setOnClickListener {
            startActivity(Intent(this@MainActivity, SimpleImageViewActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.async_media).setOnClickListener {
            startActivity(Intent(this@MainActivity, AsyncMediaActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.tags).setOnClickListener {
            startActivity(Intent(this@MainActivity, TagsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.dividers).setOnClickListener {
            startActivity(Intent(this@MainActivity, DividerActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.top_navigation).setOnClickListener {
            startActivity(Intent(this@MainActivity, NavigationActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.primary_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, PrimaryButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.alert_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, AlertButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.small_secondary_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, SmallSecondaryButtonActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.controls).setOnClickListener {
            startActivity(Intent(this@MainActivity, ControlsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.text_input).setOnClickListener {
            startActivity(Intent(this@MainActivity, TextInputActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.pager).setOnClickListener {
            startActivity(Intent(this@MainActivity, PagerActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.dialogue).setOnClickListener {
            startActivity(Intent(this@MainActivity, DialogueActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.tabs).setOnClickListener {
            startActivity(Intent(this@MainActivity, TabLayoutActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.section_headers).setOnClickListener {
            startActivity(Intent(this@MainActivity, SectionHeadersActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.section_switchers).setOnClickListener {
            startActivity(Intent(this@MainActivity, SwitcherActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.card_alert).setOnClickListener {
            startActivity(Intent(this@MainActivity, CardAlertActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.cards).setOnClickListener {
            startActivity(Intent(this@MainActivity, CardActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.charts).setOnClickListener {
            startActivity(Intent(this@MainActivity, ChartsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.progress).setOnClickListener {
            startActivity(Intent(this@MainActivity, ProgressActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.date_picker).setOnClickListener {
            startActivity(Intent(this@MainActivity, DatePickerActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.sheets).setOnClickListener {
            startActivity(Intent(this@MainActivity, SheetActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.snackbars).setOnClickListener {
            startActivity(Intent(this@MainActivity, SnackbarsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.showkase_button).setOnClickListener {
            startActivity(Showkase.getBrowserIntent(this@MainActivity))
        }
    }
}
