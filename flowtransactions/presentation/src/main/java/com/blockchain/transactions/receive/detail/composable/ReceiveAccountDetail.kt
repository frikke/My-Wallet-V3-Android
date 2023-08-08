package com.blockchain.transactions.receive.detail.composable

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.alert.PillAlert
import com.blockchain.componentlib.alert.PillAlertType
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SmallInfoWithIcon
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalSecondaryButton
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.icons.Copy
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.loader.LoadingIndicator
import com.blockchain.componentlib.sheets.ScreenWithBottomSheet
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.QRCodeEncoder
import com.blockchain.componentlib.utils.QRCodeEncoder.DEFAULT_DIMENSION_QR_CODE
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.conditional
import com.blockchain.componentlib.utils.copyToClipboard
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.componentlib.utils.toStackedIcon
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.analytics.analyticsProvider
import com.blockchain.presentation.urllinks.URL_XLM_MEMO
import com.blockchain.stringResources.R
import com.blockchain.transactions.receive.ReceiveAnalyticsEvents
import com.blockchain.transactions.receive.detail.ReceiveAccountDetailIntent
import com.blockchain.transactions.receive.detail.ReceiveAccountDetailViewModel
import com.blockchain.transactions.receive.detail.ReceiveAccountDetailViewState
import com.blockchain.transactions.receive.detail.ReceiveAddressViewState
import com.blockchain.transactions.receive.network.NetworkWarning
import com.blockchain.transactions.receive.rotatingaddress.RotatingAddress
import com.blockchain.utils.abbreviate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import org.koin.androidx.compose.getViewModel

private const val ADDRESS_ABBREVIATION_LENGTH = 6

private sealed interface SheetType {
    data class RotatingAddress(
        val assetTicker: String,
        val label: String
    ) : SheetType

