package com.blockchain.home.presentation.activity.common

import com.blockchain.componentlib.utils.TextValue
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.StackComponent

fun ActivityIcon.toStackedIcon() = when (this) {
    is ActivityIcon.OverlappingPair -> ActivityIconState.OverlappingPair.Remote(
        front = front,
        back = back
    )
    is ActivityIcon.SmallTag -> ActivityIconState.SmallTag.Remote(
        main = main,
        tag = tag
    )
    is ActivityIcon.SingleIcon -> ActivityIconState.SingleIcon.Remote(
        url = url
    )
    ActivityIcon.None -> ActivityIconState.None
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

/**
 * @param componentId some components may want to be identified for later interaction
 */
fun ActivityDataItem.toActivityComponent(componentId: String = this.toString()) = when (this) {
    is ActivityDataItem.Stack -> ActivityComponent.StackView(
        id = componentId,
        leadingImage = leadingImage.toStackedIcon(),
        leading = leading.map { it.toStackView() },
        trailing = trailing.map { it.toStackView() },
    )

    is ActivityDataItem.Button -> ActivityComponent.Button(
        id = componentId,
        value = TextValue.StringValue(value),
        style = style,
        action = action
    )
}
