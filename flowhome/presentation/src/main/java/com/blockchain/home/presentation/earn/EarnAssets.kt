package com.blockchain.home.presentation.earn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900

@Preview
@Composable
fun EarnAssets() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.common_earn),
                style = AppTheme.typography.body2,
                color = Grey700
            )

            Spacer(modifier = Modifier.weight(1f))

            /*    Text(
                    modifier = Modifier.clickableNoEffect(onSeeAllCryptoAssetsClick),
                    text = stringResource(R.string.see_all),
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.primary,
                )*/
        }
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 3.dp
        ) {
            TableRow(
                contentStart = {
                    Box {
                        Canvas(
                            modifier = Modifier
                                .size(32.dp)
                                .align(Center),
                            onDraw = {
                                drawCircle(
                                    color = Grey400,
                                )
                            }
                        )
                        Text(
                            modifier = Modifier.align(Center),
                            text = "%",
                            style = AppTheme.typography.body2,
                            color = Color.White,
                        )
                    }
                },
                content = {
                    Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                        Text(
                            text = stringResource(id = R.string.earn_up_to),
                            style = AppTheme.typography.caption2,
                            color = Grey900
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            text = stringResource(id = R.string.put_your_crypto_to_work),
                            style = AppTheme.typography.paragraph1,
                            color = Grey900
                        )
                    }
                },
                contentEnd = {
                    Button(
                        modifier = Modifier
                            .wrapContentWidth(align = End)
                            .weight(1f),
                        content = {
                            Text(
                                text = stringResource(
                                    id = R.string.common_earn
                                ).uppercase(),
                                color = Color.White,
                                style = AppTheme.typography.paragraphMono
                            )
                        },
                        onClick = {
                            /**
                             * TODO Interest dashboard
                             */
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Grey800)
                    )
                }
            )
        }
    }
}
