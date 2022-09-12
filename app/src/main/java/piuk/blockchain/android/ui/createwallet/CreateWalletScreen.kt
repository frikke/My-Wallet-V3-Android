package piuk.blockchain.android.ui.createwallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.domain.eligibility.model.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.PasswordStrengthView
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY
import piuk.blockchain.android.util.StringUtils

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateWalletScreen(
    viewState: StateFlow<CreateWalletViewState>,
    onIntent: (CreateWalletIntent) -> Unit,
    showCountryBottomSheet: (CountryInputState.Loaded) -> Unit,
    showStateBottomSheet: (StateInputState.Loaded) -> Unit,
) {
    val state by viewState.collectAsStateLifecycleAware()

    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(state.error) {
            val error = state.error
            if (error != null) {
                when (error) {
                    is CreateWalletError.Unknown,
                    CreateWalletError.RecaptchaFailed,
                    CreateWalletError.WalletCreationFailed -> {
                        keyboardController?.hide()
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = error.errorMessage(context),
                            duration = SnackbarDuration.Long,
                            actionLabel = context.getString(R.string.common_retry)
                        )
                        onIntent(CreateWalletIntent.ErrorHandled)
                    }
                }
            }
        }

        Column(
            Modifier
                .background(Color.White)
                .padding(padding)
                .fillMaxWidth()
        ) {
            NavigationBar(
                title = "",
                onBackButtonClick = { onIntent(CreateWalletIntent.BackClicked) },
                navigationBarButtons = listOf(
                    NavigationBarButton.Text(
                        stringResource(R.string.common_next),
                        if (state.nextButtonState == ButtonState.Enabled) Blue600 else Blue400
                    ) {
                        if (state.nextButtonState == ButtonState.Enabled) {
                            onIntent(CreateWalletIntent.NextClicked)
                        }
                    }
                )
            )

            when (state.step) {
                CreateWalletStep.REGION_AND_REFERRAL -> RegionAndReferralStep(
                    state,
                    onIntent,
                    showCountryBottomSheet,
                    showStateBottomSheet,
                )
                CreateWalletStep.EMAIL_AND_PASSWORD -> EmailAndPasswordStep(state, onIntent)
            }

            Spacer(Modifier.weight(1f))

            val ctaText = when (state.step) {
                CreateWalletStep.REGION_AND_REFERRAL -> R.string.common_next
                CreateWalletStep.EMAIL_AND_PASSWORD -> R.string.create_wallet_create_account
            }
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTheme.dimensions.smallSpacing,
                        vertical = AppTheme.dimensions.standardSpacing,
                    ),
                text = stringResource(ctaText),
                state = state.nextButtonState,
                onClick = {
                    onIntent(CreateWalletIntent.NextClicked)
                },
            )
        }
    }
}

