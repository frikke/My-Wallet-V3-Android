package com.blockchain.home.presentation.activity.common

import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography
import com.blockchain.unifiedcryptowallet.domain.activity.model.StackComponent

fun ActivityIcon.toStackedIcon() = when (this) {
    is ActivityIcon.OverlappingPair -> StackedIcon.OverlappingPair(
        front = ImageResource.Remote(front),
        back = ImageResource.Remote(back)
    )
    is ActivityIcon.SmallTag -> StackedIcon.SmallTag(
        main = ImageResource.Remote(main),
        tag = ImageResource.Remote(tag)
    )
    is ActivityIcon.SingleIcon -> StackedIcon.SingleIcon(
        icon = ImageResource.Remote(url)
    )
    ActivityIcon.None -> StackedIcon.None
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
        value = value,
        style = style.toTextStyle()
    )

    is StackComponent.Tag -> ActivityStackView.Tag(
        value = value,
        style = style.toTagStyle()
    )
}

fun ActivityDataItem.toActivityComponent() = when (this) {
    is ActivityDataItem.Stack -> ActivityComponent.StackView(
        leadingImage = leadingImage,
        leading = leading.map { it.toStackView() },
        trailing = trailing.map { it.toStackView() },
    )

    is ActivityDataItem.Button -> ActivityComponent.Button(
        value = value,
        style = style.toButtonStyle()
    )
}
