package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.alert.CustomEmptyState
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import piuk.blockchain.android.R

class CustomEmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    @get:StringRes
    var title: Int by mutableStateOf(R.string.common_empty_title)
    @get:StringRes
    var description: Int by mutableStateOf(R.string.common_empty_details)
    var descriptionText: String? by mutableStateOf(null)
    @get:DrawableRes
    var icon: Int by mutableStateOf(Icons.Filled.User.id)
    @get:StringRes
    var secondaryText: Int? by mutableStateOf(null)
    var secondaryAction: (() -> Unit)? by mutableStateOf(null)
    @get:StringRes
    var ctaText: Int by mutableStateOf(R.string.common_empty_cta)
    var ctaAction: () -> Unit by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                CustomEmptyState(
                    title = title,
                    description = description,
                    descriptionText = descriptionText,
                    icon = icon,
                    secondaryText = secondaryText,
                    secondaryAction = secondaryAction,
                    ctaText = ctaText,
                    ctaAction = ctaAction,
                )
            }
        }
    }
}
