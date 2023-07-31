package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.navigation.NavigationBarView
import piuk.blockchain.blockchain_component_library_catalog.R

class NavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_navigation)


        findViewById<NavigationBarView>(R.id.default_navigation).apply {
            title = "Activity"
            endNavigationBarButtons = listOf(
                NavigationBarButton.Icon(
                    drawable = com.blockchain.componentlib.R.drawable.ic_bottom_nav_activity,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
                ) {
                    Toast.makeText(this@NavigationActivity, "First icon button clicked", Toast.LENGTH_SHORT).show()
                },
                NavigationBarButton.Icon(
                    drawable = com.blockchain.componentlib.R.drawable.ic_bottom_nav_activity,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
                ) {
                    Toast.makeText(this@NavigationActivity, "Second icon button clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<NavigationBarView>(R.id.only_text_navigation).apply {
            title = "Activity"
        }

        findViewById<NavigationBarView>(R.id.text_navigation).apply {
            title = "Activity"
            onBackButtonClick = {
                Toast.makeText(this@NavigationActivity, "Back button clicked", Toast.LENGTH_SHORT).show()
            }
            endNavigationBarButtons = listOf(
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
            endNavigationBarButtons = listOf(
                NavigationBarButton.Icon(
                    drawable = com.blockchain.componentlib.R.drawable.ic_bottom_nav_prices,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
                ) {
                    Toast.makeText(this@NavigationActivity, "First icon button clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<NavigationBarView>(R.id.icon_navigation).apply {
            title = "Activity"
            onBackButtonClick = {
                Toast.makeText(this@NavigationActivity, "Back button clicked", Toast.LENGTH_SHORT).show()
            }
            endNavigationBarButtons = listOf(
                NavigationBarButton.Text(
                    "Done",
                    Color(
                        ContextCompat.getColor(
                            context,
                            com.blockchain.componentlib.R.color.paletteBasePrimary
                        )
                    )
                ) {
                    Toast.makeText(this@NavigationActivity, "Text button clicked", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}