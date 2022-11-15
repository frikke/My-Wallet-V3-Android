package com.blockchain.home.presentation.activity.detail.composable

import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityButtonStyleState
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTagStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon

val DETAIL_DUMMY_DATA: DataResource<ActivityDetail> = DataResource.Data(
    ActivityDetail(
        icon = ActivityIcon.None,
        title = "Swapped BTC -> ETH",
        subtitle = "",
        itemGroups = listOf(
            listOf<ActivityComponent>(
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Purchase",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "-10.00",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Amount",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        ),
                        ActivityStackView.Text(
                            "20/35 confirmations",
                            ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                        ),
                        ActivityStackView.Text(
                            "Network",
                            ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                        ),
                        ActivityStackView.Text(
                            "Transaction ID",
                            ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "0.00503823 BTC",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                        ),
                        ActivityStackView.Text(
                            "Bitcoin Account",
                            ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Warning)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Fees",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "Free",
                            ActivityTextStyleState(
                                ActivityTextTypographyState.Paragraph2,
                                ActivityTextColorState.Success
                            )
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Total",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "106.17",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                        ),
                        ActivityStackView.Text(
                            "0.00534908 BTC",
                            ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                        )
                    )
                )
            ),
            listOf<ActivityComponent>(
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Status",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Tag(
                            "Complete",
                            ActivityTagStyleState.Success
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Bank",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "Capital One •••• 0192",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                        )
                    )
                ),
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Status",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        ),
                        ActivityStackView.Text(
                            "20/35 confirmations",
                            ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Tag(
                            "Pending",
                            ActivityTagStyleState.Warning
                        )
                    )
                )
            ),
            listOf<ActivityComponent>(
                ActivityComponent.StackView(
                    leading = listOf(
                        ActivityStackView.Text(
                            "Bank",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Muted)
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            "Capital One •••• 0192",
                            ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                        )
                    )
                ),
                ActivityComponent.Button(
                    "Copy Transaction ID",
                    ActivityButtonStyleState.Tertiary
                )
            )
        ),
        floatingActions = listOf(
            ActivityComponent.Button(
                "View on Etherscan",
                ActivityButtonStyleState.Primary
            ),
            ActivityComponent.Button(
                "Speed Up",
                ActivityButtonStyleState.Secondary
            ),
            ActivityComponent.Button(
                "Cancel",
                ActivityButtonStyleState.Tertiary
            )
        )
    )
)