    data class Network(
        val assetTicker: String,
        val networkName: String
    ) : SheetType
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReceiveAccountDetail(
    viewModel: ReceiveAccountDetailViewModel = getViewModel(scope = payloadScope),
    onBackPressed: () -> Unit,
) {

    val analytics = analyticsProvider()
    val context = LocalContext.current

    val viewState: ReceiveAccountDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    LaunchedEffect(Unit) {
        viewModel.onIntent(ReceiveAccountDetailIntent.LoadData)
    }

    // //////////////////////////////////////////////
    // pill alert (copied from chrome)
    // todo cannot use the chrome pill atm since this screen can be opened from fragment
    var pillAlert: PillAlert? by remember { mutableStateOf(null) }
    var showPill by remember { mutableStateOf(false) }
    LaunchedEffect(pillAlert) {
        pillAlert?.let {
            showPill = true
            delay(TimeUnit.SECONDS.toMillis(3))
            showPill = false
        }
    }
    val pillAlertOffsetY by animateFloatAsState(
        targetValue = if (showPill) 0F else -300F,
        animationSpec = tween(
            durationMillis = 400
        ),
        label = "pillAlertOffsetY"
    )

    fun pillAlphaInterpolator(value: Float): Float {
        val x1 = 0f
        val x2 = -300f
        val y1 = 1f
        val y2 = 0.5f
        return (value - x1) * (y2 - y1) / (x2 - x1) + y1
    }
    // //////////////////////////////////////////////

    Box {
        ScreenWithBottomSheet(
            sheetContent = { type, hideSheet ->
                when (type) {
                    is SheetType.Network -> {
                        NetworkWarning(
                            assetTicker = type.assetTicker,
                            networkName = type.networkName,
                            closeOnClick = hideSheet
                        )
                    }

                    is SheetType.RotatingAddress -> {
                        RotatingAddress(
                            assetTicker = type.assetTicker,
                            accountLabel = type.label,
                            closeOnClick = hideSheet
                        )
                    }
                }
            },
            content = { showSheet ->
                ReceiveAccountsScreen(
                    icon = viewState.icon.toStackedIcon(),
                    assetTicker = viewState.assetTicker,
                    nativeAsset = viewState.nativeAsset,
                    receiveAddress = viewState.receiveAddress,
                    copyButtonEnabled = viewState.isCopyButtonEnabled,
                    networkWarningOnClick = {
                        val nativeAsset = viewState.nativeAsset
                        check(nativeAsset != null)
                        showSheet(
                            SheetType.Network(
                                assetTicker = viewState.assetTicker,
                                networkName = nativeAsset.first
                            )
                        )
                    },
                    rotatingAddressOnClick = {
                        showSheet(
                            SheetType.RotatingAddress(
                                assetTicker = viewState.assetTicker,
                                label = viewState.accountLabel
                            )
                        )
                    },
                    memoLearnMoreOnClick = {
                        context.openUrl(URL_XLM_MEMO)
                    },
                    copyOnClick = {
                        context.copyToClipboard(
                            label = viewState.assetTicker,
                            text = (viewState.receiveAddress as DataResource.Data).data.address
                        )

                        pillAlert = PillAlert(
                            text = TextValue.IntResValue(R.string.deposit_address_copied),
                            icon = Icons.Filled.Copy.withTint(Color.White),
                            type = PillAlertType.Info
                        )

                        analytics.logEvent(
                            ReceiveAnalyticsEvents.ReceiveDetailsCopied(
                                accountType = viewState.accountType,
                                networkTicker = viewState.assetTicker
                            )
                        )
                    },
                    memoCopyOnClick = {
                        context.copyToClipboard(
                            label = viewState.assetTicker + " memo",
                            text = it
                        )

                        pillAlert = PillAlert(
                            text = TextValue.IntResValue(R.string.deposit_memo_copied),
                            icon = Icons.Filled.Copy.withTint(Color.White),
                            type = PillAlertType.Info
                        )
                    },
                    onBackPressed = onBackPressed
                )
            }
        )

        pillAlert?.let {
            val color = Color(0XFF20242C) // default color wont work on dark mode bg - todo ethan
            PillAlert(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(AppTheme.dimensions.tinySpacing)
                    .graphicsLayer {
                        translationY = pillAlertOffsetY
                        alpha = pillAlphaInterpolator(pillAlertOffsetY)
                    },
                color = color,
                config = it
            )
        }
    }
}

@Composable
private fun ReceiveAccountsScreen(
    icon: StackedIcon,
    assetTicker: String,
    nativeAsset: Pair<String, String>?,
    receiveAddress: DataResource<ReceiveAddressViewState>,
    copyButtonEnabled: Boolean,
    networkWarningOnClick: () -> Unit,
    rotatingAddressOnClick: () -> Unit,
    memoLearnMoreOnClick: () -> Unit,
    copyOnClick: () -> Unit,
    memoCopyOnClick: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        SheetHeader(
            startImage = icon,
            title = stringResource(R.string.tx_title_receive, assetTicker),
            onClosePress = onBackPressed,
        )

        Column(
            modifier = Modifier
                .padding(AppTheme.dimensions.smallSpacing)
                .verticalScroll(rememberScrollState())
                .weight(1F)
        ) {

            // "asset on network" info
            nativeAsset?.let { (name, logoUrl) ->
                SmallInfoWithIcon(
                    iconUrl = logoUrl,
                    text = stringResource(R.string.coinview_asset_l1, assetTicker, name),
                    onClick = networkWarningOnClick
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            // network warning + qr + address
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White, // keep white bg because of qr code image
                shape = AppTheme.shapes.large
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    nativeAsset?.let { (name, _) ->
                        ReceiveNetworkWarning(
                            modifier = Modifier
                                .clickableNoEffect(onClick = networkWarningOnClick)
                                .padding(
                                    top = AppTheme.dimensions.smallSpacing
                                ),
                            assetTicker = assetTicker,
                            networkName = name
                        )
                    }

                    QrCode(
                        receiveAddress = receiveAddress
                    )

                    Address(
                        modifier = Modifier.padding(
                            bottom = AppTheme.dimensions.smallSpacing
                        ),
                        receiveAddress = receiveAddress,
                        rotatingAddressOnClick = rotatingAddressOnClick
                    )
                }
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            // memo
            (receiveAddress.map { it.memo } as? DataResource.Data)?.data?.let {
                CardAlert(
                    title = stringResource(R.string.receive_asset_memo_title, it),
                    subtitle = stringResource(R.string.receive_asset_memo_description, assetTicker),
                    titleIcon = Icons.Filled.Copy,
                    isBordered = false,
                    isDismissable = false,
                    titleIconOnClick = {
                        memoCopyOnClick(it)
                    },
                    primaryCta = CardButton(
                        text = stringResource(R.string.common_learn_more),
                        onClick = memoLearnMoreOnClick
                    )
                )
            }
        }

        MinimalSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing),
            text = stringResource(R.string.receive_copy),
            state = if (copyButtonEnabled) ButtonState.Enabled else ButtonState.Disabled,
            onClick = copyOnClick
        )
    }
}

