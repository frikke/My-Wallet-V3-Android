package piuk.blockchain.android.maintenance.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

/**
 * Figma: https://www.figma.com/file/Khjv2OKUvZ7xwTx2qmSadw/iOS---Upgrade-Prompts?node-id=0%3A1
 */
@Composable
fun AppMaintenanceScreen(
    isDebugBuild: Boolean,
    debugSkip: () -> Unit,
    uiState: AppMaintenanceViewState,
    button1OnClick: (AppMaintenanceIntents) -> Unit,
    button2OnClick: (AppMaintenanceIntents) -> Unit
) {
    with(uiState) {
        Box(modifier = Modifier.background(AppColors.background)) {
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
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                        end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                        top = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                        bottom = dimensionResource(com.blockchain.componentlib.R.dimen.large_spacing)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    imageResource = ImageResource.Local(
                        com.blockchain.componentlib.R.drawable.ic_blockchain_logo_with_text
                    )
                )

                image?.let {
                    Image(
                        imageResource = ImageResource.Local(image)
                    )
                }

                Text(
                    modifier = Modifier.padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)
                    ),
                    style = AppTheme.typography.title3,
                    color = AppColors.title,
                    text = stringResource(id = title)
                )

                Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))

                Text(
                    modifier = Modifier.padding(
                        start = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)
                    ),
                    style = AppTheme.typography.body1,
                    color = AppColors.body,
                    textAlign = TextAlign.Center,
                    text = stringResource(id = description)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isDebugBuild) {
                    MinimalPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Debug Build - Click to skip",
                        onClick = debugSkip
                    )
                    Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
                }

                button1?.let { buttonSettings ->
                    MinimalPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = buttonSettings.buttonText),
                        onClick = { button1OnClick(buttonSettings.intent) }
                    )
                }

                if (button1 != null && button2 != null) {
                    Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
                }

                button2?.let { buttonSettings ->
                    PrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = buttonSettings.buttonText),
                        onClick = { button2OnClick(buttonSettings.intent) }
                    )
                }
            }
        }
    }
}

@Preview(name = "NO_STATUS")
@Composable
fun PreviewAppMaintenanceScreenNO_STATUS() {
    AppMaintenanceScreen(isDebugBuild = false, debugSkip = {}, AppMaintenanceViewState.NO_STATUS, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppMaintenanceScreenNO_STATUSDark() {
    PreviewAppMaintenanceScreenNO_STATUS()
}

@Preview(name = "OS_NOT_SUPPORTED")
@Composable
fun PreviewAppMaintenanceScreenOS_NOT_SUPPORTED() {
    AppMaintenanceScreen(isDebugBuild = false, debugSkip = {}, AppMaintenanceViewState.OS_NOT_SUPPORTED, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppMaintenanceScreenOS_NOT_SUPPORTEDDark() {
    PreviewAppMaintenanceScreenOS_NOT_SUPPORTED()
}

@Preview(name = "SITE_WIDE_MAINTENANCE")
@Composable
fun PreviewAppMaintenanceScreenSITE_WIDE_MAINTENANCE() {
    AppMaintenanceScreen(isDebugBuild = false, debugSkip = {}, AppMaintenanceViewState.SITE_WIDE_MAINTENANCE, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppMaintenanceScreenSITE_WIDE_MAINTENANCEDark() {
    PreviewAppMaintenanceScreenSITE_WIDE_MAINTENANCE()
}

@Preview(name = "REDIRECT_TO_WEBSITE")
@Composable
fun PreviewAppMaintenanceScreenREDIRECT_TO_WEBSITE() {
    AppMaintenanceScreen(isDebugBuild = false, debugSkip = {}, AppMaintenanceViewState.REDIRECT_TO_WEBSITE, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppMaintenanceScreenREDIRECT_TO_WEBSITEDark() {
    PreviewAppMaintenanceScreenREDIRECT_TO_WEBSITE()
}

@Preview(name = "MANDATORY_UPDATE")
@Composable
fun PreviewAppMaintenanceScreenMANDATORY_UPDATE() {
    AppMaintenanceScreen(isDebugBuild = false, debugSkip = {}, AppMaintenanceViewState.MANDATORY_UPDATE, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppMaintenanceScreenMANDATORY_UPDATEDark() {
    PreviewAppMaintenanceScreenMANDATORY_UPDATE()
}

@Preview(name = "OPTIONAL_UPDATE")
@Composable
fun PreviewAppMaintenanceScreenOPTIONAL_UPDATE() {
    AppMaintenanceScreen(isDebugBuild = false, debugSkip = {}, AppMaintenanceViewState.OPTIONAL_UPDATE, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppMaintenanceScreenOPTIONAL_UPDATEDark() {
    PreviewAppMaintenanceScreenOPTIONAL_UPDATE()
}