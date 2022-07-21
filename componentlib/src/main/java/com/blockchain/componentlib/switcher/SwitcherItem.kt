package com.blockchain.componentlib.switcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark400
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey900

@Composable
fun SwitcherItem(
    modifier: Modifier = Modifier,
    text: String,
    startIcon: ImageResource = ImageResource.None,
    endIcon: ImageResource = ImageResource.Local(R.drawable.ic_arrow_right),
    state: SwitcherState = SwitcherState.Enabled,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    onClick: () -> Unit,
) {

    val textColor = when (state) {
        SwitcherState.Enabled -> if (!isDarkMode) Grey900 else White
        SwitcherState.Disabled -> if (!isDarkMode) Grey700 else Dark400
    }

    val backgroundColor = when (state) {
        SwitcherState.Enabled -> if (!isDarkMode) Grey000 else Grey100
        SwitcherState.Disabled -> if (!isDarkMode) Dark700 else Dark800
    }

    Row(
        modifier = modifier
            .clickable {
                onClick.invoke()
            }
            .background(
                backgroundColor,
                RoundedCornerShape(dimensionResource(id = R.dimen.medium_margin))
            )
            .padding(
                start = 0.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageResource = startIcon,
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.tiny_margin)
                )
        )
        Text(
            text = text,
            style = AppTheme.typography.body1,
            color = textColor,
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.tiny_margin),
                    end = dimensionResource(id = R.dimen.tiny_margin)
                )

        )
        Image(
            imageResource = endIcon,
            modifier = Modifier
                .padding(
                    end = dimensionResource(id = R.dimen.tiny_margin)
                )
        )
    }
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.very_small_margin)))
}
