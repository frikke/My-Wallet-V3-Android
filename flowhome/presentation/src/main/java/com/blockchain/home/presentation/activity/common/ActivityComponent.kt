package com.blockchain.home.presentation.activity.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.tablerow.custom.CustomTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon

/**
 * @property id Some components may want to be identified for later interaction
 */
sealed interface ActivityComponent {
    val id: String

    data class StackView(
        override val id: String,
        val leadingImage: ActivityIcon = ActivityIcon.None,
        val leading: List<ActivityStackView>,
        val trailing: List<ActivityStackView>
    ) : ActivityComponent {
        override fun equals(other: Any?) = id == (other as? StackView)?.id
        override fun hashCode() = id.hashCode()
    }

    data class Button(
        override val id: String,
        val value: TextValue,
        val style: ActivityButtonStyle,
        val action: ActivityButtonAction
    ) : ActivityComponent {
        override fun equals(other: Any?) = id == (other as? Button)?.id
        override fun hashCode() = id.hashCode()
    }
}

/**
 * all supported composable types are defined in ActivityComponent
 * this function takes an instance and draws based on which type it is
 */
@Composable
fun ActivityComponentItem(component: ActivityComponent, onClick: ((ClickAction) -> Unit)? = null) {
    when (component) {
        is ActivityComponent.Button -> {
            ActivityDetailButton(
                data = component,
                onClick = { onClick?.invoke(ClickAction.Button(component.action)) }
            )
        }
        is ActivityComponent.StackView -> {
            CustomTableRow(
                icon = component.leadingImage.toStackedIcon(),
                leadingComponents = component.leading.map { it.toViewType() },
                trailingComponents = component.trailing.map { it.toViewType() },
                onClick = { onClick?.invoke(ClickAction.Stack(data = component.id)) }
            )
        }
    }
}

/**
 * Draw a card with a list of [ActivityComponent]
 * actual drawing of components is done by [ActivityComponentItem]
 */
@Composable
fun ActivitySectionCard(
    modifier: Modifier = Modifier,
    components: List<ActivityComponent>,
    onClick: ((ClickAction) -> Unit)? = null
) {
    if (components.isNotEmpty()) {
        Card(
            backgroundColor = AppTheme.colors.backgroundSecondary,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            Column(modifier = modifier) {
                components.forEachIndexed { index, transaction ->
                    ActivityComponentItem(component = transaction, onClick = onClick)

                    if (index < components.lastIndex) {
                        Divider(color = AppColors.background)
                    }
                }
            }
        }
    }
}
