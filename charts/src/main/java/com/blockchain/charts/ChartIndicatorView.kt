package com.blockchain.charts

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.control.TabLayoutLive
import com.blockchain.componentlib.control.TabLayoutLiveBoxed
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import com.blockchain.extensions.exhaustive

class ChartIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var viewType by mutableStateOf<ViewType?>(null)

    var items: List<String> by mutableStateOf(emptyList())
    var onItemSelected by mutableStateOf({ _: Int -> })
    var selectedItemIndex by mutableStateOf(0)
    var showLiveIndicator by mutableStateOf(true)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                viewType?.let { viewType ->
                    when (viewType) {
                        ViewType.Lined -> {
                            TabLayoutLive(
                                items = items,
                                onItemSelected = { index ->
                                    selectedItemIndex = index
                                    onItemSelected(index)
                                },
                                selectedItemIndex = selectedItemIndex,
                                showLiveIndicator = showLiveIndicator
                            )
                        }

                        ViewType.Boxed -> {
                            TabLayoutLiveBoxed(
                                modifier = Modifier.padding(
                                    horizontal = AppTheme.dimensions.paddingMedium,
                                    vertical = AppTheme.dimensions.paddingMedium
                                ),
                                items = items,
                                onItemSelected = { index ->
                                    selectedItemIndex = index
                                    onItemSelected(index)
                                },
                                selectedItemIndex = selectedItemIndex,
                                showLiveIndicator = showLiveIndicator
                            )
                        }
                    }.exhaustive
                }
            }
        }
    }

    fun clearState() {
        items = emptyList()
        onItemSelected = { _: Int -> }
        selectedItemIndex = 0
    }

    sealed interface ViewType {
        object Lined : ViewType
        object Boxed : ViewType
    }
}
