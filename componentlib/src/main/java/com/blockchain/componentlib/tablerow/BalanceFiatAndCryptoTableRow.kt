package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MaskedText
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Verified
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer

@Composable
fun MaskedBalanceFiatAndCryptoTableRow(
    title: String,
    titleIcon: ImageResource? = null,
    subtitle: String = "",
    tag: String = "",
    valueCrypto: String,
    valueFiat: String,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    onClick: () -> Unit
) {
    BalanceFiatAndCryptoTableRow(
        allowMaskedValue = true,
        title = title,
        subtitle = subtitle,
        tag = tag,
        titleIcon = titleIcon,
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
fun BalanceFiatAndCryptoTableRow(
    title: String,
    titleIcon: ImageResource? = null,
    subtitle: String = "",
    tag: String = "",
    valueCrypto: String,
    valueFiat: String,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    onClick: () -> Unit
) {
    BalanceFiatAndCryptoTableRow(
        allowMaskedValue = false,
        title = title,
        subtitle = subtitle,
        tag = tag,
        titleIcon = titleIcon,
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
private fun BalanceFiatAndCryptoTableRow(
    allowMaskedValue: Boolean,
    title: String,
    subtitle: String = "",
    tag: String = "",
    valueCrypto: String,
    titleIcon: ImageResource?,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SimpleText(
                            text = title,
                            style = ComposeTypographies.Paragraph2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                        titleIcon?.let {
                            Image(
                                modifier = Modifier.padding(start = AppTheme.dimensions.smallestSpacing),
                                imageResource = titleIcon
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.size(
                            if (tag.isNotEmpty()) {
                                AppTheme.dimensions.composeSmallestSpacing
                            } else if (subtitle.isNotEmpty()) {
                                AppTheme.dimensions.smallestSpacing
                            } else 0.dp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = AppTheme.typography.caption1,
                                color = AppTheme.colors.body
                            )
                        }

                        if (subtitle.isNotEmpty() && tag.isNotEmpty()) {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                        }

                        if (tag.isNotEmpty()) {
                            DefaultTag(text = tag)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1F))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    MaskedText(
                        allowMaskedValue = allowMaskedValue,
                        text = valueFiat,
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                    SmallestVerticalSpacer()
                    MaskedText(
                        allowMaskedValue = allowMaskedValue,
                        text = valueCrypto,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewBalanceFiatAndCryptoTableRow() {
    AppTheme {
        BalanceFiatAndCryptoTableRow(
            title = "Bitcoin",
            titleIcon = Icons.Verified,
            valueCrypto = "1",
            valueFiat = "1232222",
            onClick = {}
        )
    }
}

@Preview
@Composable
fun PreviewBalanceFiatAndCryptoTableRow_Subtitle() {
    AppTheme {
        BalanceFiatAndCryptoTableRow(
            title = "Bitcoin",
            subtitle = "BTC",
            valueCrypto = "1",
            valueFiat = "1232222",
            onClick = {}
        )
    }
}

@Preview
@Composable
fun PreviewBalanceFiatAndCryptoTableRow_SubtitleTag() {
    AppTheme {
        BalanceFiatAndCryptoTableRow(
            title = "USDC",
            subtitle = "BTC",
            tag = "Polygon",
            valueCrypto = "1",
            valueFiat = "1232222",
            onClick = {}
        )
    }
}
