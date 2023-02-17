package com.blockchain.componentlib.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.ShimmerValue
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource

@Composable
fun BalanceChangeSmallCard(
    name: String,
    price: DataResource<String>,
    valueChange: DataResource<ValueChange>,
    imageResource: ImageResource = ImageResource.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    backgroundColor: Color = AppTheme.colors.background,
    onClick: () -> Unit
) {
    BalanceChangeSmallCard(
        name = name,
        price = price,
        valueChange = valueChange,
        icon = if (imageResource is ImageResource.None) {
            StackedIcon.None
        } else {
            StackedIcon.SingleIcon(imageResource)
        },
        defaultIconSize = defaultIconSize,
        backgroundColor = backgroundColor,
        onClick = onClick
    )
}

@Composable
private fun BalanceChangeSmallCard(
    name: String,
    price: DataResource<String>,
    valueChange: DataResource<ValueChange>,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    backgroundColor: Color = AppTheme.colors.background,
    onClick: () -> Unit
) {
    Surface(
        shape = AppTheme.shapes.large,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .defaultMinSize(minWidth = 140.dp)
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomStackedIcon(
                    icon = icon,
                    size = defaultIconSize
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    text = name,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            when (valueChange) {
                DataResource.Loading -> {
                    ShimmerValue(
                        modifier = Modifier
                            .width(50.dp)
                            .height(18.dp)
                    )
                }

                is DataResource.Error -> {
                }

                is DataResource.Data -> {
                    Text(
                        text = "${valueChange.data.indicator} ${valueChange.data.value}%",
                        style = AppTheme.typography.body2,
                        color = valueChange.data.color
                    )
                }
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            when (price) {
                DataResource.Loading -> {
                    ShimmerValue(
                        modifier = Modifier
                            .width(70.dp)
                            .height(18.dp)
                    )
                }

                is DataResource.Error -> {
                }

                is DataResource.Data -> {
                    Text(
                        text = price.data,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewBalanceChangeSmallCard() {
    BalanceChangeSmallCard(
        name = "BTC",
        price = DataResource.Data("$100.000"),
        valueChange = DataResource.Data(ValueChange.Up(20.0)),
        icon = StackedIcon.SingleIcon(ImageResource.Local(R.drawable.logo_bitcoin)),
        onClick = {}
    )
}

@Preview
@Composable
fun PreviewBalanceChangeSmallCard_SmallTag() {
    BalanceChangeSmallCard(
        name = "BTC",
        price = DataResource.Data("$100.000"),
        valueChange = DataResource.Data(ValueChange.Down(20.0)),
        icon = StackedIcon.SmallTag(
            main = ImageResource.Local(R.drawable.logo_bitcoin),
            tag = ImageResource.Local(R.drawable.close_on)
        ),
        onClick = {}
    )
}
