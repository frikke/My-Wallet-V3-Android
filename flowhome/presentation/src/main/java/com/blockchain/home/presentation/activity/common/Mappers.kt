package com.blockchain.home.presentation.activity.common

import com.blockchain.componentlib.utils.TextValue
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography
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

fun ActivityTextTypography.toTextTypography() = when (this) {
    ActivityTextTypography.Paragraph2 -> ActivityTextTypographyState.Paragraph2
    ActivityTextTypography.Caption1 -> ActivityTextTypographyState.Caption1
}

fun ActivityTextColor.toTextColor() = when (this) {
    ActivityTextColor.Title -> ActivityTextColorState.Title
    ActivityTextColor.Muted -> ActivityTextColorState.Muted
    ActivityTextColor.Success -> ActivityTextColorState.Success
    ActivityTextColor.Error -> ActivityTextColorState.Error
    ActivityTextColor.Warning -> ActivityTextColorState.Warning
}

fun ActivityTextStyle.toTextStyle() = ActivityTextStyleState(
    typography = typography.toTextTypography(),
    color = color.toTextColor(),
    strikethrough = strikethrough
)

fun ActivityTagStyle.toTagStyle() = when (this) {
    ActivityTagStyle.Success -> ActivityTagStyleState.Success
    ActivityTagStyle.Warning -> ActivityTagStyleState.Warning
}

fun ActivityButtonStyle.toButtonStyle() = when (this) {
    ActivityButtonStyle.Primary -> ActivityButtonStyleState.Primary
    ActivityButtonStyle.Secondary -> ActivityButtonStyleState.Secondary
    ActivityButtonStyle.Tertiary -> ActivityButtonStyleState.Tertiary
}

fun StackComponent.toStackView() = when (this) {
    is StackComponent.Text -> ActivityStackView.Text(
        value = TextValue.StringValue(value),
        style = style.toTextStyle()
    )

    is StackComponent.Tag -> ActivityStackView.Tag(
        value = TextValue.StringValue(value),
        style = style.toTagStyle()
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
        style = style.toButtonStyle(),
        action = action
    )
}
