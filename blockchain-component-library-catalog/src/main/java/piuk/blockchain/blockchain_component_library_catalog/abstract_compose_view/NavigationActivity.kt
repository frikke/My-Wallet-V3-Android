package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.navigation.NavigationBarView
import piuk.blockchain.blockchain_component_library_catalog.R

class NavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_navigation)


        findViewById<NavigationBarView>(R.id.default_navigation).apply {
            title = "Activity"
            onBackButtonClick = null
        }

        findViewById<NavigationBarView>(R.id.text_navigation).apply {
            title = "Activity"
            onBackButtonClick = {
                Toast.makeText(this@NavigationActivity, "Back button clicked", Toast.LENGTH_SHORT).show()
            }
            navigationBarButtons = listOf(
                NavigationBarButton.Text("Cancel") {
                    Toast.makeText(this@NavigationActivity, "Text button clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<NavigationBarView>(R.id.icon_navigation).apply {
            title = "Activity"
            onBackButtonClick = {
                Toast.makeText(this@NavigationActivity, "Back button clicked", Toast.LENGTH_SHORT).show()
            }
            navigationBarButtons = listOf(
                NavigationBarButton.Icon(R.drawable.ic_bottom_nav_home) {
                    Toast.makeText(this@NavigationActivity, "First icon button clicked", Toast.LENGTH_SHORT).show()
                },
                NavigationBarButton.Icon(R.drawable.ic_bottom_nav_activity) {
                    Toast.makeText(this@NavigationActivity, "Second icon button clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}