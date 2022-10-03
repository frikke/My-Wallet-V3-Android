package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.media.AsyncMediaView
import piuk.blockchain.blockchain_component_library_catalog.R

class AsyncMediaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_async_media)

        findViewById<AsyncMediaView>(R.id.video).apply {
            url = "https://i.imgur.com/lHL4Xy0.mp4"
        }

        findViewById<AsyncMediaView>(R.id.gif).apply {
            url = "https://i.stack.imgur.com/IzNGE.gif"
        }

        findViewById<AsyncMediaView>(R.id.png).apply {
            url = "https://w7.pngwing.com/pngs/915/345/png-transparent-multicolored-balloons-illustration-" +
                "balloon-balloon-free-balloons-easter-egg-desktop-wallpaper-party-thumbnail.png"
        }

        findViewById<AsyncMediaView>(R.id.jpg).apply {
            url = "https://img-19.commentcamarche.net/cI8qqj-finfDcmx6jMK6Vr-krEw=/1500x/" +
                "smart/b829396acc244fd484c5ddcdcb2b08f3/ccmcms-commentcamarche/20494859.jpg"
        }

        findViewById<AsyncMediaView>(R.id.lottie).apply {
            url = "https://assets2.lottiefiles.com/packages/lf20_q77jpumk.json"
        }

        findViewById<AsyncMediaView>(R.id.svg).apply {
            url = "https://www.svgrepo.com/show/240138/lock.svg"
        }
    }
}