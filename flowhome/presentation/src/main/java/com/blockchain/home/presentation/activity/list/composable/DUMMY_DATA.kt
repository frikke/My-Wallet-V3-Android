package com.blockchain.home.presentation.activity.list.composable

import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.image.LocalLogo
import com.blockchain.image.LogoValue
import com.blockchain.image.LogoValueSource
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography
import java.util.Calendar

val DUMMY_DATA: DataResource<Map<TransactionGroup, List<ActivityComponent>>> = DataResource.Data(
    mapOf(
        TransactionGroup.Group.Pending to listOf(
            ActivityComponent.StackView(
                id = "",
                leadingImage = LogoValue.SmallTag(
                    main = LogoValueSource.Local(LocalLogo.Buy),
                    tag = LogoValueSource.Local(LocalLogo.Buy)
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        TextValue.StringValue("85% confirmed"),
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        TextValue.StringValue("-10.00"),
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        TextValue.StringValue("-0.00893208 ETH"),
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                id = "",
                leadingImage = LogoValue.SmallTag(
                    main = LogoValueSource.Local(LocalLogo.Buy),
                    tag = LogoValueSource.Local(LocalLogo.Buy)
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        style = ActivityTextStyle(
                            ActivityTextTypography.Paragraph2,
                            ActivityTextColor.Title
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("RBF transaction"),
                        style = ActivityTextStyle(
                            ActivityTextTypography.Caption1,
                            ActivityTextColor.Error
                        )
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-25.00"),
                        style = ActivityTextStyle(
                            ActivityTextTypography.Paragraph2,
                            ActivityTextColor.Muted
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("0.00025 BTC"),
                        style = ActivityTextStyle(
                            ActivityTextTypography.Caption1,
                            ActivityTextColor.Muted
                        )
                    )
                )
            )
        ),
        TransactionGroup.Group.Date(Calendar.getInstance().apply { set(Calendar.MONTH, 4) }) to listOf(
            ActivityComponent.StackView(
                id = "",
                leadingImage = LogoValue.SmallTag(
                    main = LogoValueSource.Local(LocalLogo.Buy),
                    tag = LogoValueSource.Local(LocalLogo.Buy)
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("June 14"),
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-10.00"),
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-0.00893208 ETH"),
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                id = "",
                leadingImage = LogoValue.SmallTag(
                    main = LogoValueSource.Local(LocalLogo.Buy),
                    tag = LogoValueSource.Local(LocalLogo.Buy)
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Canceled"),
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-10.00"),
                        ActivityTextStyle(
                            ActivityTextTypography.Paragraph2,
                            ActivityTextColor.Muted,
                            true
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-0.00893208 ETH"),
                        ActivityTextStyle(
                            ActivityTextTypography.Caption1,
                            ActivityTextColor.Muted,
                            true
                        )
                    )
                )
            )
        ),
        TransactionGroup.Group.Date(Calendar.getInstance().apply { set(Calendar.MONTH, 5) }) to listOf(
            ActivityComponent.StackView(
                id = "",
                leadingImage = LogoValue.SmallTag(
                    main = LogoValueSource.Local(LocalLogo.Buy),
                    tag = LogoValueSource.Local(LocalLogo.Buy)
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyle(ActivityTextTypography.Paragraph2, ActivityTextColor.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Declined"),
                        ActivityTextStyle(ActivityTextTypography.Caption1, ActivityTextColor.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-10.00"),
                        ActivityTextStyle(
                            ActivityTextTypography.Paragraph2,
                            ActivityTextColor.Muted,
                            true
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-0.00893208 ETH"),
                        ActivityTextStyle(
                            ActivityTextTypography.Caption1,
                            ActivityTextColor.Muted,
                            true
                        )
                    )
                )
            )
        )
    )
)
