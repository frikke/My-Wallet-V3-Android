package com.blockchain.home.presentation.activity.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.tablerow.custom.MaskedCustomTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.toStackedIcon
import com.blockchain.image.LogoValue
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle

/**
 * @property id Some components may want to be identified for later interaction
 */
sealed interface ActivityComponent {
    val id: String

    data class StackView(
        override val id: String,
        val leadingImage: LogoValue = LogoValue.None,
        val leadingImageDark: LogoValue = LogoValue.None,
        val leading: List<ActivityStackView>,
        val trailing: List<ActivityStackView>
    ) : ActivityComponent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StackView

            if (id != other.id) return false
            if (leading != other.leading) return false
            if (trailing != other.trailing) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + leading.hashCode()
            result = 31 * result + trailing.hashCode()
            return result
        }
    }

    data class Button(
        override val id: String,
        val value: TextValue,
        val style: ActivityButtonStyle,
        val action: ActivityButtonAction
    ) : ActivityComponent
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
            MaskedCustomTableRow(
                ellipsiseLeading = true,
                icon = (if (isSystemInDarkTheme()) component.leadingImageDark else component.leadingImage)
                    .toStackedIcon(),
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
