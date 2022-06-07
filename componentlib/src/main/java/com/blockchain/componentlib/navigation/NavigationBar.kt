package com.blockchain.componentlib.navigation

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400

sealed class NavigationBarButton(val onClick: () -> Unit) {
    data class Icon(
        val drawable: Int,
        val color: Color? = Grey400,
        @DimenRes val size: Int = R.dimen.standard_margin,
        @StringRes val contentDescription: Int,
        val onIconClick: () -> Unit
    ) :
        NavigationBarButton(onIconClick)

    data class Text(val text: String, val color: Color? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)

    data class TextWithColorInt(val text: String, @ColorRes val colorId: Int? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)
}

@Composable
fun NavigationBar(
    title: String,
    onBackButtonClick: (() -> Unit)? = null,
    navigationBarButtons: List<NavigationBarButton> = emptyList()
) = NavigationBar(
    title = title,
    startNavigationBarButton = onBackButtonClick?.let { onClick ->
        NavigationBarButton.Icon(
            drawable = R.drawable.ic_nav_bar_back,
            onIconClick = onClick,
            contentDescription = R.string.accessibility_back
        )
    },
    endNavigationBarButtons = navigationBarButtons
)

@Composable
fun NavigationBar(
    title: String,
    startNavigationBarButton: NavigationBarButton.Icon? = null,
    endNavigationBarButtons: List<NavigationBarButton> = emptyList()
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
                .padding(start = dimensionResource(R.dimen.standard_margin))
        ) {
            startNavigationBarButton?.let { button ->
                Box(
                    modifier = Modifier
                        .clickable {
                            button.onClick.invoke()
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
                        painter = painterResource(id = button.drawable),
                        contentDescription = stringResource(id = button.contentDescription),
                        colorFilter = if (button.color != null) ColorFilter.tint(button.color) else null
                    )
                }
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.very_small_margin)))
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
                .padding(end = dimensionResource(R.dimen.standard_margin))
        ) {
            endNavigationBarButtons.forEach {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smallest_margin)))
                Box(
                    modifier = Modifier
                        .clickable {
                            it.onClick.invoke()
                        }
                        .align(CenterVertically)
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    when (it) {
                        is NavigationBarButton.Icon -> {
                            Image(
                                modifier = Modifier.size(dimensionResource(it.size)),
                                painter = painterResource(id = it.drawable),
                                contentDescription = stringResource(id = it.contentDescription),
                                colorFilter = if (it.color != null) ColorFilter.tint(it.color) else null
                            )
                        }
                        is NavigationBarButton.Text -> {
                            Text(
                                text = it.text,
                                color = it.color ?: AppTheme.colors.error,
                                style = AppTheme.typography.body2
                            )
                        }
                        is NavigationBarButton.TextWithColorInt -> {
                            Text(
                                text = it.text,
                                color = it.colorId?.let { colorResource(id = it) } ?: AppTheme.colors.error,
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
        NavigationBar(title = "Test", onBackButtonClick = null, navigationBarButtons = emptyList())
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
                    drawable = R.drawable.ic_bottom_nav_buy,
                    contentDescription = R.string.accessibility_back
                ) {},
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_bottom_nav_buy,
                    contentDescription = R.string.accessibility_back
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
