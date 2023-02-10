package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer

@Composable
fun NonCustodialAssetBalanceTableRow(
    title: String,
    subtitle: String = "",
    valueCrypto: String,
    valueFiat: String,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    onClick: () -> Unit
) {
    NonCustodialAssetBalanceTableRow(
        title = title,
        subtitle = subtitle,
        valueCrypto = valueCrypto,
        valueFiat = valueFiat,
        contentStart = {
            CustomStackedIcon(
                icon = icon,
                size = defaultIconSize
            )
        },
        onClick = onClick
    )
}

@Composable
private fun NonCustodialAssetBalanceTableRow(
    title: String,
    subtitle: String = "",
    valueCrypto: String,
    valueFiat: String,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    TableRow(
        contentStart = contentStart,
        onContentClicked = onClick,
        content = {
            SmallHorizontalSpacer()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    SimpleText(
                        text = title,
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    if (subtitle.isNotEmpty()) {
                        SmallestVerticalSpacer()
                        DefaultTag(text = subtitle)
                    }
                }

                Spacer(modifier = Modifier.weight(1F))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    SimpleText(
                        text = valueFiat,
                        gravity = ComposeGravities.End,
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title
                    )
                    SmallestVerticalSpacer()
                    SimpleText(
                        text = valueCrypto,
                        gravity = ComposeGravities.End,
                        style = ComposeTypographies.Paragraph1,
                        color = ComposeColors.Body
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun NonCustodialBalanceTableRowPreview() {
    AppTheme {
        NonCustodialAssetBalanceTableRow(
            title = "Bitcoin",
            valueCrypto = "1",
            valueFiat = "1232222",
            onClick = {}
        )
    }
}

@Preview
@Composable
fun NonCustodialL2EVMBalanceTableRowPreview() {
    AppTheme {
        NonCustodialAssetBalanceTableRow(
            title = "USDC",
            subtitle = "Polygon",
            valueCrypto = "1",
            valueFiat = "1232222",
            onClick = {}
        )
    }
}
