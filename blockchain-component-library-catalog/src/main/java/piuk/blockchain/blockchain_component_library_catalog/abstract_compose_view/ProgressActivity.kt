package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.system.CircularProgressBarView
import com.blockchain.componentlib.system.LinearProgressBarView
import piuk.blockchain.blockchain_component_library_catalog.R

class ProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        findViewById<LinearProgressBarView>(R.id.linear_determinate).apply {
            progress = 0.5f
        }

        findViewById<CircularProgressBarView>(R.id.circular_determinate).apply {
            progress = 0.5f
        }

        findViewById<CircularProgressBarView>(R.id.circular_indeterminate_text).apply {
            text = "Checking for Update..."
        }
    }
}