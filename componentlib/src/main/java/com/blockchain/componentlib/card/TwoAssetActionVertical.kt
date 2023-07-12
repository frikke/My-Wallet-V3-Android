package com.blockchain.componentlib.card

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ArrowDown
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.permissions.RuntimePermission.Notification.icon
import com.blockchain.componentlib.permissions.RuntimePermission.Notification.title
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.utils.conditional
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.persistentListOf

@Stable
data class TwoAssetActionAsset(
    val title: String,
    val subtitle: String,
    val endTitle: String,
    val endSubtitle: String,
    val icon: StackedIcon,
)

@Stable
data class TwoAssetActionExtra(
    val title: String,
    val endTitle: String,
    val endSubtitle: String? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun TwoAssetAction(
    topAsset: TwoAssetActionAsset,
    topExtras: ImmutableList<TwoAssetActionExtra> = persistentListOf(),
    bottomAsset: TwoAssetActionAsset,
    bottomExtras: ImmutableList<TwoAssetActionExtra> = persistentListOf()
) {
    ConstraintLayout {
        val (topSection, bottomSection, spacer, arrow) = createRefs()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(topSection) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = AppTheme.shapes.large,
            color = AppTheme.colors.backgroundSecondary
        ) {
            Column {
                Asset(topAsset)

                topExtras.forEach { extra ->
                    AppDivider()
                    Extra(extra = extra)
                }
            }

        }

        Spacer(
            modifier = Modifier
                .size(AppTheme.dimensions.tinySpacing)
                .constrainAs(spacer) {
                    top.linkTo(topSection.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(bottomSection) {
                    top.linkTo(spacer.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                },
            shape = AppTheme.shapes.large,
            color = AppTheme.colors.backgroundSecondary
        ) {
            Column {
                Asset(bottomAsset)
                bottomExtras.forEach { extra ->
                    AppDivider()
                    Extra(extra = extra)
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(AppTheme.dimensions.hugeSpacing)
                .constrainAs(arrow) {
                    top.linkTo(spacer.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(spacer.bottom)
                },
            shape = CircleShape,
            color = AppColors.background,
            border = BorderStroke(AppTheme.dimensions.tinySpacing, AppTheme.colors.background)
        ) {
            Image(
                imageResource = Icons.ArrowDown.withBackground(
                    backgroundColor = AppColors.backgroundSecondary,
                    backgroundSize = AppTheme.dimensions.standardSpacing,
                    iconSize = AppTheme.dimensions.standardSpacing
                )
            )
        }
    }
}

@Composable
private fun Asset(
    asset: TwoAssetActionAsset
) {
    Row(
        modifier = Modifier
            .heightIn(min = 80.dp)
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomStackedIcon(icon = asset.icon)

        SmallHorizontalSpacer()

        Column(
            modifier = Modifier.weight(weight = 1F, fill = true)
        ) {
            Text(
                text = asset.title,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
            SmallestVerticalSpacer()
            Text(
                text = asset.subtitle,
                style = AppTheme.typography.paragraph1,
                color = AppTheme.colors.body
            )
        }

        SmallHorizontalSpacer()

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = asset.endTitle,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
            SmallestVerticalSpacer()
            Text(
                text = asset.endSubtitle,
                style = AppTheme.typography.paragraph1,
                color = AppTheme.colors.body
            )
        }
    }
}

@Composable
private fun Extra(
    extra: TwoAssetActionExtra
) {
    Row(
        modifier = Modifier
            .conditional(extra.onClick != null) {
                clickable(onClick = extra.onClick!!)
            }
            .heightIn(min = 80.dp)
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(weight = 1F, fill = true),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = extra.title,
                style = AppTheme.typography.paragraph2,
                color = AppColors.body
            )
            extra.onClick?.let {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.minusculeSpacing))
                Image(
                    Icons.Filled.Question.withTint(AppColors.dark).withSize(AppTheme.dimensions.smallSpacing)
                )
            }
        }

        SmallHorizontalSpacer()

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = extra.endTitle,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.title
            )
            extra.endSubtitle?.let {
                SmallestVerticalSpacer()
                Text(
                    text = extra.endSubtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFF0000FF)
@Composable
private fun PreviewTwoAssetAction() {
    TwoAssetAction(
        topAsset = TwoAssetActionAsset(
            title = "From",
            subtitle = "Ethereum",
            endTitle = "0.05459411 ETH",
            endSubtitle = "100.00",
            icon = StackedIcon.SingleIcon(ImageResource.Remote(""))
        ),
        topExtras = persistentListOf(
            TwoAssetActionExtra(
                title = "ETH network fees",
                endTitle = "~4.39",
                endSubtitle = "0.0011234 ETH",
                onClick = {}
            ),
            TwoAssetActionExtra(
                title = "Subtotal",
                endTitle = "~104.39"
            )
        ),
        bottomAsset = TwoAssetActionAsset(
            title = "To",
            subtitle = "Bitcoin",
            endTitle = "0.00350795 BTC",
            endSubtitle = "96.99",
            icon = StackedIcon.SingleIcon(ImageResource.Remote("")),
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF0000FF)
@Composable
private fun PreviewTwoAssetActionDark() {
    PreviewTwoAssetAction()
}

@Preview(showBackground = true, backgroundColor = 0XFF0000FF)
@Composable
private fun PreviewAssetStart() {
    Asset(
        TwoAssetActionAsset(
            title = "From",
            subtitle = "Ethereum",
            endTitle = "0.05459411 ETH",
            endSubtitle = "100.00",
            icon = StackedIcon.SingleIcon(ImageResource.Remote(""))
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF0000FF)
@Composable
private fun PreviewAssetStartDark() {
    PreviewAssetStart()
}

@Preview(showBackground = true, backgroundColor = 0XFF0000FF)
@Composable
private fun PreviewExtra() {
    Extra(
        TwoAssetActionExtra(
            title = "From",
            endTitle = "0.05459411 ETH",
            endSubtitle = "100.00",
        )
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF0000FF)
@Composable
private fun PreviewExtraDark() {
    PreviewExtra()
}
