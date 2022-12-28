package com.blockchain.componentlib.media

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import coil.annotation.ExperimentalCoilApi
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

@SuppressLint("GetContentDescriptionOverride")
@OptIn(ExperimentalCoilApi::class)
class AsyncMediaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var url by mutableStateOf("")
    var contentDescription by mutableStateOf("")
    var contentScale by mutableStateOf(ContentScale.Fit)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                AsyncMediaItem(
                    url = url,
                    contentDescription = contentDescription,
                    contentScale = contentScale
                )
            }
        }
    }
}
