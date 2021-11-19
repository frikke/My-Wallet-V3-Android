package com.blockchain.componentlib.alert

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.Grey800

enum class AlertType {
    Default, Success, Warning, Error
}

@Composable
fun CardAlert(
    title: String,
    subtitle: String,
    alertType: AlertType = AlertType.Default,
    isBordered: Boolean = false,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onClose: () -> Unit = {}
) {

    val typeColor = when (alertType) {
        AlertType.Default -> AppTheme.colors.title
        AlertType.Success -> AppTheme.colors.success
        AlertType.Warning -> AppTheme.colors.warning
        AlertType.Error -> AppTheme.colors.error
    }

    val borderColor = if (alertType == AlertType.Default) {
        if (!isDarkTheme) {
            Grey300
        } else {
            Dark600
        }
    } else {
        typeColor
    }

    var boxModifier = Modifier
        .padding(4.dp)
        .defaultMinSize(minWidth = 340.dp)
        .background(color = AppTheme.colors.light, shape = AppTheme.shapes.small)

    if (isBordered) {
        boxModifier = boxModifier.border(1.dp, borderColor, AppTheme.shapes.small)
    }

    Box(
        modifier = boxModifier
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .background(AppTheme.colors.light)
                .clip(AppTheme.shapes.small)

        ) {
            Column(
                modifier = Modifier.background(AppTheme.colors.light)
            ) {
                Row {
                    Text(
                        modifier = Modifier
                            .background(AppTheme.colors.light)
                            .weight(1f, true),
                        text = title,
                        style = AppTheme.typography.body2,
                        color = typeColor
                    )
                    CardCloseButton(onClick = onClose)
                }
                Text(
                    modifier = Modifier
                        .background(AppTheme.colors.light)
                        .padding(top = 8.dp),
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.title
                )
            }
        }
    }
}

@Composable
fun CardCloseButton(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onClick: () -> Unit = {}
) {

    val backgroundColor = if (!isDarkTheme) {
        Grey100
    } else {
        Grey800
    }

    Box(
        modifier = Modifier
            .clickable {
                onClick.invoke()
            }
            .size(24.dp)
            .background(color = backgroundColor, shape = CircleShape)
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun DefaultCardAlert_Basic() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Title", subtitle = "Subtitle")
        }
    }
}

@Preview
@Composable
fun SuccessCardAlert_Basic() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Title", subtitle = "Subtitle", alertType = AlertType.Success)
        }
    }
}
