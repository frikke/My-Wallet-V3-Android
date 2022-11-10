package com.blockchain.home.presentation.activity.list.composable

import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import java.util.Calendar

val DUMMY_DATA: DataResource<Map<TransactionGroup, List<ActivityComponent>>> = DataResource.Data(
    mapOf(
        TransactionGroup.Group.Pending to listOf(
            ActivityComponent.StackView(
                leadingImage = ActivityIcon.SmallTag("", ""),
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        "85% confirmed",
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                leadingImage = ActivityIcon.SmallTag("", ""),
                leading = listOf(
                    ActivityStackView.Text(
                        value = "Sent Bitcoin",
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Title
                        )
                    ),
                    ActivityStackView.Text(
                        value = "RBF transaction",
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Caption1,
                            ActivityTextColorState.Error
                        )
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = "-25.00",
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Muted
                        )
                    ),
                    ActivityStackView.Text(
                        value = "0.00025 BTC",
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Caption1,
                            ActivityTextColorState.Muted
                        )
                    )
                )
            )
        ),
        TransactionGroup.Group.Date(Calendar.getInstance().apply { set(Calendar.MONTH, 4) }) to listOf(
            ActivityComponent.StackView(
                leadingImage = ActivityIcon.SmallTag("", ""),
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        "June 14",
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                leadingImage = ActivityIcon.SmallTag("", ""),
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        "Canceled",
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Muted, true
                        )
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyleState(
                            ActivityTextTypographyState.Caption1,
                            ActivityTextColorState.Muted, true
                        )
                    )
                )
            )
        ),
        TransactionGroup.Group.Date(Calendar.getInstance().apply { set(Calendar.MONTH, 5) }) to listOf(
            ActivityComponent.StackView(
                leadingImage = ActivityIcon.SmallTag("", ""),
                leading = listOf(
                    ActivityStackView.Text(
                        "Sent Bitcoin",
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        "Declined",
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        "-10.00",
                        ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Muted, true
                        )
                    ),
                    ActivityStackView.Text(
                        "-0.00893208 ETH",
                        ActivityTextStyleState(
                            ActivityTextTypographyState.Caption1,
                            ActivityTextColorState.Muted, true
                        )
                    )
                )
            )
        )
    )
)
