package com.blockchain.kyc.email

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.anim.AnimatedStateContent
import com.blockchain.componentlib.anim.AnimatedStateIndicatorImage
import com.blockchain.componentlib.anim.TargetState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Email
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.SuccessTag
import com.blockchain.componentlib.tag.WarningTag
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.openEmailClient
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import kotlinx.coroutines.delay
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EmailVerification(
    verificationRequired: Boolean,
    viewModel: EmailVerificationViewModel = getViewModel(
        scope = payloadScope,
        key = verificationRequired.toString(),
        parameters = { parametersOf(verificationRequired) }
    ),
    legacyToolbar: Boolean = false,
    closeOnClick: () -> Unit,
    nextOnClick: () -> Unit
) {
    val viewState by viewModel.viewState.collectAsStateLifecycleAware()

    EmailVerificationScreen(
        email = viewState.email,
        status = viewState.status,
        showResendingEmailInProgress = viewState.showResendingEmailInProgress,
        snackbarMessage = viewState.snackbarMessage,
        legacyToolbar = legacyToolbar,
        resendEmailClicked = {
            viewModel.onIntent(EmailVerificationIntent.ResendEmailClicked)
        },
        closeOnClick = closeOnClick,
        nextOnClick = nextOnClick
    )
}

@Composable
private fun EmailVerificationScreen(
    email: String,
    status: EmailVerificationStatus,
    showResendingEmailInProgress: Boolean,
    snackbarMessage: EmailVerificationNotification?,
    legacyToolbar: Boolean = false,
    resendEmailClicked: () -> Unit,
    closeOnClick: () -> Unit,
    nextOnClick: () -> Unit
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let {
                when (snackbarMessage) {
                    EmailVerificationNotification.EmailSent -> {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.email_verification_resend_snackbar,
                                email
                            ),
                            duration = SnackbarDuration.Short
                        )
                    }

                    is EmailVerificationNotification.Error -> {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = snackbarMessage.error.errorMessage(context),
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
        ) {

            if (!legacyToolbar) {
                TinyVerticalSpacer()

                SheetFlatHeader(
                    icon = StackedIcon.None,
                    title = "",
                    onCloseClick = closeOnClick
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.dimensions.smallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1F))

                AnimatedStateIndicatorImage(
                    imageResource = Icons.Filled.Email,
                    state = status.toTargetState()
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

                AnimatedStateContent(
                    state = status.toTargetState(),
                    defaultContent = {
                        VerificationInProgress(email = email)
                    },
                    successContent = {
                        VerificationSuccess(email = email)
                    },
                    failureContent = {
                        VerificationFailed(email = email)
                    }
                )

                Spacer(modifier = Modifier.weight(2.5F))

                when (status) {
                    EmailVerificationStatus.Default -> {

                        PrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeightIn(min = 60.dp),
                            text = stringResource(R.string.check_my_inbox),
                            onClick = { context.openEmailClient() }
                        )

                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                        MinimalPrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeightIn(min = 60.dp),
                            text = stringResource(R.string.resend_email),
                            state = if (showResendingEmailInProgress) ButtonState.Loading else ButtonState.Enabled,
                            onClick = resendEmailClicked
                        )
                    }

                    EmailVerificationStatus.Success -> {
                        PrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeightIn(min = 60.dp),
                            text = stringResource(R.string.common_next),
                            onClick = nextOnClick
                        )
                    }

                    EmailVerificationStatus.Error -> {
                        PrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeightIn(min = 60.dp),
                            text = stringResource(R.string.check_my_inbox),
                            onClick = { context.openEmailClient() }
                        )

                        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                        MinimalPrimaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeightIn(min = 60.dp),
                            text = stringResource(R.string.resend_email),
                            state = if (showResendingEmailInProgress) ButtonState.Loading else ButtonState.Enabled,
                            onClick = resendEmailClicked
                        )
                    }
                }
            }
        }
    }
}

private fun EmailVerificationStatus.toTargetState() =
    when (this) {
        EmailVerificationStatus.Default -> TargetState.DEFAULT
        EmailVerificationStatus.Error -> TargetState.FAILURE
        EmailVerificationStatus.Success -> TargetState.SUCCESS
    }

