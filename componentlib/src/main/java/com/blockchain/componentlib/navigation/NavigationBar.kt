package com.blockchain.componentlib.navigation

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Colors
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ArrowLeft
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.MenuKebabVertical
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import com.blockchain.componentlib.theme.clickableWithIndication
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.AuthPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import org.koin.androidx.compose.get

sealed class NavigationBarButton(val onClick: () -> Unit) {
    data class Icon(
        val drawable: Int,
        val color: Color? = Grey400,
        @DimenRes val size: Int = com.blockchain.componentlib.R.dimen.standard_spacing,
        @StringRes val contentDescription: Int,
        val onIconClick: () -> Unit
    ) : NavigationBarButton(onIconClick)

    data class IconResource(
        val image: ImageResource,
        val onIconClick: () -> Unit
    ) : NavigationBarButton(onIconClick)

    data class Text(val text: String, val color: Color? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)

    data class TextWithColorInt(val text: String, @ColorRes val colorId: Int? = null, val onTextClick: () -> Unit) :
        NavigationBarButton(onTextClick)

    data class DropdownMenu(
        val expanded: MutableState<Boolean>,
        val onMenuClick: () -> Unit,
        val content: @Composable ColumnScope.() -> Unit
    ) :
        NavigationBarButton(onMenuClick)
}

@Composable
fun NavigationBar(
    modeColor: ModeBackgroundColor = ModeBackgroundColor.Current,
    mutedBackground: Boolean = true,
    title: String,
    icon: StackedIcon = StackedIcon.None,
    onBackButtonClick: (() -> Unit)? = null,
    navigationBarButtons: List<NavigationBarButton> = emptyList()
) {
    NavigationBar(
        modeColor = modeColor,
        mutedBackground = mutedBackground,
        title = title,
        icon = icon,
        startNavigationBarButton = onBackButtonClick?.let { onClick ->
            NavigationBarButton.IconResource(
                Icons.ArrowLeft.withTint(AppColors.title),
                onIconClick = onClick
            )
        },
        endNavigationBarButtons = navigationBarButtons
    )
}

@Composable
private fun NavigationBar(
    walletMode: WalletMode?,
    mutedBg: Boolean,
    title: String,
    icon: StackedIcon = StackedIcon.None,
    onBackButtonClick: (() -> Unit)? = null,
    navigationBarButtons: List<NavigationBarButton> = emptyList()
) {
    NavigationBar(
        walletMode = walletMode,
        mutedBg = mutedBg,
        title = title,
        icon = icon,
        startNavigationBarButton = onBackButtonClick?.let { onClick ->
            NavigationBarButton.IconResource(
                Icons.ArrowLeft.withTint(AppColors.title),
                onIconClick = onClick
            )
        },
        endNavigationBarButtons = navigationBarButtons
    )
}

@Composable
fun NavigationBar(
    modeColor: ModeBackgroundColor = ModeBackgroundColor.Current,
    mutedBackground: Boolean = true,
    title: String,
    icon: StackedIcon = StackedIcon.None,
    startNavigationBarButton: NavigationBarButton? = null,
    endNavigationBarButtons: List<NavigationBarButton> = emptyList()
) {
    val walletMode: WalletMode? = walletModeProvider(modeColor)

    NavigationBar(
        walletMode = walletMode,
        mutedBg = mutedBackground,
        title = title,
        icon = icon,
        startNavigationBarButton = startNavigationBarButton,
        endNavigationBarButtons = endNavigationBarButtons
    )
}

@Composable
fun NavigationBar(
    walletMode: WalletMode?,
    mutedBg: Boolean,
    title: String,
    icon: StackedIcon = StackedIcon.None,
    startNavigationBarButton: NavigationBarButton? = null,
    endNavigationBarButtons: List<NavigationBarButton> = emptyList(),
    spaceBetweenArrangement: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .defaultMinSize(minHeight = 52.dp)
            .then(
                when (walletMode) {
                    WalletMode.CUSTODIAL -> Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(START_TRADING, END_TRADING)
                        )
                    )

                    WalletMode.NON_CUSTODIAL -> Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(START_DEFI, END_DEFI)

                        )
                    )

                    else -> Modifier.background(
                        if (mutedBg) AppTheme.colors.background else AppTheme.colors.backgroundSecondary
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.CenterStart)
                .background(
                    if (mutedBg) AppTheme.colors.background else AppTheme.colors.backgroundSecondary,
                    walletMode?.let { AppTheme.shapes.veryLarge.topOnly() } ?: RectangleShape
                )
                .padding(horizontal = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (spaceBetweenArrangement) Arrangement.SpaceBetween else Arrangement.Start
        ) {
            startNavigationBarButton?.let { button ->
                when (button) {
                    is NavigationBarButton.Icon -> {
                        StartButton(button = button)
                    }

                    is NavigationBarButton.IconResource -> {
                        StartButtonResource(button = button)
                    }

                    is NavigationBarButton.DropdownMenu,
                    is NavigationBarButton.Text,
                    is NavigationBarButton.TextWithColorInt -> {
                    }
                }
            }

            if (icon !is StackedIcon.None) {
                CustomStackedIcon(
                    icon = icon,
                    iconBackground = AppTheme.colors.backgroundSecondary,
                    borderColor = if (mutedBg) AppTheme.colors.background else AppTheme.colors.backgroundSecondary
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }

            Text(
                modifier = if (!spaceBetweenArrangement) Modifier.weight(1f) else Modifier,
                text = title,
                color = AppTheme.colors.title,
                style = AppTheme.typography.body2
            )
            endNavigationBarButtons.forEach {
                Spacer(
                    modifier = Modifier.width(dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
                )
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
                            com.blockchain.componentlib.basic.Image(
                                it.image.run {
                                    if (this is ImageResource.Local) withTint(AppColors.title)
                                    else this
                                }
                            )
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

                        is NavigationBarButton.DropdownMenu -> {
                            com.blockchain.componentlib.basic.Image(
                                imageResource = Icons.MenuKebabVertical.withTint(AppColors.title)
                            )

                            // DropdownMenu uses MaterialTheme.shapes.medium for its shape, so we need to override it
                            MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = AppTheme.shapes.large)) {
                                DropdownMenu(
                                    modifier = Modifier.background(
                                        AppColors.backgroundSecondary, AppTheme.shapes.large
                                    ),
                                    expanded = it.expanded.value,
                                    onDismissRequest = { it.expanded.value = false },
                                    content = it.content
                                )
                            }
                        }
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
            .clickableWithIndication {
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
    Spacer(modifier = Modifier.width(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)))
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
    Spacer(modifier = Modifier.width(dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)))
}

