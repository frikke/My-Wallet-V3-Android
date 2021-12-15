package com.blockchain.componentlib.card.abstract

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.card.AnnouncementCard
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class AnnouncementCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var iconResource: ImageResource by mutableStateOf(ImageResource.None)
    var onClose by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                AnnouncementCard(
                    title = title,
                    subtitle = subtitle,
                    iconResource = iconResource,
                    onClose = onClose
                )
            }
        }
    }

    fun clearState() {
        title = ""
        subtitle = ""
        iconResource = ImageResource.None
        onClose = {}
    }
}
