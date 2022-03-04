package com.blockchain.componentlib.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark800

@Composable
fun CallOutCard(
    title: String,
    subtitle: String,
    iconResource: ImageResource = ImageResource.None,
    callToActionButton: CardButton? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {

    val backgroundColor = if (!isDarkTheme) {
        Color.White
    } else {
        Dark800
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 340.dp)
            .background(color = backgroundColor, shape = AppTheme.shapes.medium)
            .border(1.dp, Blue400, AppTheme.shapes.medium)
    ) {
        Surface(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.medium_margin))
        ) {
            Row(modifier = Modifier.background(backgroundColor), verticalAlignment = CenterVertically) {
                Image(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.large_margin)),
                    imageResource = iconResource
                )

                Column(
                    modifier = Modifier
                        .weight(1f, true)
                        .padding(start = dimensionResource(R.dimen.medium_margin))
                        .background(backgroundColor)
                ) {

                    Text(
                        text = title,
                        style = AppTheme.typography.caption1,
                        color = AppTheme.colors.title
                    )

                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp),
                        text = subtitle,
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title
                    )
                }

                SmallPrimaryButton(
                    modifier = Modifier
                        .padding(start = dimensionResource(R.dimen.medium_margin))
                        .background(backgroundColor),
                    text = callToActionButton?.text ?: "",
                    onClick = callToActionButton?.onClick ?: {},
                    state = ButtonState.Enabled
                )
            }
        }
    }
}

@Preview
@Composable
fun CallOutCardAlert_Basic() {
    AppTheme {
        AppSurface {
            CallOutCard(
                title = "Title", subtitle = "Subtitle",
                iconResource = ImageResource.Local(R.drawable.ic_blockchain, null),
                CardButton("Go") {}
            )
        }
    }
}
