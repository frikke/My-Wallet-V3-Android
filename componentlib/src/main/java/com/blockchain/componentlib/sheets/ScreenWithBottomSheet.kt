package com.blockchain.componentlib.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import kotlinx.coroutines.launch

typealias ShowSheet<T> = (T) -> Unit
typealias HideSheet = () -> Unit

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> ScreenWithBottomSheet(
    sheetContent: @Composable (T, HideSheet) -> Unit,
    content: @Composable (ShowSheet<T>) -> Unit,
) {
    var data: T? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    )

    fun hide() {
        coroutineScope.launch {
            sheetState.hide()
        }
    }

    fun show() {
        coroutineScope.launch {
            sheetState.show()
        }
    }

    BackHandler(sheetState.isVisible) {
        hide()
    }

    ModalBottomSheetLayout(
        modifier = Modifier.background(AppColors.background),
        sheetState = sheetState,
        sheetShape = AppTheme.shapes.veryLarge.topOnly(),
        sheetBackgroundColor = Color.Transparent,
        scrimColor = AppColors.scrim,
        sheetContent = {
            data?.let {
                sheetContent(it, ::hide)
            }
        },
        content = {
            content {
                data = it
                show()
            }
        }
    )
}
