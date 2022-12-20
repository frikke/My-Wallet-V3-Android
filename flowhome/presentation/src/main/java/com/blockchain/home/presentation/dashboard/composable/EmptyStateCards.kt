package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.home.presentation.R
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency

@Preview
@Composable
fun EmptyStateCards() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 3.dp
        ) {
            TableRow(content = {
                val progress = 0
                Box {
                    Canvas(modifier = Modifier.size(50.dp), onDraw = {
                        drawCircle(
                            color = Grey000,
                            style = Stroke(
                                width = 12f
                            )
                        )
                        drawArc(
                            color = Color.Blue,
                            startAngle = -90f,
                            sweepAngle = progress.times(360f).div(3f),
                            useCenter = false,
                            style = Stroke(
                                width = 12f
                            )
                        )
                    })

                    Text(
                        modifier = Modifier.align(Center),
                        text = "$progress/3",
                        color = AppTheme.colors.primary,
                        style = AppTheme.typography.paragraphMono
                    )
                }

                Column(
                    modifier = Modifier.padding(
                        start = AppTheme.dimensions.smallSpacing
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.complete_your_profile),
                        style = AppTheme.typography.caption1,
                        color = Grey400
                    )
                    Text(
                        text = stringResource(id = R.string.buy_crypto_today),
                        style = AppTheme.typography.body2,
                        color = Grey900
                    )
                }
            })
        }

        Spacer(modifier = Modifier.padding(vertical = 32.dp))
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_spacing)))
                CustomStackedIcon(
                    icon = StackedIcon.SmallTag(
                        main = ImageResource.Remote(CryptoCurrency.BTC.logo),
                        tag = ImageResource.Remote(FiatCurrency.Dollars.logo)
                    ),
                    size = 88.dp
                )

                Text(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.smallSpacing,
                    ),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.to_get_started_buy_your_first_btc),
                    style = AppTheme.typography.title2,
                    color = Grey900
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    for (i in 1..3) {
                        Button(
                            content = {
                                Text(
                                    text = "$${i * 100}",
                                    color = Color.White,
                                    style = AppTheme.typography.paragraphMono
                                )
                            },
                            onClick = {},
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Grey800)
                        )
                    }
                }

                MinimalButton(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.standardSpacing
                    ),
                    text = stringResource(id = R.string.other_amount),
                    onClick = {
                    }
                )
            }
        }
    }
}
