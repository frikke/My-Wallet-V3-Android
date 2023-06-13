package com.blockchain.componentlib.tablerow

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MaskStateConfig
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Fire
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import kotlin.math.absoluteValue

// todo(othman) support logo small tag
@Stable
data class BalanceChange(
    val name: String,
    val ticker: String,
    val network: String?,
    val logo: String,
    val nativeAssetLogo: String?,
    val delta: DataResource<ValueChange>,
    val currentPrice: DataResource<String>,
    val showRisingFastTag: Boolean
)

// with data: BalanceChange, spread into multiple params
@Composable
fun BalanceChangeTableRow(
    modifier: Modifier = Modifier,
    data: BalanceChange,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    BalanceChangeTableRow(
        modifier = modifier,
        name = data.name,
        subtitle = data.ticker,
        networkTag = data.network,
        value = data.currentPrice,
        valueChange = data.delta,
        showRisingFastTag = data.showRisingFastTag,
        icon = if (data.nativeAssetLogo != null) {
            StackedIcon.SmallTag(
                main = ImageResource.Remote(data.logo),
                tag = ImageResource.Remote(data.nativeAssetLogo),
            )
        } else {
            StackedIcon.SingleIcon(ImageResource.Remote(data.logo))
        },
        defaultIconSize = defaultIconSize,
        withChevron = withChevron,
        onClick = onClick
    )
}

// maskable with imageResource
@Composable
fun MaskableBalanceChangeTableRow(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    imageResource: ImageResource = ImageResource.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    BalanceChangeTableRow(
        modifier = modifier,
        maskState = MaskStateConfig.Default,
        name = name,
        subtitle = subtitle,
        networkTag = networkTag,
        value = value,
        valueChange = valueChange,
        showRisingFastTag = showRisingFastTag,
        imageResource = imageResource,
        defaultIconSize = defaultIconSize,
        withChevron = withChevron,
        onClick = onClick
    )
}

// normal wth ImageResource
@Composable
fun BalanceChangeTableRow(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    imageResource: ImageResource = ImageResource.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    BalanceChangeTableRow(
        modifier = modifier,
        maskState = MaskStateConfig.Override(maskEnabled = false),
        name = name,
        subtitle = subtitle,
        networkTag = networkTag,
        value = value,
        valueChange = valueChange,
        showRisingFastTag = showRisingFastTag,
        imageResource = imageResource,
        defaultIconSize = defaultIconSize,
        withChevron = withChevron,
        onClick = onClick
    )
}

// ImageResource to StackedIcon
// with maskState
@Composable
private fun BalanceChangeTableRow(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    imageResource: ImageResource = ImageResource.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    BalanceChangeTableRow(
        modifier = modifier,
        maskState = maskState,
        name = name,
        subtitle = subtitle,
        networkTag = networkTag,
        value = value,
        valueChange = valueChange,
        showRisingFastTag = showRisingFastTag,
        icon = if (imageResource is ImageResource.None) {
            StackedIcon.None
        } else {
            StackedIcon.SingleIcon(imageResource)
        },
        defaultIconSize = defaultIconSize,
        withChevron = withChevron,
        onClick = onClick
    )
}

@Composable
fun MaskedBalanceChangeTableRow(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    BalanceChangeTableRow(
        modifier = modifier,
        maskState = MaskStateConfig.Default,
        name = name,
        subtitle = subtitle,
        networkTag = networkTag,
        value = value,
        valueChange = valueChange,
        showRisingFastTag = showRisingFastTag,
        icon = icon,
        defaultIconSize = defaultIconSize,
        withChevron = withChevron,
        onClick = onClick
    )
}

@Composable
fun BalanceChangeTableRow(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    BalanceChangeTableRow(
        modifier = modifier,
        maskState = MaskStateConfig.Override(maskEnabled = false),
        name = name,
        subtitle = subtitle,
        networkTag = networkTag,
        value = value,
        valueChange = valueChange,
        showRisingFastTag = showRisingFastTag,
        icon = icon,
        defaultIconSize = defaultIconSize,
        withChevron = withChevron,
        onClick = onClick
    )
}

// with allowMaskedValue and StackedIcon
@Composable
private fun BalanceChangeTableRow(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    withChevron: Boolean = false,
    onClick: () -> Unit
) {
    if (withChevron) {
        BalanceChangeTableRowWithChevron(
            modifier = modifier,
            maskState = maskState,
            name = name,
            subtitle = subtitle,
            networkTag = networkTag,
            value = value,
            valueChange = valueChange,
            showRisingFastTag = showRisingFastTag,
            contentStart = {
                CustomStackedIcon(
                    icon = icon,
                    size = defaultIconSize
                )
            },
            onClick = onClick
        )
    } else {
        BalanceChangeTableRow(
            modifier = modifier,
            maskState = maskState,
            name = name,
            subtitle = subtitle,
            networkTag = networkTag,
            value = value,
            valueChange = valueChange,
            showRisingFastTag = showRisingFastTag,
            contentStart = {
                CustomStackedIcon(
                    icon = icon,
                    size = defaultIconSize
                )
            },
            onClick = onClick
        )
    }
}

