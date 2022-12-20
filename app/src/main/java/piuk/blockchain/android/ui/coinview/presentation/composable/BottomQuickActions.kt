package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import piuk.blockchain.android.ui.coinview.presentation.CoinviewQuickActionState

@Composable
fun BottomQuickActions(
    data: DataResource<List<CoinviewQuickActionState>>,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    when (data) {
        DataResource.Loading -> BottomQuickActionLoading()
        is DataResource.Error -> Empty()
        is DataResource.Data -> {
            BottomQuickActionData(
                data = data,
                onQuickActionClick = onQuickActionClick
            )
        }

    }
}

@Composable
fun BottomQuickActionLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(
                        topStart = AppTheme.dimensions.borderRadiiMedium,
                        topEnd = AppTheme.dimensions.borderRadiiMedium
                    )
                )
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = "",
                state = ButtonState.Loading,
                onClick = {}
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            SecondaryButton(
                modifier = Modifier.weight(1F),
                text = "",
                state = ButtonState.Loading,
                onClick = {}
            )
        }
    }
}

@Composable
fun BottomQuickActionData(
    data: DataResource.Data<List<CoinviewQuickActionState>>,
    onQuickActionClick: (CoinviewQuickActionState) -> Unit
) {
    if (data.data.isEmpty()) {
        Empty()
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(
                        topStart = AppTheme.dimensions.borderRadiiMedium,
                        topEnd = AppTheme.dimensions.borderRadiiMedium
                    )
                )
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            data.data.forEachIndexed { index, action ->
                SecondaryButton(
                    modifier = Modifier.weight(1F),
                    text = action.name.value(),
                    icon = ImageResource.Local(
                        action.logo.value,
                        colorFilter = ColorFilter.tint(AppTheme.colors.background),
                        size = AppTheme.dimensions.standardSpacing
                    ),
                    onClick = { onQuickActionClick(action) }
                )

                if (index < data.data.lastIndex) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewBottomQuickActions_Loading() {
    BottomQuickActions(
        data = DataResource.Loading,
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewBottomQuickActions_Data_2() {
    BottomQuickActions(
        data = DataResource.Data(
            listOf(CoinviewQuickActionState.Send, CoinviewQuickActionState.Receive)
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewBottomQuickActions_Data_1() {
    BottomQuickActions(
        data = DataResource.Data(
            listOf(CoinviewQuickActionState.Send)
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewQuickActionsBottom_Data_0() {
    BottomQuickActions(
        data = DataResource.Data(
            listOf()
        ),
        onQuickActionClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewQuickActionsBottom_Data_Error() {
    BottomQuickActions(
        data = DataResource.Error(Exception()),
        onQuickActionClick = {}
    )
}
