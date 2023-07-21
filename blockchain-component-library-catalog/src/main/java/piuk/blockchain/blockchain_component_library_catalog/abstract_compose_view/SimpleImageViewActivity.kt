package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleImageView
import piuk.blockchain.blockchain_component_library_catalog.R

class SimpleImageViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_image_view)

        findViewById<SimpleImageView>(R.id.local_image_view).apply {
            image = ImageResource.Local(
                id = com.blockchain.componentlib.R.drawable.ic_blockchain,
                contentDescription = ""
            )
            scaleType = ContentScale.Fit
        }

        findViewById<SimpleImageView>(R.id.local_image_w_bkgd_view).apply {
            image = ImageResource.LocalWithBackground(
                id = com.blockchain.componentlib.R.drawable.ic_blockchain,
                iconColor = Color.Blue,
                backgroundColor = Color.Black,
                contentDescription = "",
                size = Dp(32f),
                iconSize = Dp(24f)
            )
            scaleType = ContentScale.Fit
        }

        findViewById<SimpleImageView>(R.id.remote_image_view).apply {
            image = ImageResource.Remote(
                url = "https://cloudfront-us-east-1.images.arcpublishing.com/coindesk/XA6KIXE6FBFM5EWSA25JI5YAU4.jpg",
                contentDescription = "",
                size = Dp(32f),
            )
            scaleType = ContentScale.Fit
        }
    }
}