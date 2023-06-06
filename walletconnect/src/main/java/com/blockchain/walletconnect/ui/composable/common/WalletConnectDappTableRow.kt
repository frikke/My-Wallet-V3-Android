package com.blockchain.walletconnect.ui.composable.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.stringResources.R.string
import com.blockchain.walletconnect.R

@Composable
fun WalletConnectDappTableRow(
    session: DappSessionUiElement,
    shouldEllipse: Boolean,
    onSessionClicked: () -> Unit,
) {
    TableRow(
        contentStart = {
            Box {
                Image(
                    imageResource = session.dappLogoUrl.takeIf { it.isNotEmpty() }?.let {
                        ImageResource.Remote(it)
                    } ?: ImageResource.Local(R.drawable.ic_walletconnect_logo)
                )
            }
        },
        content = {
            Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                SimpleText(
                    text = session.dappName.ifEmpty { stringResource(string.common_dapp) },
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                    isMultiline = !shouldEllipse,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.size(2.dp))

                SimpleText(
                    text = session.dappUrl,
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                    isMultiline = !shouldEllipse,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        contentEnd = {
            Row(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.End)
                    .background(AppTheme.colors.light, shape = CircleShape)
                    .weight(0.8f)
                    .padding(
                        horizontal = AppTheme.dimensions.tinySpacing,
                        vertical = AppTheme.dimensions.smallestSpacing,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Image(imageResource = ImageResource.Remote(session.chainLogo, size = 16.dp))
                    TinyHorizontalSpacer()
                    SimpleText(
                        text = session.chainName,
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                },
            )
        },
        onContentClicked = onSessionClicked
    )
}

@Preview
@Composable
fun WalletConnectDappTableRowPreview() {
    AppTheme {
        WalletConnectDappTableRow(
            session = DappSessionUiElement(
                dappName = "My Dapp",
                dappDescription = "This is a description of my dapp",
                dappUrl = "https://mydapp.com",
                dappLogoUrl = "https://mydapp.com/logo.png",
                chainName = "Ethereum",
                chainLogo = "https://ethereum.org/logo.png",
                sessionId = "1234567890",
                isV2 = true
            ),
            shouldEllipse = false,
            onSessionClicked = {},
        )
    }
}
