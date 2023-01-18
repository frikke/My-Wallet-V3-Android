package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.home.presentation.R

@Composable
fun NonCustodialEmptyStateCard(
    onReceiveClicked: () -> Unit,
) {
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
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = AppTheme.dimensions.smallSpacing
                    )
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_spacing)))
                Text(
                    modifier = Modifier
                        .padding(
                            bottom = AppTheme.dimensions.smallSpacing
                        ),
                    text = "📥",
                    style = AppTheme.typography.display,
                )

                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.nc_empty_state_title),
                    style = AppTheme.typography.title2,
                    color = Grey900
                )

                Text(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.smallSpacing,
                    ),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.transfer_from_your_trading_account),
                    style = AppTheme.typography.paragraph1,
                    color = Grey700
                )

                PrimaryButton(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.standardSpacing
                    ),
                    text = stringResource(id = R.string.nc_empty_state_cta),
                    onClick = {
                        onReceiveClicked()
                    }
                )
            }
        }
    }
}
