package com.blockchain.home.presentation.activity.detail.composable

import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIconSource
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityLocalIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography

val DETAIL_DUMMY_DATA: DataResource<ActivityDetail> = DataResource.Data(
    ActivityDetail(
        icon = ActivityIcon.SmallTag(
            main = ActivityIconSource.Local(ActivityLocalIcon.Buy),
            tag = ActivityIconSource.Local(ActivityLocalIcon.Buy)
        ),
        title = TextValue.StringValue("Swapped BTC -> ETH"),
        subtitle = TextValue.StringValue("some subtitle"),
        detailItems = listOf(
            ActivityDetailGroup(
                title = "title",
                itemGroup = listOf<ActivityComponent>(
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Purchase"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Purchase"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Title
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Amount"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("20/35 confirmations"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Caption1,
                                    ActivityTextColor.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Network"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Caption1,
                                    ActivityTextColor.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Transaction ID"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Caption1,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("0.00503823 BTC"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Title
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Bitcoin Account"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Caption1,
                                    ActivityTextColor.Warning
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Fees"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Free"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Success
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Total"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("106.17"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Title
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("0.00534908 BTC"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Caption1,
                                    ActivityTextColor.Muted
                                )
                            )
                        )
                    )
                )
            ),
            ActivityDetailGroup(
                title = "iii",
                itemGroup = listOf<ActivityComponent>(
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Status"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Tag(
                                value = TextValue.StringValue("Complete"),
                                ActivityTagStyle.Success
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Bank"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Capital One •••• 0192"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Title
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Status"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("20/35 confirmations"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Caption1,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Tag(
                                value = TextValue.StringValue("Pending"),
                                ActivityTagStyle.Warning
                            )
                        )
                    )
                )
            ),
            ActivityDetailGroup(
                title = null,
                itemGroup = listOf<ActivityComponent>(
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Bank"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Capital One •••• 0192"),
                                ActivityTextStyle(
                                    ActivityTextTypography.Paragraph2,
                                    ActivityTextColor.Title
                                )
                            )
                        )
                    ),
                    ActivityComponent.Button(
                        id = "",
                        value = TextValue.StringValue("Copy Transaction ID"),
                        style = ActivityButtonStyle.Tertiary,
                        action = ActivityButtonAction(
                            type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                            data = ""
                        )
                    )
                )
            )

        ),
        floatingActions = listOf(
            ActivityComponent.Button(
                id = "",
                value = TextValue.StringValue("View on Etherscan"),
                style = ActivityButtonStyle.Primary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                    data = ""
                )

            ),
            ActivityComponent.Button(
                id = "",
                value = TextValue.StringValue("Speed Up"),
                style = ActivityButtonStyle.Secondary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                    data = ""
                )
            ),
            ActivityComponent.Button(
                id = "",
                value = TextValue.StringValue("Cancel"),
                style = ActivityButtonStyle.Tertiary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                    data = ""
                )
            )
        )
    )
)
