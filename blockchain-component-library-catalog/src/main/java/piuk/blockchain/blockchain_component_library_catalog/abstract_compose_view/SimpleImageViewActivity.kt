package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.layout.ContentScale
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleImageView
import piuk.blockchain.blockchain_component_library_catalog.R

class SimpleImageViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_image_view)

        findViewById<SimpleImageView>(R.id.local_image_view).apply {
            image = ImageResource.Local(
                id = R.drawable.ic_blockchain,
                contentDescription = ""
            )
            imageSize = 24
            scaleType = ContentScale.Fit
        }

        findViewById<SimpleImageView>(R.id.local_image_w_bkgd_view).apply {
            image = ImageResource.LocalWithBackground(
                id = R.drawable.ic_blockchain,
                iconTintColour = R.color.colorPrimary,
                backgroundColour = R.color.black,
                contentDescription = ""
            )
            imageSize = 24
            scaleType = ContentScale.Fit
        }

        findViewById<SimpleImageView>(R.id.remote_image_view).apply {
            image = ImageResource.Remote(
                url = "https://cloudfront-us-east-1.images.arcpublishing.com/coindesk/XA6KIXE6FBFM5EWSA25JI5YAU4.jpg",
                contentDescription = ""
            )
            imageSize = 24
            scaleType = ContentScale.Fit
        }
    }
}