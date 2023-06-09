package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.CustomBackgroundCardView
import piuk.blockchain.blockchain_component_library_catalog.R

class CardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cards)

        findViewById<CustomBackgroundCardView>(R.id.cta_custom_bckg_card).apply {
            title = "Complete the promo"
            subtitle = "Verify your ID and refer some friends"
            iconResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_star)
            backgroundResource =
                ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_blockchain_logo_with_text)
            isCloseable = true
        }
    }
}
