package piuk.blockchain.android.ui.kyc.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.commonui.UserIcon

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KycProfileScreen(
    viewState: StateFlow<KycProfileViewState>,
    isSavingProfileLoadingOverride: Boolean,
    onIntent: (KycProfileIntent) -> Unit,
    showDatePicker: () -> Unit
) {
    val state by viewState.collectAsStateLifecycleAware()

    val keyboardController = LocalSoftwareKeyboardController.current
    val localFocusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(state.error) {
            val error = state.error
            if (error != null) {
                keyboardController?.hide()
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.errorMessage(context),
                    duration = SnackbarDuration.Long
                )
                onIntent(KycProfileIntent.ErrorHandled)
            }
        }

        Column(
            modifier = Modifier
                .background(AppColors.background)
                .padding(padding)
                .padding(all = AppTheme.dimensions.standardSpacing)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserIcon(
                modifier = Modifier.padding(top = AppTheme.dimensions.xHugeSpacing),
                iconRes = R.drawable.ic_bank_user
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.largeSpacing),
                text = stringResource(com.blockchain.stringResources.R.string.verify_your_identity),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.tinySpacing),
                text = stringResource(com.blockchain.stringResources.R.string.kyc_profile_message),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            OutlinedTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.standardSpacing),
                state = if (state.isNameInputErrorShowing) {
                    TextInputState.Error(null)
                } else {
                    TextInputState.Default(null)
                },
                singleLine = true,
                value = state.firstNameInput,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                label = stringResource(com.blockchain.stringResources.R.string.kyc_profile_first_name_hint),
                onValueChange = { value -> onIntent(KycProfileIntent.FirstNameInputChanged(value)) }
            )

            OutlinedTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.smallSpacing),
                state = if (state.isNameInputErrorShowing) {
                    TextInputState.Error(
                        stringResource(com.blockchain.stringResources.R.string.kyc_profile_error_invalid_name)
                    )
                } else {
                    TextInputState.Default(null)
                },
                singleLine = true,
                value = state.lastNameInput,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onNext = {
                    keyboardController?.hide()
                    localFocusManager.clearFocus(force = true)
                    showDatePicker()
                }),
                label = stringResource(com.blockchain.stringResources.R.string.kyc_profile_last_name_hint),
                onValueChange = { value -> onIntent(KycProfileIntent.LastNameInputChanged(value)) }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.smallSpacing)
            ) {
                val dobDisplayFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
                val dob = state.dateOfBirthInput?.let { dobDisplayFormat.format(it.time) }.orEmpty()
                OutlinedTextInput(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = dob,
                    label = stringResource(com.blockchain.stringResources.R.string.kyc_profile_dob_hint),
                    readOnly = true,
                    onValueChange = {
                        // no op readonly
                    }
                )

                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(onClick = showDatePicker)
                )
            }

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.standardSpacing),
                text = stringResource(com.blockchain.stringResources.R.string.kyc_profile_next),
                state = if (isSavingProfileLoadingOverride) ButtonState.Loading else state.continueButtonState,
                onClick = { onIntent(KycProfileIntent.ContinueClicked) }
            )
        }
    }
}

private fun KycProfileError.errorMessage(context: Context) = when (this) {
    is KycProfileError.Generic -> message?.ifEmpty {
        null
    } ?: context.getString(com.blockchain.stringResources.R.string.kyc_profile_error)
    KycProfileError.UserConflict -> context.getString(
        com.blockchain.stringResources.R.string.kyc_profile_error_conflict
    )
    KycProfileError.InvalidName -> context.getString(
        com.blockchain.stringResources.R.string.kyc_profile_error_invalid_name
    )
}