@Composable
private fun ColumnScope.VerificationInProgress(
    email: String,
) {
    Text(
        text = stringResource(R.string.email_verify),
        style = AppTheme.typography.title3,
        color = AppColors.title
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

    Text(
        text = stringResource(R.string.email_verification_subtitle),
        style = AppTheme.typography.body1,
        color = AppColors.body,
        textAlign = TextAlign.Center
    )

    Text(
        text = email,
        style = AppTheme.typography.body2,
        color = AppColors.title,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

    WarningTag(text = stringResource(R.string.not_verified))
}

@Composable
private fun ColumnScope.VerificationSuccess(email: String) {
    Text(
        text = stringResource(R.string.email_verified),
        style = AppTheme.typography.title3,
        color = AppColors.title
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

    Text(
        text = stringResource(R.string.success_email_veriff),
        style = AppTheme.typography.body1,
        color = AppColors.body,
        textAlign = TextAlign.Center
    )

    Text(
        text = email,
        style = AppTheme.typography.body2,
        color = AppColors.title,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

    SuccessTag(text = stringResource(R.string.verified))
}

@Composable
private fun ColumnScope.VerificationFailed(
    email: String
) {
    Text(
        text = stringResource(R.string.error_email_veriff_title),
        style = AppTheme.typography.title3,
        color = AppColors.title
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

    Text(
        text = stringResource(R.string.error_email_veriff),
        style = AppTheme.typography.body1,
        color = AppColors.body,
        textAlign = TextAlign.Center
    )

    Text(
        text = email,
        style = AppTheme.typography.body2,
        color = AppColors.title,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

    WarningTag(text = stringResource(R.string.not_verified))
}

@Composable
private fun VerificationStatusIcon(
    status: EmailVerificationStatus,
    legacyBackground: Boolean = false,
) {
    CustomStackedIcon(
        icon = when (status) {
            EmailVerificationStatus.Default,
            EmailVerificationStatus.Error -> {
                StackedIcon.SingleIcon(
                    Icons.Filled.Email.withSize(58.dp).withTint(AppColors.title),
                )
            }

            EmailVerificationStatus.Success -> {
                StackedIcon.SmallTag(
                    Icons.Filled.Email.withSize(58.dp).withTint(AppColors.title),
                    Icons.Filled.Check.withTint(AppColors.success)
                )
            }
        },
        size = 88.dp,
        tagIconSize = 44.dp,
        iconBackground = if (legacyBackground) AppColors.background else AppColors.backgroundSecondary,
        borderColor = if (legacyBackground) AppColors.backgroundSecondary else AppTheme.colors.background
    )
}

@Composable
private fun VerificationStatusTag(
    status: EmailVerificationStatus
) {
    when (status) {
        EmailVerificationStatus.Default,
        EmailVerificationStatus.Error -> {
            WarningTag(text = stringResource(R.string.not_verified))
        }

        EmailVerificationStatus.Success -> {
            SuccessTag(text = stringResource(R.string.verified))
        }
    }
}

@Composable
private fun EmailVerificationStatus.title(): String = when (this) {
    EmailVerificationStatus.Default -> stringResource(R.string.email_verify)
    EmailVerificationStatus.Error -> stringResource(R.string.error_email_veriff_title)
    EmailVerificationStatus.Success -> stringResource(R.string.email_verified)
}

@Composable
private fun EmailVerificationStatus.subtitle(): String = when (this) {
    EmailVerificationStatus.Default -> stringResource(R.string.email_verification_subtitle)
    EmailVerificationStatus.Error -> stringResource(R.string.error_email_veriff)
    EmailVerificationStatus.Success -> stringResource(R.string.success_email_veriff)
}

private fun EmailVerificationError.errorMessage(context: Context): String = when (this) {
    is EmailVerificationError.Generic -> this.message ?: context.getString(
        R.string.error_email_veriff
    )

    EmailVerificationError.TooManyResendAttempts -> context.getString(
        R.string.error_email_veriff_too_many_attempts
    )
}

@Preview
@Composable
private fun PreviewEmailVerificationScreen() {

    var status by remember { mutableStateOf(EmailVerificationStatus.Default) }
    LaunchedEffect(Unit) {
        // change status every 1500ms
        while (true) {
            delay(2000)
            status = EmailVerificationStatus.Success
            delay(2000)
            status = EmailVerificationStatus.Default
            delay(2000)
            status = EmailVerificationStatus.Error
            delay(2000)
            status = EmailVerificationStatus.Default
        }
    }

    EmailVerificationScreen(
        email = "johnsmith@gmail.com",
        status = status,
        showResendingEmailInProgress = false,
        snackbarMessage = null,
        resendEmailClicked = {},
        closeOnClick = {
            status = when (status) {
                EmailVerificationStatus.Default -> EmailVerificationStatus.Error
                EmailVerificationStatus.Error -> EmailVerificationStatus.Success
                EmailVerificationStatus.Success -> EmailVerificationStatus.Default
            }
        },
        nextOnClick = {
        }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEmailVerificationScreenDark() {
    PreviewEmailVerificationScreen()
}

@Preview
@Composable
private fun PreviewEmailVerificationScreenLegacy() {
    EmailVerificationScreen(
        email = "johnsmith@gmail.com",
        status = EmailVerificationStatus.Default,
        showResendingEmailInProgress = false,
        snackbarMessage = null,
        resendEmailClicked = {},
        closeOnClick = {},
        nextOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEmailVerificationScreenLegacyDark() {
    PreviewEmailVerificationScreenLegacy()
}

@Preview
@Composable
private fun PreviewEmailVerificationScreenSuccess() {
    EmailVerificationScreen(
        email = "johnsmith@gmail.com",
        status = EmailVerificationStatus.Success,
        showResendingEmailInProgress = false,
        snackbarMessage = null,
        resendEmailClicked = {},
        closeOnClick = {},
        nextOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEmailVerificationScreenSuccessDark() {
    PreviewEmailVerificationScreenSuccess()
}
