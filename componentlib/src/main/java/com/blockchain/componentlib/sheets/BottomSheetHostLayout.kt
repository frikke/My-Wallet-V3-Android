package com.blockchain.componentlib.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetHostLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    onBackAction: () -> Unit,
    modalBottomSheetState: ModalBottomSheetState,
    content: @Composable () -> Unit
) {
    BackHandler(modalBottomSheetState.isVisible) {
        onBackAction()
    }

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetState = modalBottomSheetState,
        sheetShape = RoundedCornerShape(
            topEnd = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
            topStart = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
        ),
        sheetContent = sheetContent,
        scrimColor = AppTheme.colors.scrim
    ) {
        content()
    }
}
