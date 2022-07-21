package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.switcher.SwitcherItemView
import com.blockchain.componentlib.switcher.SwitcherState
import piuk.blockchain.blockchain_component_library_catalog.R

class SwitcherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switcher)

        findViewById<SwitcherItemView>(R.id.switcher_enabled).apply {
            text = "Enabled"
            switcherState = SwitcherState.Enabled
        }

        findViewById<SwitcherItemView>(R.id.switcher_disabled).apply {
            text = "Disabled"
            switcherState = SwitcherState.Disabled
        }

        findViewById<SwitcherItemView>(R.id.switcher_enabled_start_icon).apply {
            text = "Enabled Icon"
            startIcon = ImageResource.Local(
                contentDescription = "Close",
                id = R.drawable.ic_close
            )
            endIcon = ImageResource.Local(
                contentDescription = "Refresh",
                id = R.drawable.ic_refresh
            )
            switcherState = SwitcherState.Enabled
        }

        findViewById<SwitcherItemView>(R.id.switcher_disabled_start_icon).apply {
            text = "Disabled Icon"
            startIcon = ImageResource.Local(
                contentDescription = "Close",
                id = R.drawable.ic_close
            )
            endIcon = ImageResource.Local(
                contentDescription = "Refresh",
                id = R.drawable.ic_refresh
            )
            switcherState = SwitcherState.Disabled
        }
    }
}