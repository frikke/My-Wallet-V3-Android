package piuk.blockchain.android.ui.kyc.veriffsplash

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.commonui.UserIcon

@Composable
fun VeriffSplashScreen(
    viewState: StateFlow<VeriffSplashViewState>,
    onIntent: (VeriffSplashIntent) -> Unit,
    nextClicked: () -> Unit
) {
    val state by viewState.collectAsStateLifecycleAware()

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(state.error) {
            val error = state.error
            if (error != null) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.errorMessage(context),
                    duration = SnackbarDuration.Long
                )
                onIntent(VeriffSplashIntent.ErrorHandled)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.background)
                .padding(padding)
                .padding(all = AppTheme.dimensions.standardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserIcon(
                modifier = Modifier.padding(top = AppTheme.dimensions.xHugeSpacing),
                iconRes = R.drawable.ic_identification_filled
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.largeSpacing),
                text = stringResource(com.blockchain.stringResources.R.string.kyc_veriff_splash_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.tinySpacing),
                text = stringResource(com.blockchain.stringResources.R.string.kyc_veriff_splash_subtitle),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            Column(
                modifier = Modifier
                    .padding(top = AppTheme.dimensions.xLargeSpacing)
                    .padding(vertical = AppTheme.dimensions.verySmallSpacing),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.standardSpacing)
            ) {
                state.supportedDocuments.forEach { document ->
                    val documentName = when (document) {
                        SupportedDocuments.PASSPORT -> stringResource(
                            com.blockchain.stringResources.R.string.kyc_veriff_splash_passport
                        )
                        SupportedDocuments.DRIVING_LICENCE -> stringResource(
                            com.blockchain.stringResources.R.string.kyc_veriff_splash_drivers_license
                        )
                        SupportedDocuments.NATIONAL_IDENTITY_CARD -> stringResource(
                            com.blockchain.stringResources.R.string.kyc_veriff_splash_id_card
                        )
                        SupportedDocuments.RESIDENCE_PERMIT ->
                            stringResource(com.blockchain.stringResources.R.string.kyc_veriff_splash_residence_permit)
                    }

                    SimpleText(
                        modifier = Modifier.fillMaxWidth(),
                        text = "• $documentName",
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(com.blockchain.stringResources.R.string.kyc_veriff_splash_continue),
                state = state.continueButtonState,
                onClick = {
                    nextClicked()
                    onIntent(VeriffSplashIntent.ContinueClicked)
                }
            )
        }
    }
}

private fun VeriffSplashError.errorMessage(context: Context) = when (this) {
    VeriffSplashError.Generic -> context.getString(
        com.blockchain.stringResources.R.string.kyc_veriff_splash_verification_error
    )
}

@Preview
@Composable
private fun PreviewVeriffSplashScreen() {
    val viewState = MutableStateFlow(
        VeriffSplashViewState(
            isLoading = true,
            supportedDocuments = SupportedDocuments.values().toSortedSet(),
            error = VeriffSplashError.Generic,
            continueButtonState = ButtonState.Disabled
        )
    )
    VeriffSplashScreen(
        viewState = viewState,
        onIntent = {},
        nextClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewVeriffSplashScreenDark() {
    PreviewVeriffSplashScreen()
}
