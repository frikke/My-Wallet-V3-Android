package com.blockchain.componentlib.card.abstract

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.card.CallOutCard
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.image.ImageResource

class CallOutCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var iconResource: ImageResource by mutableStateOf(ImageResource.None)
    var callToActionButton by mutableStateOf(null as? CardButton?)

    @Composable
    override fun Content() {
        CallOutCard(
            title = title,
            subtitle = subtitle,
            iconResource = iconResource,
            callToActionButton = callToActionButton
        )
    }
}
