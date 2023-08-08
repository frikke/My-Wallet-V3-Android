package com.blockchain.home.presentation.dapps.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.walletconnect.R

@Composable
fun WalletConnectDashboardCTA(
    openQRCodeScanner: () -> Unit,
) {
    Card(
        backgroundColor = AppTheme.colors.backgroundSecondary,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        TableRow(
            contentStart = {
                Box {
                    Image(imageResource = ImageResource.Local(R.drawable.ic_walletconnect_logo))
                }
            },
            content = {
                Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                    SimpleText(
                        text = "WalletConnect",
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    Spacer(modifier = Modifier.size(2.dp))

                    SimpleText(
                        text = "Connect your wallet to Dapps",
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
            },
            contentEnd = {
                Button(
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.End),
                    content = {
                        Image(
                            imageResource = ImageResource.Local(
                                com.blockchain.componentlib.icons.R.drawable.viewfinder_off,
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                        )
                    },
                    onClick = {
                        openQRCodeScanner()
                        // analytics.logEvent(DashboardAnalyticsEvents.EarnGetStartedClicked) TODO ANALYTICS
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.body)
                )
            }
        )
    }
}

@Preview
@Composable
fun WalletConnectDashboardCTAPreview() {
    AppTheme {
        WalletConnectDashboardCTA(openQRCodeScanner = {})
    }
}
