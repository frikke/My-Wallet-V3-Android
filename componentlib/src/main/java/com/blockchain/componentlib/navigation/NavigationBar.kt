package com.blockchain.componentlib.navigation

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopEnd
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
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400

sealed class NavigationBarButton(val onClick: () -> Unit) {
    data class Icon(
        val drawable: Int,
        val color: Color? = Grey400,
        @DimenRes val size: Int = R.dimen.standard_spacing,
        @StringRes val contentDescription: Int,
        val onIconClick: () -> Unit,
    ) :
        NavigationBarButton(onIconClick)

    data class IconResource(
        val image: ImageResource,
        val onIconClick: () -> Unit,
    ) : NavigationBarButton(onIconClick)

    data class DropdownIndicator(
        val dropDownClicked: () -> Unit,
        val text: String,
        val isHighlighted: Boolean,
        val rightIcon: Int,
        val contentDescription: String,
        val color: Color = Grey000,
    ) : NavigationBarButton(dropDownClicked)

    data class Text(val text: String, val color: Color? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)

    data class TextWithColorInt(val text: String, @ColorRes val colorId: Int? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)
}

@Composable
fun NavigationBar(
    title: String,
    onBackButtonClick: (() -> Unit)? = null,
    dropDownIndicator: NavigationBarButton.DropdownIndicator? = null,
    navigationBarButtons: List<NavigationBarButton> = emptyList(),
) = NavigationBar(
    title = title,
    startNavigationBarButton = onBackButtonClick?.let { onClick ->
        NavigationBarButton.Icon(
            drawable = R.drawable.ic_nav_bar_back,
            onIconClick = onClick,
            contentDescription = R.string.accessibility_back
        )
    } ?: dropDownIndicator,
    endNavigationBarButtons = navigationBarButtons
)

@Composable
fun NavigationBar(
    title: String,
    startNavigationBarButton: NavigationBarButton? = null,
    endNavigationBarButtons: List<NavigationBarButton> = emptyList(),
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
                .padding(horizontal = dimensionResource(R.dimen.medium_spacing)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            startNavigationBarButton?.let { button ->
                when (button) {
                    is NavigationBarButton.Icon -> {
                        StartButton(button = button)
                    }
                    is NavigationBarButton.IconResource -> {
                        StartButtonResource(button = button)
                    }
                    is NavigationBarButton.DropdownIndicator -> {
                        DropDown(button)
                    }
                    is NavigationBarButton.Text,
                    is NavigationBarButton.TextWithColorInt -> {
                    }
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = AppTheme.colors.title,
                style = AppTheme.typography.body2
            )
            endNavigationBarButtons.forEach {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smallest_spacing)))
                Box(
                    modifier = Modifier
                        .clickable {
                            it.onClick.invoke()
                        }
                        .align(CenterVertically)
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
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
                        is NavigationBarButton.IconResource -> {
                            com.blockchain.componentlib.basic.Image(imageResource = it.image)
                        }
                        is NavigationBarButton.Text -> {
                            Text(
                                modifier = Modifier.wrapContentWidth(),
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
                        is NavigationBarButton.DropdownIndicator -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.StartButton(button: NavigationBarButton.Icon) {
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
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.very_small_spacing)))
}

@Composable
fun RowScope.StartButtonResource(button: NavigationBarButton.IconResource) {
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
        com.blockchain.componentlib.basic.Image(imageResource = button.image)
    }
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.very_small_spacing)))
}

@Composable
fun RowScope.DropDown(dropdownIndicator: NavigationBarButton.DropdownIndicator) {

    var isHighlighted by remember { mutableStateOf(dropdownIndicator.isHighlighted) }

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .align(CenterVertically)
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    isHighlighted = false
                    dropdownIndicator.onClick.invoke()
                }
                .background(
                    dropdownIndicator.color,
                    RoundedCornerShape(dimensionResource(id = R.dimen.medium_spacing))
                )
                .padding(
                    start = 0.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
        ) {
            Image(
                painter = painterResource(id = dropdownIndicator.rightIcon),
                contentDescription = dropdownIndicator.contentDescription,
                modifier = Modifier
                    .padding(
                        start = dimensionResource(id = R.dimen.tiny_spacing)
                    )
            )
            Text(
                text = dropdownIndicator.text,
                style = AppTheme.typography.body1,
                modifier = Modifier
                    .padding(
                        start = dimensionResource(id = R.dimen.tiny_spacing),
                        end = dimensionResource(id = R.dimen.tiny_spacing)
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "IconArrowDown",
                modifier = Modifier
                    .padding(
                        end = dimensionResource(id = R.dimen.tiny_spacing)
                    )
            )
        }
        if (isHighlighted) {
            DropDownHighLightIndicator(modifier = Modifier.align(TopEnd))
        }
    }
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.very_small_spacing)))
}

@Composable
fun DropDownHighLightIndicator(modifier: Modifier) {
    Canvas(
        modifier = modifier
            .border(
                2.dp,
                Color.White,
                shape = CircleShape
            )
            .size(12.dp),
        onDraw = {
            drawCircle(
                color = Color(0xFFDE0082),
                radius = 5.dp.toPx()
            )
        }
    )
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
fun NavigationBarPreviewLongText() {
    AppTheme {
        NavigationBar(
            title = "Comunicarse con el soporte técnico longer longer longer",
            onBackButtonClick = { },
            navigationBarButtons = emptyList()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreviewLongTextWithActions() {
    AppTheme {
        NavigationBar(
            title = "Comunicarse con el soporte técnico longer longer longer",
            onBackButtonClick = { },
            navigationBarButtons = listOf(
                NavigationBarButton.Text("Some button") {}
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreview2() {
    AppTheme {
        NavigationBar(
            "Test",
            {},
            null,
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
fun NavigationBarPreviewDropDown() {
    AppTheme {
        NavigationBar(
            "Test",
            null,
            NavigationBarButton.DropdownIndicator(
                dropDownClicked = {},
                text = "Portfolio",
                rightIcon = R.drawable.ic_bottom_nav_home,
                isHighlighted = true,
                contentDescription = "123",
            ),
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
            "Test", {}, null,
            listOf(
                NavigationBarButton.Text(
                    text = "Cancel"
                ) {}
            )
        )
    }
}