@Composable
private fun BalanceChangeTableRow(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    TableRow(
        modifier = modifier,
        contentStart = contentStart,
        content = {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(weight = 1F, fill = true)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = AppTheme.typography.paragraph2,
                            color = AppTheme.colors.title
                        )

                        if (showRisingFastTag) {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                            Image(
                                imageResource = Icons.Filled.Fire
                                    .withSize(14.dp)
                                    .withTint(AppTheme.colors.warningMuted)
                            )
                        }
                    }

                    subtitle?.let {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subtitle,
                                style = AppTheme.typography.paragraph1,
                                color = AppTheme.colors.body
                            )

                            networkTag?.let {
                                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                                DefaultTag(text = networkTag)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    when (value) {
                        DataResource.Loading -> {
                            ShimmerValue(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(18.dp)
                            )
                        }

                        is DataResource.Error -> {
                            // todo
                        }

                        is DataResource.Data -> {
                            MaskableText(
                                maskState = maskState,
                                text = value.data,
                                style = AppTheme.typography.paragraph2,
                                color = AppTheme.colors.title
                            )
                        }
                    }

                    valueChange?.let {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                        when (valueChange) {
                            DataResource.Loading -> {
                                ShimmerValue(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(18.dp)
                                )
                            }

                            is DataResource.Error -> {
                                // todo
                            }

                            is DataResource.Data -> {
                                Text(
                                    text = valueChange.data.formattedText,
                                    style = AppTheme.typography.caption1,
                                    textAlign = TextAlign.End,
                                    color = valueChange.data.color
                                )
                            }
                        }
                    }
                }
            }
        },
        onContentClicked = onClick
    )
}

@Composable
private fun BalanceChangeTableRowWithChevron(
    modifier: Modifier = Modifier,
    maskState: MaskStateConfig,
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    showRisingFastTag: Boolean = false,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    TableRow(
        modifier = modifier,
        contentStart = contentStart,
        content = {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.title
                    )

                    if (showRisingFastTag) {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                        Image(
                            imageResource = Icons.Filled.Fire
                                .withSize(14.dp)
                                .withTint(AppTheme.colors.warningMuted)
                        )
                    }
                }

                // sub and network tag row
                subtitle?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subtitle,
                            style = AppTheme.typography.paragraph1,
                            color = AppTheme.colors.muted
                        )

                        networkTag?.let {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                            // todo(othman) tags superapp styling
                            DefaultTag(text = networkTag)
                        }
                    }
                }

                // price and change row
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (value) {
                        DataResource.Loading -> {
                            ShimmerValue(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(18.dp)
                            )
                        }

                        is DataResource.Error -> {
                            // todo
                        }

                        is DataResource.Data -> {
                            MaskableText(
                                maskState = maskState,
                                text = value.data,
                                style = AppTheme.typography.paragraph1,
                                color = AppTheme.colors.title,
                            )
                        }
                    }

                    valueChange?.let {
                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                        when (valueChange) {
                            DataResource.Loading -> {
                                ShimmerValue(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(18.dp)
                                )
                            }

                            is DataResource.Error -> {
                                // todo
                            }

                            is DataResource.Data -> {
                                Text(
                                    text = "${valueChange.data.value}%",
                                    style = AppTheme.typography.paragraph1,
                                    textAlign = TextAlign.End,
                                    color = valueChange.data.color
                                )
                            }
                        }
                    }
                }
            }
        },
        contentEnd = {
            Image(imageResource = Icons.ChevronRight.withTint(AppTheme.colors.title))
        },
        onContentClicked = onClick
    )
}

@Composable
fun ShimmerValue(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = listOf(AppTheme.colors.light, AppTheme.colors.backgroundSecondary, AppTheme.colors.light),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    Spacer(
        modifier = modifier.background(brush = brush)
    )
}

sealed interface ValueChange {
    val value: Double
    val indicator: String

    @get:Composable
    val color: Color

    data class Up(override val value: Double) : ValueChange {
        override val indicator: String = "↑"
        override val color: Color
            @Composable get() = AppTheme.colors.success
    }

    data class Down(override val value: Double) : ValueChange {
        override val indicator: String = "↓"
        override val color: Color
            @Composable get() = AppTheme.colors.negative
    }

    data class None(override val value: Double) : ValueChange {
        override val indicator: String = "→"
        override val color: Color
            @Composable get() = AppTheme.colors.body
    }

    companion object {
        fun fromValue(value: Double): ValueChange {
            return when {
                value > 0 -> Up(value)
                value < 0 -> Down(value.absoluteValue)
                else -> None(value)
            }
        }
    }
}

val ValueChange.signedValue: Double
    get() = value.absoluteValue.run {
        when (this@signedValue) {
            is ValueChange.None,
            is ValueChange.Up -> unaryPlus()

            is ValueChange.Down -> unaryMinus()
        }
    }

val ValueChange.formattedText: String
    get() = "$indicator $value%"

@Preview
@Composable
private fun PreviewBalanceChangeTableRow() {
    BalanceChangeTableRow(
        name = "Bitcoin",
        subtitle = "BTC",
        networkTag = "Bitcoin",
        value = DataResource.Data("$1,000.00"),
        showRisingFastTag = true,
        imageResource = ImageResource.Local(R.drawable.ic_blockchain),
        valueChange = DataResource.Data(ValueChange.Up(1.88)),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBalanceChangeTableRowDark() {
    PreviewBalanceChangeTableRow()
}

@Preview
@Composable
fun PreviewBalanceChangeTableRow_Chevron() {
    BalanceChangeTableRow(
        name = "Bitcoin",
        value = DataResource.Data("$1,000.00"),
        imageResource = ImageResource.Local(R.drawable.ic_blockchain),
        valueChange = DataResource.Data(ValueChange.Up(1.88)),
        showRisingFastTag = true,
        withChevron = true,
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBalanceChangeTableRow_ChevronDark() {
    PreviewBalanceChangeTableRow_Chevron()
}

@Preview
@Composable
fun PreviewBalanceChangeTableRow_Loading() {
    BalanceChangeTableRow(
        name = "Bitcoin",
        value = DataResource.Loading,
        imageResource = ImageResource.Local(R.drawable.ic_blockchain),
        valueChange = DataResource.Loading,
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBalanceChangeTableRow_LoadingDark() {
    PreviewBalanceChangeTableRow_Loading()
}