@Composable
private fun isLoggedInProvider(): Boolean {
    var isLoggedIn: Boolean by remember { mutableStateOf(true) }
    if (!LocalInspectionMode.current) {
        isLoggedIn = get<AuthPrefs>().run { walletGuid.isNotEmpty() && pinId.isNotEmpty() }
    }
    return isLoggedIn
}

@Composable
private fun walletModeProvider(modeColor: ModeBackgroundColor): WalletMode? {
    return if (LocalInspectionMode.current) {
        WalletMode.CUSTODIAL
    } else {
        val walletMode: WalletMode? by when (modeColor) {
            ModeBackgroundColor.Current -> {
                if (isLoggedInProvider()) {
                    get<WalletModeService>(
                        scope = payloadScope
                    ).walletMode.collectAsStateLifecycleAware(initial = null)
                } else {
                    remember { mutableStateOf(null) }
                }
            }

            is ModeBackgroundColor.Override -> {
                remember { mutableStateOf(modeColor.walletMode) }
            }

            ModeBackgroundColor.None -> {
                remember { mutableStateOf(null) }
            }
        }
        walletMode
    }
    //    var walletMode: WalletMode? by remember { mutableStateOf(WalletMode.CUSTODIAL) }
    //
    //    if (!LocalInspectionMode.current) {
    //        val byWalletMode: WalletMode? by when (modeColor) {
    //            ModeBackgroundColor.Current -> {
    //                if (isLoggedInProvider()) {
    //                    get<WalletModeService>(
    //                        scope = payloadScope
    //                    ).walletMode.collectAsStateLifecycleAware(initial = null)
    //                } else {
    //                    remember { mutableStateOf(null) }
    //                }
    //            }
    //            is ModeBackgroundColor.Override -> {
    //                remember { mutableStateOf(modeColor.walletMode) }
    //            }
    //            ModeBackgroundColor.None -> {
    //                remember { mutableStateOf(null) }
    //            }
    //        }
    //        walletMode = byWalletMode
    //    }
    //
    //    return walletMode
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreview() {
    AppTheme {
        NavigationBar(
            walletMode = WalletMode.NON_CUSTODIAL,
            mutedBg = true,
            title = "Test",
            onBackButtonClick = null,
            navigationBarButtons = emptyList()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreviewLongTextModeNull() {
    AppTheme {
        NavigationBar(
            walletMode = WalletMode.NON_CUSTODIAL,
            mutedBg = true,
            title = "Comunicarse con el soporte técnico longer longer longer",
            icon = StackedIcon.SmallTag(
                main = ImageResource.Local(R.drawable.ic_close_circle_dark),
                tag = ImageResource.Local(R.drawable.ic_close_circle)
            ),
            onBackButtonClick = { },
            navigationBarButtons = emptyList()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPreviewLongText() {
    AppTheme {
        NavigationBar(
            walletMode = null,
            mutedBg = false,
            title = "Comunicarse con el soporte técnico longer longer longer",
            icon = StackedIcon.SmallTag(
                main = ImageResource.Local(R.drawable.ic_close_circle_dark),
                tag = ImageResource.Local(R.drawable.ic_close_circle)
            ),
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
            walletMode = WalletMode.CUSTODIAL,
            mutedBg = true,
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
            walletMode = WalletMode.CUSTODIAL,
            mutedBg = true,
            title = "Test",
            icon = StackedIcon.SmallTag(
                main = ImageResource.Local(R.drawable.ic_close_circle_dark),
                tag = ImageResource.Local(R.drawable.ic_close_circle)
            ),
            onBackButtonClick = {},
            navigationBarButtons = listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_close_circle,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
                ) {},
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_close_circle,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
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
            walletMode = WalletMode.CUSTODIAL,
            mutedBg = true,
            title = "Test",
            onBackButtonClick = null,
            navigationBarButtons = listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_close_circle,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
                ) {},
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_close_circle,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
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
            walletMode = WalletMode.CUSTODIAL,
            mutedBg = true,
            title = "Test",
            onBackButtonClick = {},
            navigationBarButtons = listOf(
                NavigationBarButton.Text(
                    text = "Cancel"
                ) {}
            )
        )
    }
}
