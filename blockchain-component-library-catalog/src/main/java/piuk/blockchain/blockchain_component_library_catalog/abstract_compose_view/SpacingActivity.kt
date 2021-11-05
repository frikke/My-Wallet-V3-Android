package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import piuk.blockchain.blockchain_component_library_catalog.R

class SpacingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spacing)
    }
}