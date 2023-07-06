package com.blockchain.home.presentation.activity.common

import androidx.compose.runtime.Composable
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Rewards
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.image.LocalLogo
import com.blockchain.image.LogoValue
import com.blockchain.image.LogoValueSource
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.StackComponent

@Composable
fun LogoValue.toStackedIcon() = when (this) {
    is LogoValue.OverlappingPair -> StackedIcon.OverlappingPair(
        front = front.toImageResource(),
        back = back.toImageResource()
    )

    is LogoValue.SmallTag -> StackedIcon.SmallTag(
        main = main.toImageResource(),
        tag = tag.toImageResource()
    )

    is LogoValue.SingleIcon -> StackedIcon.SingleIcon(
        icon = icon.toImageResource()
    )

    LogoValue.None -> StackedIcon.None
}

fun StackComponent.toStackView() = when (this) {
    is StackComponent.Text -> ActivityStackView.Text(
        value = TextValue.StringValue(value),
        style = style
    )

    is StackComponent.Tag -> ActivityStackView.Tag(
        value = TextValue.StringValue(value),
        style = style
    )
}

@Composable
private fun LogoValueSource.toImageResource(): ImageResource {
    fun LocalLogo.toIconResource() = when (this) {
        LocalLogo.Buy -> Icons.Plus
        LocalLogo.Sell -> Icons.Minus
        LocalLogo.Send -> Icons.Send
        LocalLogo.Receive -> Icons.Receive
        LocalLogo.Rewards -> Icons.Rewards
        LocalLogo.Swap -> Icons.Swap
    }

    return when (this) {
        is LogoValueSource.Remote -> ImageResource.Remote(url)
        is LogoValueSource.Local -> icon.toIconResource().withTint(AppTheme.colors.title)
    }
}

/**
 * @param componentId some components may want to be identified for later interaction
 */
fun ActivityDataItem.toActivityComponent(componentId: String = this.toString()) = when (this) {
    is ActivityDataItem.Stack -> ActivityComponent.StackView(
        id = componentId,
        leadingImage = leadingImage,
        leading = leading.map { it.toStackView() },
        trailing = trailing.map { it.toStackView() }
    )

    is ActivityDataItem.Button -> ActivityComponent.Button(
        id = componentId,
        value = TextValue.StringValue(value),
        style = style,
        action = action
    )
}
