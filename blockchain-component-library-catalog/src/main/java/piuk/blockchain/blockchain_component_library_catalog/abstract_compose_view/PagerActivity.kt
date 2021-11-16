package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blockchain.componentlib.control.PagerIndicatorDotsView
import kotlinx.coroutines.delay
import piuk.blockchain.blockchain_component_library_catalog.R

class PagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager)
        val count = 5
        val pager = findViewById<PagerIndicatorDotsView>(R.id.pager).apply {
            this.count = count
        }

        lifecycleScope.launchWhenStarted {
            var selectedIndex = 0
            while (true) {
                delay(2000)
                selectedIndex = (selectedIndex + 1) % count
                pager.selectedIndex = selectedIndex
            }
        }
    }
}