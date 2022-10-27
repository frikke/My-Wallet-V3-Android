package com.blockchain.home.presentation.activity.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.tablerow.generic.GenericTableRow
import com.blockchain.componentlib.theme.AppTheme

sealed interface ActivityComponent {
    data class StackView(
        val leadingImagePrimaryUrl: String? = null,
        val leadingImageImageSecondaryUrl: String? = null,
        val leading: List<ActivityStackView>,
        val trailing: List<ActivityStackView>,
    ) : ActivityComponent

    data class Button(
        val value: String,
        val style: ActivityButtonStyle
    ) : ActivityComponent
}

@Composable
fun ActivityComponentItem(component: ActivityComponent, onClick: (() -> Unit)? = null) {
    when (component) {
        is ActivityComponent.Button -> {
            ActivityDetailButton(
                data = component,
                onClick = onClick
            )
        }
        is ActivityComponent.StackView -> {
            GenericTableRow(
                leadingImagePrimaryUrl = component.leadingImagePrimaryUrl,
                leadingImageSecondaryUrl = component.leadingImageImageSecondaryUrl,
                leadingComponents = component.leading.map { it.toViewType() },
                trailingComponents = component.trailing.map { it.toViewType() },
                onClick = onClick
            )
        }
    }
}

@Composable
fun ActivitySectionCard(
    modifier: Modifier = Modifier,
    components: List<ActivityComponent>,
    onClick: (() -> Unit)? = null
) {
    if (components.isNotEmpty()) {
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            Column(modifier = modifier) {
                components.forEachIndexed { index, transaction ->
                    ActivityComponentItem(component = transaction, onClick = onClick)

                    if (index < components.lastIndex) {
                        Divider(color = Color(0XFFF1F2F7))
                    }
                }
            }
        }
    }
}