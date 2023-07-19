package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.graphics.toArgb
import com.blockchain.componentlib.utils.ViewSystemUtils
import piuk.blockchain.blockchain_component_library_catalog.R

class ColorsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_colors)

        findViewById<AppCompatImageView>(R.id.title_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).title.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.body_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).body.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.muted_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).muted.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.dark_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).dark.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.medium_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).medium.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.light_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).light.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.background_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).backgroundSecondary.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.primary_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).primary.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.success_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).success.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.warning_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).warning.toArgb()
        )
        findViewById<AppCompatImageView>(R.id.error_color).setBackgroundColor(
            ViewSystemUtils.getSemanticColors(this).error.toArgb()
        )
    }
}