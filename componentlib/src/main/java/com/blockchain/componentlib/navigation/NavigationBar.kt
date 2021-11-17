package com.blockchain.componentlib.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400

sealed class NavigationBarButton(val onClick: () -> Unit) {
    data class Icon(val drawable: Int, val color: Color? = null, val onIconClick: () -> Unit) :
        NavigationBarButton(onIconClick)

    data class Text(val text: String, val color: Color? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)
}

@Composable
fun NavigationBar(
    title: String,
    onBackButtonClick: (() -> Unit)? = null,
    navigationBarButtons: List<NavigationBarButton> = listOf()
) {

    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .defaultMinSize(minHeight = 52.dp)
            .background(AppTheme.colors.background)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            if (onBackButtonClick != null) {
                Box(
                    modifier = Modifier
                        .clickable {
                            onBackButtonClick.invoke()
                        }
                        .align(CenterVertically)
                        .padding(
                            start = 0.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nav_bar_back),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Grey400)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title,
                color = AppTheme.colors.title,
                style = AppTheme.typography.title2
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            navigationBarButtons.forEach {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clickable {
                            it.onClick.invoke()
                        }
                        .align(CenterVertically)
                        .padding(start = 8.dp, top = 8.dp, end = 0.dp, bottom = 8.dp)
                ) {
                    when (it) {
                        is NavigationBarButton.Icon -> {
                            Image(
                                painter = painterResource(id = it.drawable),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(it.color ?: Grey400)
                            )
                        }
                        is NavigationBarButton.Text -> {
                            Text(
                                text = it.text,
                                color = it.color ?: AppTheme.colors.error,
                                style = AppTheme.typography.body2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreview() {
    AppTheme {
        NavigationBar("Test")
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreview2() {
    AppTheme {
        NavigationBar(
            "Test",
            {},
            listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_bottom_nav_buy
                ) {},
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_bottom_nav_buy
                ) {}
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreview3() {
    AppTheme {
        NavigationBar(
            "Test", {},
            listOf(
                NavigationBarButton.Text(
                    text = "Cancel"
                ) {}
            )
        )
    }
}
