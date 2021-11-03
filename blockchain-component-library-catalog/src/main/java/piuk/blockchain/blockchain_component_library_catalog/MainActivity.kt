package piuk.blockchain.blockchain_component_library_catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.models.Showkase
import piuk.blockchain.blockchain_component_library_catalog.theme.CatalogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatalogTheme {
                Surface(color = MaterialTheme.colors.background) {
                }
            }
        }

        startActivity(Showkase.getBrowserIntent(this))
    }
}
