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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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
import com.blockchain.componentlib.basic.MaskStateConfig
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Verified
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import kotlinx.coroutines.flow.Flow

@Stable
data class CryptoAndFiatBalance(
    val crypto: String,
    val fiat: String
)

@Stable
data class AsyncBalanceUi(
    val fetcher: Flow<CryptoAndFiatBalance>
)

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
        maskState = MaskStateConfig.Default,
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
    subtitle: String? = null,
    tag: String? = null,
    balance: AsyncBalanceUi,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    onClick: (() -> Unit)? = null
) {
    val accountBalance by balance.fetcher.collectAsStateLifecycleAware(CryptoAndFiatBalance(crypto = "", fiat = ""))

    BalanceFiatAndCryptoTableRow(
        maskState = MaskStateConfig.Override(maskEnabled = false),
        title = title,
        subtitle = subtitle,
        tag = tag,
        titleIcon = titleIcon,
        valueCrypto = accountBalance.crypto,
        valueFiat = accountBalance.fiat,
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
    subtitle: String? = null,
    tag: String? = null,
    valueCrypto: String,
    valueFiat: String,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    onClick: (() -> Unit)? = null
) {
    BalanceFiatAndCryptoTableRow(
        maskState = MaskStateConfig.Override(maskEnabled = false),
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
    maskState: MaskStateConfig,
    title: String,
    subtitle: String? = null,
    tag: String? = null,
    valueCrypto: String,
    titleIcon: ImageResource?,
    valueFiat: String,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
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
                            if (!tag.isNullOrEmpty()) {
                                AppTheme.dimensions.composeSmallestSpacing
                            } else if (!subtitle.isNullOrBlank()) {
                                AppTheme.dimensions.smallestSpacing
                            } else 0.dp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = AppTheme.typography.caption1,
                                color = AppTheme.colors.body
                            )
                        }

                        if (!subtitle.isNullOrBlank() && tag?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                        }

                        if (!tag.isNullOrEmpty()) {
                            DefaultTag(text = tag)
                        }
                    }
                }
                Spacer(modifier = Modifier.size(AppTheme.dimensions.composeSmallestSpacing))
                Spacer(modifier = Modifier.weight(1F))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    MaskableText(
                        maskState = maskState,
                        text = valueFiat,
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                    SmallestVerticalSpacer()
                    MaskableText(
                        maskState = maskState,
                        text = valueCrypto,
                        maxLines = 1,
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