@Composable
private fun ReceiveNetworkWarning(
    modifier: Modifier = Modifier,
    assetTicker: String,
    networkName: String
) {
    Row(modifier = modifier) {
        Image(
            imageResource = Icons.Info
                .withTint(AppColors.warning)
                .withSize(AppTheme.dimensions.smallSpacing)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            text = stringResource(R.string.receive_network_warning, assetTicker, networkName),
            style = AppTheme.typography.caption1,
            color = AppColors.warning
        )
    }
}

@Composable
private fun QrCode(
    receiveAddress: DataResource<ReceiveAddressViewState>,
) {
    when (receiveAddress) {
        DataResource.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1F),
            ) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.primary
                )
            }
        }

        is DataResource.Error -> {
        }

        is DataResource.Data -> {
            var bitmap: Bitmap? by remember {
                mutableStateOf(QRCodeEncoder.encodeAsBitmap(receiveAddress.data.uri, DEFAULT_DIMENSION_QR_CODE))
            }

            bitmap?.let {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1F),
                    imageResource = ImageResource.LocalWithResolvedBitmap(
                        bitmap = it,
                        shape = RectangleShape
                    )
                )
            }
        }
    }
}

@Composable
private fun Address(
    modifier: Modifier = Modifier,
    receiveAddress: DataResource<ReceiveAddressViewState>,
    rotatingAddressOnClick: () -> Unit,
) {
    (receiveAddress as? DataResource.Data)?.data?.let {
        Row(
            modifier = modifier.conditional(it.isRotating) {
                clickableNoEffect(onClick = rotatingAddressOnClick)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = it.address.abbreviate(
                    startLength = ADDRESS_ABBREVIATION_LENGTH,
                    endLength = ADDRESS_ABBREVIATION_LENGTH
                ),
                style = AppTheme.typography.paragraph1,
                color = AppColors.body
            )

            if (it.isRotating) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Image(
                    imageResource = Icons.Filled.Question
                        .withTint(AppColors.body)
                        .withSize(AppTheme.dimensions.smallSpacing)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewReceiveAccountsScreen() {
    ReceiveAccountsScreen(
        icon = StackedIcon.SingleIcon(ImageResource.Remote("")),
        assetTicker = "USDC",
        nativeAsset = Pair("Polygon", "http"),
        receiveAddress = DataResource.Data(
            ReceiveAddressViewState(
                uri = "0xCFcb5fD2cfA67CaAce286f43c1fbc202A93F1FE8",
                address = "0xCFcb5fD2cfA67CaAce286f43c1fbc202A93F1FE8",
                memo = "JDZHDZJ23J",
                isRotating = true
            )
        ),
        copyButtonEnabled = true,
        networkWarningOnClick = {},
        rotatingAddressOnClick = {},
        memoLearnMoreOnClick = {},
        copyOnClick = {},
        memoCopyOnClick = {},
        onBackPressed = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewReceiveAccountsScreenDark() {
    PreviewReceiveAccountsScreen()
}

@Preview
@Composable
private fun PreviewReceiveAccountsScreenLoading() {
    ReceiveAccountsScreen(
        icon = StackedIcon.SingleIcon(ImageResource.Remote("")),
        assetTicker = "USDC",
        nativeAsset = Pair("Polygon", "http"),
        receiveAddress = DataResource.Loading,
        copyButtonEnabled = false,
        networkWarningOnClick = {},
        rotatingAddressOnClick = {},
        memoLearnMoreOnClick = {},
        copyOnClick = {},
        memoCopyOnClick = {},
        onBackPressed = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewReceiveAccountsScreenLoadingDark() {
    PreviewReceiveAccountsScreenLoading()
}
