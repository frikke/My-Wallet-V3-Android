package piuk.blockchain.android.maintenance.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey900

/**
 * Figma: https://www.figma.com/file/Khjv2OKUvZ7xwTx2qmSadw/iOS---Upgrade-Prompts?node-id=0%3A1
 */
@Composable
fun AppMaintenanceScreen(appMaintenanceViewState: AppMaintenanceViewState, viewModel: AppMaintenanceViewModel) {
    with(appMaintenanceViewState) {
        Box {
            Image(
                modifier = Modifier.fillMaxSize(),
                imageResource = ImageResource.Local(
                    com.blockchain.componentlib.R.drawable.background_gradient
                ),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = dimensionResource(R.dimen.standard_margin),
                        end = dimensionResource(R.dimen.standard_margin),
                        top = dimensionResource(R.dimen.standard_margin),
                        bottom = dimensionResource(R.dimen.large_margin),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    imageResource = ImageResource.Local(
                        R.drawable.ic_blockchain_logo_with_text
                    )
                )

                Image(
                    imageResource = ImageResource.Local(uiState.image)
                )

                Text(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.title3,
                    color = Grey900,
                    text = stringResource(id = uiState.title),
                )

                Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))

                Text(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                    style = AppTheme.typography.body1,
                    color = Grey900,
                    textAlign = TextAlign.Center,
                    text = stringResource(id = uiState.description)
                )

                Spacer(modifier = Modifier.weight(1f))

                uiState.button1?.let { buttonSettings ->
                    TertiaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = buttonSettings.buttonText),
                        onClick = { viewModel.onIntent(buttonSettings.intent) },
                    )
                }

                if (uiState.button1 != null && uiState.button2 != null) {
                    Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))
                }

                uiState.button2?.let { buttonSettings ->
                    PrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = buttonSettings.buttonText),
                        onClick = { viewModel.onIntent(buttonSettings.intent) }
                    )
                }
            }
        }
    }
}
