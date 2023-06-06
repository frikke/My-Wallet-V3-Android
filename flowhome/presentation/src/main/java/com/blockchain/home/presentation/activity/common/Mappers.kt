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
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIconSource
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityLocalIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.StackComponent

@Composable
fun ActivityIcon.toStackedIcon() = when (this) {
    is ActivityIcon.OverlappingPair -> StackedIcon.OverlappingPair(
        front = front.toImageResource(),
        back = back.toImageResource()
    )

    is ActivityIcon.SmallTag -> StackedIcon.SmallTag(
        main = main.toImageResource(),
        tag = tag.toImageResource()
    )

    is ActivityIcon.SingleIcon -> StackedIcon.SingleIcon(
        icon = icon.toImageResource()
    )

    ActivityIcon.None -> StackedIcon.None
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
private fun ActivityIconSource.toImageResource(): ImageResource {
    fun ActivityLocalIcon.toIconResource() = when (this) {
        ActivityLocalIcon.Buy -> Icons.Plus
        ActivityLocalIcon.Sell -> Icons.Minus
        ActivityLocalIcon.Send -> Icons.Send
        ActivityLocalIcon.Receive -> Icons.Receive
        ActivityLocalIcon.Reward -> Icons.Rewards
        ActivityLocalIcon.Swap -> Icons.Swap
    }

    return when (this) {
        is ActivityIconSource.Remote -> ImageResource.Remote(url)
        is ActivityIconSource.Local -> icon.toIconResource().withTint(AppTheme.colors.title)
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
