@file:Suppress("OPT_IN_USAGE")

package piuk.blockchain.blockchain_component_library_catalog.preview.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.media.AsyncMediaItem
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "video mp4", group = "Video")
@Composable
fun AsyncMediaItem_mp4() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://i.imgur.com/lHL4Xy0.mp4",
                contentDescription = "video"
            )
        }
    }
}

@Preview(name = "gif", group = "Gif")
@Composable
fun AsyncMediaItem_gif() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://i.stack.imgur.com/IzNGE.gif",
                contentDescription = "gif"
            )
        }
    }
}

@Preview(name = "png", group = "images")
@Composable
fun AsyncMediaItem_png() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://w7.pngwing.com/pngs/915/345/png-transparent-multicolored-balloons-illustration-" +
                    "balloon-balloon-free-balloons-easter-egg-desktop-wallpaper-party-thumbnail.png",
                contentDescription = "png"
            )
        }
    }
}

@Preview(name = "jpg", group = "images")
@Composable
fun AsyncMediaItem_jpg() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://img-19.commentcamarche.net/cI8qqj-finfDcmx6jMK6Vr-krEw=/1500x/" +
                    "smart/b829396acc244fd484c5ddcdcb2b08f3/ccmcms-commentcamarche/20494859.jpg",
                contentDescription = "jpg"
            )
        }
    }
}

@Preview(name = "lottie", group = "json")
@Composable
fun AsyncMediaItem_lottie() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://assets2.lottiefiles.com/packages/lf20_q77jpumk.json",
                contentDescription = "lottie"
            )
        }
    }
}

@Preview(name = "svg", group = "images")
@Composable
fun AsyncMediaItem_svg() {
    AppTheme {
        AppSurface {
            AsyncMediaItem(
                url = "https://www.svgrepo.com/show/240138/lock.svg",
                contentDescription = "svg"
            )
        }
    }
}
