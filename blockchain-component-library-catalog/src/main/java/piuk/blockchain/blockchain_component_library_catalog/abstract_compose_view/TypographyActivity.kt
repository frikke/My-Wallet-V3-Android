package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.theme.AppTypography
import com.google.android.material.textview.MaterialTextView
import piuk.blockchain.blockchain_component_library_catalog.R

class TypographyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_typography)
    }
}