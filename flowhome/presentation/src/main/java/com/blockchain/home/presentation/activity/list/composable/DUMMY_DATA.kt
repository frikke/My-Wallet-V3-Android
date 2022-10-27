package com.blockchain.home.presentation.activity.list.composable

import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.components.ActivityStackView
import com.blockchain.home.presentation.activity.components.ActivityStackViewComponent
import com.blockchain.home.presentation.activity.components.ActivityTextColor
import com.blockchain.home.presentation.activity.components.ActivityTextStyle
import com.blockchain.home.presentation.activity.components.ActivityTextTypography
import com.blockchain.home.presentation.activity.list.TransactionGroup

val DUMMY_DATA: DataResource<Map<TransactionGroup, List<ActivityStackView>>> = DataResource.Data(
    mapOf(
        TransactionGroup.Group("Pending") to listOf(
            ActivityStackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackViewComponent.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        "85% confirmed",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackViewComponent.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            ),
            ActivityStackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackViewComponent.Text(
                        value = "Sent Bitcoin",
                        style = ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        value = "RBF transaction",
                        style = ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Error)
                    )
                ),
                trailing = listOf(
                    ActivityStackViewComponent.Text(
                        value = "-25.00",
                        style = ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                    ),
                    ActivityStackViewComponent.Text(
                        value = "0.00025 BTC",
                        style = ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            )
        ),
        TransactionGroup.Group("June") to listOf(
            ActivityStackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackViewComponent.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        "June 14",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackViewComponent.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            ),
            ActivityStackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackViewComponent.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        "Canceled",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackViewComponent.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted, true)
                    ),
                    ActivityStackViewComponent.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted, true)
                    )
                )
            )
        ),
        TransactionGroup.Group("July") to listOf(
            ActivityStackView(
                leadingImagePrimaryUrl = "transactionTypeIcon",
                leadingImageImageSecondaryUrl = "transactionCoinIcon",
                leading = listOf(
                    ActivityStackViewComponent.Text(
                        "Sent Bitcoin",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackViewComponent.Text(
                        "Declined",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackViewComponent.Text(
                        "-10.00",
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted, true)
                    ),
                    ActivityStackViewComponent.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted, true)
                    )
                )
            )
        )
    )
)
