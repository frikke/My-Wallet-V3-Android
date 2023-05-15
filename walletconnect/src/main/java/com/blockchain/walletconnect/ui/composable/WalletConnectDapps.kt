package com.blockchain.walletconnect.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun WalletConnectDappTableRow(
    session: DappSessionUiElement,
    onSessionClicked: () -> Unit,
) {
    TableRow(
        contentStart = {
            Box {
                Image(imageResource = ImageResource.Remote(session.dappLogoUrl))
            }
        },
        content = {
            Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                SimpleText(
                    text = session.dappName,
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )

                Spacer(modifier = Modifier.size(2.dp))

                SimpleText(
                    text = session.dappUrl,
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
            }
        },
        contentEnd = {
            Button(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.End)
                    .weight(1f),
                content = {
                    Image(imageResource = ImageResource.Remote(session.chainLogo))
                    TinyHorizontalSpacer()
                    SimpleText(
                        text = session.chainName,
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                },
                onClick = onSessionClicked,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.light)
            )
        }
    )
}

@Preview
@Composable
fun WalletConnectDappTableRowPreview() {
    AppTheme {
        WalletConnectDappTableRow(
            session = DappSessionUiElement(
                dappName = "My Dapp",
                dappUrl = "https://mydapp.com",
                dappLogoUrl = "https://mydapp.com/logo.png",
                chainName = "Ethereum",
                chainLogo = "https://ethereum.org/logo.png",
            ),
            onSessionClicked = {},
        )
    }
}

data class DappSessionUiElement(
    val dappName: String,
    val dappUrl: String,
    val dappLogoUrl: String,
    val chainName: String,
    val chainLogo: String,
)
