package com.blockchain.home.presentation.activity.detail.composable

import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityButtonStyle
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTagStyle
import com.blockchain.home.presentation.activity.common.ActivityTextColor
import com.blockchain.home.presentation.activity.common.ActivityTextStyle
import com.blockchain.home.presentation.activity.common.ActivityTextTypography
import com.blockchain.home.presentation.activity.detail.ActivityDetail

val DETAIL_DUMMY_DATA: DataResource<ActivityDetail> = DataResource.Data(
    ActivityDetail(
        itemGroups = listOf(
            listOf<ActivityComponent>(
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Purchase",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "-10.00",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Amount",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        ),
                        ActivityStackView.Text(
                            "20/35 confirmations",
                            ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                        ),
                        ActivityStackView.Text(
                            "Network",
                            ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                        ),
                        ActivityStackView.Text(
                            "Transaction ID",
                            ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "0.00503823 BTC",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                        ),
                        ActivityStackView.Text(
                            "Bitcoin Account",
                            ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Fees",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "Free",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Success)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Total",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "106.17",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                        ),
                        ActivityStackView.Text(
                            "0.00534908 BTC",
                            ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                        )
                    )
                )
            ),
            listOf<ActivityComponent>(
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Status",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Tag(
                            "Complete",
                            ActivityTagStyle.Success
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Bank",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "Capital One •••• 0192",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Status",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        ),
                        ActivityStackView.Text(
                            "20/35 confirmations",
                            ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Tag(
                            "Pending",
                            ActivityTagStyle.Warning
                        )
                    )
                )
            ),
            listOf<ActivityComponent>(
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Bank",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "Capital One •••• 0192",
                            ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                        )
                    )
                ),
                ActivityComponent.Button(
                    "Copy Transaction ID",
                    ActivityButtonStyle.Tertiary
                )
            )
        ),
        floatingActions = listOf(
            ActivityComponent.Button(
                "View on Etherscan",
                ActivityButtonStyle.Primary
            ),
            ActivityComponent.Button(
                "Speed Up",
                ActivityButtonStyle.Secondary
            ),
            ActivityComponent.Button(
                "Cancel",
                ActivityButtonStyle.Tertiary
            )
        )
    )
)
