package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.control.TabLayoutLargeView
import com.blockchain.componentlib.control.TabLayoutLiveView
import piuk.blockchain.blockchain_component_library_catalog.R

class TabLayoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_layout)

        findViewById<TabLayoutLargeView>(R.id.large).apply {
            this.items = listOf("First", "Second", "Third")
            this.onItemSelected = {}
            this.selectedItemIndex = 0
        }

        findViewById<TabLayoutLiveView>(R.id.live).apply {
            this.items = listOf("Live", "1D", "1W", "1M", "1Y", "All")
            this.onItemSelected = {}
            this.selectedItemIndex = 0
        }
    }
}