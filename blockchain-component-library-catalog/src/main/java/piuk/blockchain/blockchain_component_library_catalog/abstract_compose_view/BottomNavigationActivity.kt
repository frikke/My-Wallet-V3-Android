package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.navigation.BottomNavigationBarView
import piuk.blockchain.blockchain_component_library_catalog.R

class BottomNavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        findViewById<BottomNavigationBarView>(R.id.default_bottom_navigation).apply {
            onNavigationItemClick = {
                selectedNavigationItem = it
            }
        }
    }
}