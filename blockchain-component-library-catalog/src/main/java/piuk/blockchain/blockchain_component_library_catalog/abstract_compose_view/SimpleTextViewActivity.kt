package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.layout.ContentScale
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleImageView
import com.blockchain.componentlib.basic.SimpleTextView
import piuk.blockchain.blockchain_component_library_catalog.R

class SimpleTextViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_text_view)

        findViewById<SimpleTextView>(R.id.default_text_view).apply {
            text = "Body 1 style & colour"
            style = ComposeTypographies.Body1
            textColor = ComposeColors.Body
        }
    }
}