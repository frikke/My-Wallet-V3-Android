package com.blockchain.home.presentation.activity.detail.composable

import com.blockchain.componentlib.R
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityButtonStyleState
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTagStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction

val DETAIL_DUMMY_DATA: DataResource<ActivityDetail> = DataResource.Data(
    ActivityDetail(
        icon = ActivityIconState.SmallTag.Local(
            main = R.drawable.ic_close_circle_dark,
            tag = R.drawable.ic_close_circle
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
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Purchase"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Amount"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("20/35 confirmations"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Network"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Transaction ID"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("0.00503823 BTC"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Bitcoin Account"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Caption1, ActivityTextColorState.Warning
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Fees"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Free"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2,
                                    ActivityTextColorState.Success
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Total"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("106.17"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("0.00534908 BTC"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted
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
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Tag(
                                value = TextValue.StringValue("Complete"),
                                ActivityTagStyleState.Success
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Bank"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Capital One •••• 0192"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title
                                )
                            )
                        )
                    ),
                    ActivityComponent.StackView(
                        id = "",
                        leading = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Status"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            ),
                            ActivityStackView.Text(
                                value = TextValue.StringValue("20/35 confirmations"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Tag(
                                value = TextValue.StringValue("Pending"),
                                ActivityTagStyleState.Warning
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
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted
                                )
                            )
                        ),
                        trailing = listOf(
                            ActivityStackView.Text(
                                value = TextValue.StringValue("Capital One •••• 0192"),
                                ActivityTextStyleState(
                                    ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title
                                )
                            )
                        )
                    ),
                    ActivityComponent.Button(
                        id = "",
                        value = TextValue.StringValue("Copy Transaction ID"),
                        style = ActivityButtonStyleState.Tertiary,
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
                style = ActivityButtonStyleState.Primary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                    data = ""
                )

            ),
            ActivityComponent.Button(
                id = "",
                value = TextValue.StringValue("Speed Up"),
                style = ActivityButtonStyleState.Secondary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                    data = ""
                )
            ),
            ActivityComponent.Button(
                id = "",
                value = TextValue.StringValue("Cancel"),
                style = ActivityButtonStyleState.Tertiary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.OpenUrl,
                    data = ""
                )
            )
        )
    )
)
