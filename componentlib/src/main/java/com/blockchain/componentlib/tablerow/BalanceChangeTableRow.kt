package com.blockchain.componentlib.tablerow

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Green600
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.data.DataResource
import kotlin.math.absoluteValue

@Composable
fun BalanceChangeTableRow(
    name: String,
    subtitle: String? = null,
    networkTag: String? = null,
    value: DataResource<String>,
    valueChange: DataResource<ValueChange>? = null,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit
) {

    TableRow(
        contentStart = contentStart,
        content = {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = name,
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )

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
                }

                Spacer(modifier = Modifier.weight(1F))

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
                            Text(
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
                                    text = "${valueChange.data.indicator} ${valueChange.data.value}%",
                                    style = AppTheme.typography.caption1,
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
        colors = listOf(Grey100, Color.White, Grey100),
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
    val color: Color

    data class Up(override val value: Double) : ValueChange {
        override val indicator: String = "↑"
        override val color: Color = Green600
    }

    data class Down(override val value: Double) : ValueChange {
        override val indicator: String = "↓"
        override val color: Color = Grey700
    }

    data class None(override val value: Double) : ValueChange {
        override val indicator: String = ""
        override val color: Color = Grey700
    }

    companion object {
        fun fromValue(value: Double): ValueChange {
            return when {
                value >= 0 -> Up(value)
                else -> Down(value.absoluteValue)
            }
        }
    }
}

@Preview
@Composable
fun PreviewBalanceChangeTableRow() {
    AppTheme {
        AppSurface {
            BalanceChangeTableRow(
                name = "Bitcoin",
                subtitle = "BTC",
                networkTag = "Bitcoin",
                value = DataResource.Data("$1,000.00"),
                contentStart = {
                    ImageResource.Local(
                        id = R.drawable.ic_blockchain,
                    )
                },
                valueChange = DataResource.Data(ValueChange.Up(1.88)),
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
fun PreviewBalanceChangeTableRow_Loading() {
    AppTheme {
        AppSurface {
            BalanceChangeTableRow(
                name = "Bitcoin",
                value = DataResource.Loading,
                contentStart = {
                    ImageResource.Local(
                        id = R.drawable.ic_blockchain,
                    )
                },
                valueChange = DataResource.Loading,
                onClick = {}
            )
        }
    }
}