@Composable
private fun RegionAndReferralStep(
    state: CreateWalletViewState,
    onIntent: (CreateWalletIntent) -> Unit,
    showCountryBottomSheet: (CountryInputState.Loaded) -> Unit,
    showStateBottomSheet: (StateInputState.Loaded) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.padding(top = AppTheme.dimensions.hugeSpacing),
                imageResource = ImageResource.Local(R.drawable.ic_world_blue),
            )
        }
        SimpleText(
            modifier = Modifier.padding(top = AppTheme.dimensions.standardSpacing),
            text = stringResource(R.string.create_wallet_step_1_header),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        SimpleText(
            modifier = Modifier.padding(
                top = AppTheme.dimensions.tinySpacing,
                bottom = AppTheme.dimensions.standardSpacing
            ),
            text = stringResource(R.string.create_wallet_step_1_subheader),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        val countryFocusRequester = remember { FocusRequester() }
        val countryInputIcon =
            if (state.countryInputState is CountryInputState.Loading) ImageResource.None
            else ImageResource.Local(R.drawable.ic_arrow_down)
        Box {
            OutlinedTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(countryFocusRequester),
                value = (state.countryInputState as? CountryInputState.Loaded)?.selected?.name.orEmpty(),
                readOnly = true,
                placeholder = stringResource(R.string.create_wallet_country),
                focusedTrailingIcon = countryInputIcon,
                unfocusedTrailingIcon = countryInputIcon,
                onValueChange = {
                    // no op readonly
                }
            )
            if (state.countryInputState is CountryInputState.Loading) {
                CircularProgressBar(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(24.dp)
                )
            }

            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        enabled = state.countryInputState is CountryInputState.Loaded
                    ) {
                        countryFocusRequester.requestFocus()
                        if (state.countryInputState is CountryInputState.Loaded) {
                            showCountryBottomSheet(state.countryInputState)
                        }
                    }
            )
        }

        if (state.stateInputState !is StateInputState.Hidden) {
            val stateFocusRequester = remember { FocusRequester() }
            val stateInputIcon =
                if (state.stateInputState is StateInputState.Loading) ImageResource.None
                else ImageResource.Local(R.drawable.ic_arrow_down)
            Box {
                OutlinedTextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(stateFocusRequester),
                    value = (state.stateInputState as? StateInputState.Loaded)?.selected?.name.orEmpty(),
                    readOnly = true,
                    label = stringResource(R.string.create_wallet_state),
                    placeholder = stringResource(R.string.state_not_selected),
                    focusedTrailingIcon = stateInputIcon,
                    unfocusedTrailingIcon = stateInputIcon,
                    onValueChange = {
                        // no op readonly
                    }
                )
                if (state.stateInputState is StateInputState.Loading) {
                    CircularProgressBar(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                }

                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(
                            enabled = state.stateInputState is StateInputState.Loaded
                        ) {
                            stateFocusRequester.requestFocus()
                            if (state.stateInputState is StateInputState.Loaded) {
                                showStateBottomSheet(state.stateInputState)
                            }
                        }
                )
            }
        }

        val referralInputState = if (state.isInvalidReferralErrorShowing) {
            TextInputState.Error(stringResource(R.string.new_account_referral_code_invalid))
        } else {
            TextInputState.Default(null)
        }
        val trailingIcon =
            if (state.referralCodeInput.isNotEmpty()) ImageResource.Local(R.drawable.ic_close_circle)
            else ImageResource.None
        OutlinedTextInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.standardSpacing),
            value = state.referralCodeInput,
            label = stringResource(R.string.new_account_referral_code_label),
            placeholder = stringResource(R.string.new_account_referral_code),
            focusedTrailingIcon = trailingIcon,
            unfocusedTrailingIcon = trailingIcon,
            singleLine = true,
            onTrailingIconClicked = {
                onIntent(CreateWalletIntent.ReferralInputChanged(""))
            },
            state = referralInputState,
            onValueChange = {
                onIntent(CreateWalletIntent.ReferralInputChanged(it))
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EmailAndPasswordStep(
    state: CreateWalletViewState,
    onIntent: (CreateWalletIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val emailFocusRequester = FocusRequester()
        val passwordFocusRequester = FocusRequester()

        LaunchedEffect(state.isShowingInvalidEmailError) {
            if (state.isShowingInvalidEmailError) emailFocusRequester.requestFocus()
        }
        LaunchedEffect(state.passwordInputError) {
            if (state.passwordInputError != null) passwordFocusRequester.requestFocus()
        }

        SimpleText(
            modifier = Modifier.padding(top = AppTheme.dimensions.standardSpacing),
            text = stringResource(R.string.create_wallet_step_2_header),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        SimpleText(
            modifier = Modifier.padding(
                top = AppTheme.dimensions.tinySpacing, bottom = AppTheme.dimensions.standardSpacing
            ),
            text = stringResource(R.string.create_wallet_step_2_subheader),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        val emailTextState =
            if (state.isShowingInvalidEmailError) TextInputState.Error(stringResource(R.string.invalid_email))
            else TextInputState.Default(null)
        OutlinedTextInput(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester),
            value = state.emailInput,
            label = stringResource(R.string.sign_up_email),
            state = emailTextState,
            placeholder = stringResource(R.string.create_wallet_email_hint),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
            onValueChange = {
                onIntent(CreateWalletIntent.EmailInputChanged(it))
            }
        )

        var isPasswordVisible by remember { mutableStateOf(false) }
        val trailingIconRes = if (isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility_on
        val keyboardController = LocalSoftwareKeyboardController.current
        val passwordTextState = when (state.passwordInputError) {
            CreateWalletPasswordError.InvalidPasswordTooLong ->
                TextInputState.Error(stringResource(R.string.invalid_password))
            CreateWalletPasswordError.InvalidPasswordTooShort ->
                TextInputState.Error(stringResource(R.string.invalid_password_too_short))
            CreateWalletPasswordError.InvalidPasswordTooWeak ->
                TextInputState.Error(stringResource(R.string.weak_password))
            null -> TextInputState.Default(null)
        }
        OutlinedTextInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.standardSpacing)
                .focusRequester(passwordFocusRequester),
            value = state.passwordInput,
            label = stringResource(R.string.password),
            state = passwordTextState,
            placeholder = stringResource(R.string.create_wallet_password_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            focusedTrailingIcon = ImageResource.Local(trailingIconRes),
            unfocusedTrailingIcon = ImageResource.Local(trailingIconRes),
            singleLine = true,
            onTrailingIconClicked = {
                isPasswordVisible = !isPasswordVisible
            },
            onValueChange = {
                onIntent(CreateWalletIntent.PasswordInputChanged(it))
            }
        )

        var isPasswordStrengthVisible by remember { mutableStateOf(false) }
        if (!isPasswordStrengthVisible && state.passwordInput.isNotEmpty()) {
            isPasswordStrengthVisible = true
        }
        if (isPasswordStrengthVisible) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.smallestSpacing,
                        start = AppTheme.dimensions.tinySpacing,
                        end = AppTheme.dimensions.tinySpacing,
                    ),
                factory = { context ->
                    PasswordStrengthView(context, null)
                },
                update = {
                    it.updatePassword(state.passwordInput)
                }
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.standardSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                state = if (state.areTermsOfServiceChecked) CheckboxState.Checked else CheckboxState.Unchecked,
                onCheckChanged = { isChecked ->
                    onIntent(CreateWalletIntent.TermsOfServiceStateChanged(isChecked))
                }
            )

            val context = LocalContext.current
            val linksMap = mapOf(
                "terms" to URL_TOS_POLICY,
                "privacy" to URL_PRIVACY_POLICY
            )
            val disclaimerText = StringUtils.getAnnotatedStringWithMappedAnnotations(
                context,
                R.string.password_disclaimer,
                linksMap
            )
            SimpleText(
                modifier = Modifier.padding(start = AppTheme.dimensions.tinySpacing),
                text = disclaimerText,
                style = ComposeTypographies.Micro2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                onAnnotationClicked = { tag, value ->
                    if (tag == StringUtils.TAG_URL) {
                        Intent(Intent.ACTION_VIEW, Uri.parse(value))
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            .also { context.startActivity(it) }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_RegionAndReferral() {
    val state = CreateWalletViewState(
        step = CreateWalletStep.REGION_AND_REFERRAL,
        emailInput = "test@blockchain.com",
        isShowingInvalidEmailError = true,
        passwordInput = "Somepassword",
        passwordInputError = CreateWalletPasswordError.InvalidPasswordTooShort,
        countryInputState = CountryInputState.Loaded(countries = countries, selected = countries[1]),
        stateInputState = StateInputState.Loading,
        areTermsOfServiceChecked = false,
        referralCodeInput = "12345678",
        isInvalidReferralErrorShowing = true,
        isCreateWalletLoading = false,
        nextButtonState = ButtonState.Disabled,
        error = null
    )
    CreateWalletScreen(
        viewState = MutableStateFlow(state),
        onIntent = {},
        showCountryBottomSheet = {},
        showStateBottomSheet = {},
    )
}

@Preview
@Composable
private fun Preview_EmailAndPassword() {
    val state = CreateWalletViewState(
        step = CreateWalletStep.EMAIL_AND_PASSWORD,
        emailInput = "test@blockchain.com",
        isShowingInvalidEmailError = true,
        passwordInput = "Somepassword",
        passwordInputError = CreateWalletPasswordError.InvalidPasswordTooShort,
        countryInputState = CountryInputState.Loaded(countries = countries, selected = countries[1]),
        stateInputState = StateInputState.Loading,
        areTermsOfServiceChecked = false,
        referralCodeInput = "12345678",
        isInvalidReferralErrorShowing = false,
        isCreateWalletLoading = true,
        nextButtonState = ButtonState.Loading,
        error = null
    )
    CreateWalletScreen(
        viewState = MutableStateFlow(state),
        onIntent = {},
        showCountryBottomSheet = {},
        showStateBottomSheet = {},
    )
}

private val states = listOf(
    Region.State("US", "Arkansas", true, "AK"),
    Region.State("US", "New York", true, "NY"),
)
private val countries = listOf(
    Region.Country("PT", "Portugal", true, emptyList()),
    Region.Country("US", "United States of America", true, states.map { it.stateCode }),
)

private fun CreateWalletError.errorMessage(context: Context): String = when (this) {
    CreateWalletError.WalletCreationFailed -> context.getString(R.string.hd_error)
    CreateWalletError.RecaptchaFailed -> context.getString(R.string.recaptcha_failed)
    is CreateWalletError.Unknown -> this.message ?: context.getString(R.string.something_went_wrong_try_again)
}
