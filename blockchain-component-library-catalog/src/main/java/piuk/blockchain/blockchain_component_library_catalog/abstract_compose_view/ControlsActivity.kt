package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.control.CheckboxView
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.control.RadioView
import com.blockchain.componentlib.control.SearchView
import piuk.blockchain.blockchain_component_library_catalog.R

class ControlsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controls)

        findViewById<SearchView>(R.id.search_view).apply {
            label = "Search Coins"
            onValueChange = {}
        }

        findViewById<CheckboxView>(R.id.checkbox_error).apply {
            state = CheckboxState.Error
        }

        findViewById<RadioView>(R.id.radio_error).apply {
            state = RadioButtonState.Error
        }
    }
}