package com.blockchain.home.presentation.activity.list.composable

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.blockchain.componentlib.R
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import org.koin.androidx.compose.defaultExtras
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import java.util.Calendar

val DUMMY_DATA: DataResource<Map<TransactionGroup, List<ActivityComponent>>> = DataResource.Data(
    mapOf(
        TransactionGroup.Group.Pending to listOf(
            ActivityComponent.StackView(
                id = "",
                leadingImage = ActivityIconState.SmallTag.Local(
                    main = R.drawable.ic_close_circle_dark,
                    tag = R.drawable.ic_close_circle
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        TextValue.StringValue("85% confirmed"),
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        TextValue.StringValue("-10.00"),
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        TextValue.StringValue("-0.00893208 ETH"),
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                id = "",
                leadingImage = ActivityIconState.SmallTag.Local(
                    main = R.drawable.ic_close_circle_dark,
                    tag = R.drawable.ic_close_circle
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Title
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("RBF transaction"),
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Caption1,
                            ActivityTextColorState.Error
                        )
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-25.00"),
                        style = ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Muted
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("0.00025 BTC"),
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
                id = "",
                leadingImage = ActivityIconState.SmallTag.Local(
                    main = R.drawable.ic_close_circle_dark,
                    tag = R.drawable.ic_close_circle
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("June 14"),
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-10.00"),
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-0.00893208 ETH"),
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Muted)
                    )
                )
            ),
            ActivityComponent.StackView(
                id = "",
                leadingImage = ActivityIconState.SmallTag.Local(
                    main = R.drawable.ic_close_circle_dark,
                    tag = R.drawable.ic_close_circle
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Canceled"),
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-10.00"),
                        ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Muted, true
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-0.00893208 ETH"),
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
                id = "",
                leadingImage = ActivityIconState.SmallTag.Local(
                    main = R.drawable.ic_close_circle_dark,
                    tag = R.drawable.ic_close_circle
                ),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Sent Bitcoin"),
                        ActivityTextStyleState(ActivityTextTypographyState.Paragraph2, ActivityTextColorState.Title)
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("Declined"),
                        ActivityTextStyleState(ActivityTextTypographyState.Caption1, ActivityTextColorState.Warning)
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-10.00"),
                        ActivityTextStyleState(
                            ActivityTextTypographyState.Paragraph2,
                            ActivityTextColorState.Muted, true
                        )
                    ),
                    ActivityStackView.Text(
                        value = TextValue.StringValue("-0.00893208 ETH"),
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
