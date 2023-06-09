package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.commonui.UserIcon

@Composable
fun EmailVerificationScreen(
    viewState: StateFlow<EmailVerificationViewState>,
    onIntent: (EmailVerificationIntent) -> Unit,
    openInbox: () -> Unit,
    openResendOrChangeSheet: () -> Unit
) {
    val state by viewState.collectAsStateLifecycleAware()

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(state.error, state.showResendEmailConfirmation) {
            val error = state.error
            val showResendEmailConfirmation = state.showResendEmailConfirmation
            if (error != null) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.errorMessage(context),
                    duration = SnackbarDuration.Long
                )
            } else if (showResendEmailConfirmation) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = context.getString(
                        com.blockchain.stringResources.R.string.email_verification_resend_snackbar,
                        state.email
                    ),
                    duration = SnackbarDuration.Short
                )
                onIntent(EmailVerificationIntent.ShowResendEmailConfirmationHandled)
            }
        }

        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(padding)
                .padding(all = AppTheme.dimensions.standardSpacing)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val assets = when {
                state.isVerified -> Assets.VerifiedEmail()
                state.error != null -> Assets.Error()
                else -> Assets.UnverifiedEmail(state.email.orEmpty())
            }

            UserIcon(
                modifier = Modifier.padding(top = AppTheme.dimensions.xHugeSpacing),
                iconRes = R.drawable.ic_verify_email,
                statusIconRes = assets.statusIcon()
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.largeSpacing),
                text = assets.title(),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.tinySpacing),
                text = assets.subtitle(),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            Spacer(Modifier.weight(1f))

            assets.primaryCta()?.let { text ->
                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppTheme.dimensions.smallSpacing),
                    text = text,
                    onClick = openInbox
                )
            }
            assets.secondaryCta()?.let { text ->
                MinimalPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    onClick = {
                        onIntent(EmailVerificationIntent.StopPollingForVerification)
                        openResendOrChangeSheet()
                    }
                )
            }
        }
    }
}

private sealed class Assets(
    val statusIcon: @Composable () -> Int?,
    val title: @Composable () -> String,
    val subtitle: @Composable () -> String,
    val primaryCta: @Composable () -> String?,
    val secondaryCta: @Composable () -> String?
) {
    class UnverifiedEmail(
        private val email: String
    ) : Assets(
        statusIcon = { null },
        title = { stringResource(com.blockchain.stringResources.R.string.email_verify) },
        subtitle = { stringResource(com.blockchain.stringResources.R.string.email_verification_title, email) },
        primaryCta = { stringResource(com.blockchain.stringResources.R.string.check_my_inbox) },
        secondaryCta = { stringResource(com.blockchain.stringResources.R.string.did_not_get_email) }
    )

    class VerifiedEmail : Assets(
        statusIcon = { R.drawable.ic_check_circle },
        title = { stringResource(com.blockchain.stringResources.R.string.email_verified) },
        subtitle = { stringResource(com.blockchain.stringResources.R.string.success_email_veriff) },
        primaryCta = { null },
        secondaryCta = { null }
    )

    class Error : Assets(
        statusIcon = { R.drawable.ic_alert_white_bkgd },
        title = { stringResource(com.blockchain.stringResources.R.string.error_email_veriff_title) },
        subtitle = { stringResource(com.blockchain.stringResources.R.string.error_email_veriff) },
        primaryCta = { stringResource(com.blockchain.stringResources.R.string.check_my_inbox) },
        secondaryCta = { stringResource(com.blockchain.stringResources.R.string.did_not_get_email) }
    )
}

@Preview
@Composable
private fun PreviewUnverified() {
    val state = EmailVerificationViewState(
        email = "somerandomemail@email.com",
        isVerified = false,
        showResendEmailConfirmation = true,
        error = null
    )
    EmailVerificationScreen(
        viewState = MutableStateFlow(state),
        onIntent = {},
        openInbox = {},
        openResendOrChangeSheet = {}
    )
}

@Preview
@Composable
private fun PreviewVerified() {
    val state = EmailVerificationViewState(
        email = "somerandomemail@email.com",
        isVerified = true,
        showResendEmailConfirmation = false,
        error = null
    )
    EmailVerificationScreen(
        viewState = MutableStateFlow(state),
        onIntent = {},
        openInbox = {},
        openResendOrChangeSheet = {}
    )
}

@Preview
@Composable
private fun PreviewError() {
    val state = EmailVerificationViewState(
        email = "somerandomemail@email.com",
        isVerified = false,
        showResendEmailConfirmation = false,
        error = EmailVerificationError.TooManyResendAttempts
    )
    EmailVerificationScreen(
        viewState = MutableStateFlow(state),
        onIntent = {},
        openInbox = {},
        openResendOrChangeSheet = {}
    )
}

private fun EmailVerificationError.errorMessage(context: Context): String = when (this) {
    is EmailVerificationError.Generic -> this.message ?: context.getString(
        com.blockchain.stringResources.R.string.error_email_veriff
    )
    EmailVerificationError.TooManyResendAttempts -> context.getString(
        com.blockchain.stringResources.R.string.error_email_veriff_too_many_attempts
    )
}
