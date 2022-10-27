package com.blockchain.home.presentation.activity.list.composable

import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColor
import com.blockchain.home.presentation.activity.common.ActivityTextStyle
import com.blockchain.home.presentation.activity.common.ActivityTextTypography
import com.blockchain.home.presentation.activity.list.TransactionGroup

val DUMMY_DATA: DataResource<Map<TransactionGroup, List<ActivityComponent.StackView>>> = DataResource.Data(
    mapOf(
        TransactionGroup.Group("Pending") to listOf(
            ActivityComponent.StackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        "85% confirmed",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackView.Text(
                        value = "Sent Bitcoin",
                        style = ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        value = "RBF transaction",
                        style = ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Error)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = "-25.00",
                        style = ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                    ),
                    ActivityStackView.Text(
                        value = "0.00025 BTC",
                        style = ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            )
        ),
        TransactionGroup.Group("June") to listOf(
            ActivityComponent.StackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        "June 14",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        "Canceled",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted, true)
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted, true)
                    )
                )
            )
        ),
        TransactionGroup.Group("July") to listOf(
            ActivityComponent.StackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        "Declined",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted, true)
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted, true)
                    )
                )
            )
        )
    )
)
