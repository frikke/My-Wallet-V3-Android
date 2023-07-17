package piuk.blockchain.android.ui.createwallet

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.basic.closeImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.icons.Globe
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.stringResources.R.string
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import piuk.blockchain.android.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateWalletScreen(
    viewState: StateFlow<CreateWalletViewState>,
    onIntent: (CreateWalletIntent) -> Unit,
    showCountryBottomSheet: (CountryInputState.Loaded) -> Unit,
    showStateBottomSheet: (StateInputState.Loaded) -> Unit
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
                    CreateWalletError.RecaptchaFailed -> {
                        keyboardController?.hide()
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = error.errorMessage(context),
                            duration = SnackbarDuration.Long,
                            actionLabel = context.getString(string.common_retry)
                        )
                        onIntent(CreateWalletIntent.ErrorHandled)
                    }
                }
            }
        }

        Column(
            Modifier
                .background(AppColors.background)
                .padding(padding)
                .fillMaxSize()
        ) {
            NavigationBar(
                modeColor = ModeBackgroundColor.Override(WalletMode.CUSTODIAL),
                title = "",
                onBackButtonClick = { onIntent(CreateWalletIntent.BackClicked) }
            )

            when (state.screen) {
                CreateWalletScreen.REGION_AND_REFERRAL -> RegionAndReferralStep(
                    state,
                    onIntent,
                    showCountryBottomSheet,
                    showStateBottomSheet
                )

                CreateWalletScreen.EMAIL_AND_PASSWORD -> EmailAndPasswordStep(state, onIntent)
                CreateWalletScreen.CREATION_FAILED -> CreateWalletFailed(
                    backOnClick = {
                        onIntent(CreateWalletIntent.BackClicked)
                    }
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RegionAndReferralStep(
    state: CreateWalletViewState,
    onIntent: (CreateWalletIntent) -> Unit,
    showCountryBottomSheet: (CountryInputState.Loaded) -> Unit,
    showStateBottomSheet: (StateInputState.Loaded) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        SmallVerticalSpacer()

        Box(
            modifier = Modifier
                .background(Color.White, CircleShape)
                .size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(58.dp),
                imageResource = Icons.Filled.Globe
            )
        }
        SimpleText(
            modifier = Modifier.padding(top = AppTheme.dimensions.standardSpacing),
            text = stringResource(string.create_wallet_step_1_header),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        SimpleText(
            modifier = Modifier.padding(
                top = AppTheme.dimensions.tinySpacing,
                bottom = AppTheme.dimensions.standardSpacing
            ),
            text = stringResource(string.create_wallet_step_1_subheader),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        val countryFocusRequester = remember { FocusRequester() }
        val countryInputIcon =
            if (state.countryInputState is CountryInputState.Loading) {
                ImageResource.None
            } else ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_arrow_down)
        Box {
            Column {
                SimpleText(
                    text = stringResource(string.create_wallet_country),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                TinyVerticalSpacer()

                OutlinedTextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(countryFocusRequester),
                    value = (state.countryInputState as? CountryInputState.Loaded)?.selected?.name.orEmpty(),
                    readOnly = true,
                    placeholder = stringResource(string.create_wallet_country),
                    focusedTrailingIcon = countryInputIcon,
                    unfocusedTrailingIcon = countryInputIcon,
                    onValueChange = {
                        // no op readonly
                    },
                    shape = AppTheme.shapes.large,
                )
            }
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
            StandardVerticalSpacer()

            val stateFocusRequester = remember { FocusRequester() }
            val stateInputIcon =
                if (state.stateInputState is StateInputState.Loading) {
                    ImageResource.None
                } else ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_arrow_down)

            Box {
                Column {
                    SimpleText(
                        text = stringResource(string.create_wallet_state), style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title, gravity = ComposeGravities.Centre
                    )

                    TinyVerticalSpacer()

                    OutlinedTextInput(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(stateFocusRequester),
                        value = (state.stateInputState as? StateInputState.Loaded)?.selected?.name.orEmpty(),
                        readOnly = true,
                        label = stringResource(string.create_wallet_state),
                        placeholder = stringResource(string.state_not_selected),
                        focusedTrailingIcon = stateInputIcon,
                        unfocusedTrailingIcon = stateInputIcon,
                        onValueChange = {
                            // no op readonly
                        },
                        shape = AppTheme.shapes.large,
                    )
                }
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

        StandardVerticalSpacer()

        Column {
            SimpleText(
                text = stringResource(string.create_wallet_referral_code_label),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            TinyVerticalSpacer()

            val referralInputState = if (state.isInvalidReferralErrorShowing) {
                TextInputState.Error(
                    stringResource(string.new_account_referral_code_invalid)
                )
            } else {
                TextInputState.Default(null)
            }
            val trailingIcon =
                if (state.referralCodeInput.isNotEmpty()) {
                    closeImageResource()
                } else {
                    ImageResource.None
                }
            OutlinedTextInput(
                modifier = Modifier.fillMaxWidth(),
                value = state.referralCodeInput,
                label = stringResource(string.new_account_referral_code_label),
                placeholder = stringResource(string.new_account_referral_code),
                focusedTrailingIcon = trailingIcon,
                unfocusedTrailingIcon = trailingIcon,
                singleLine = true,
                onTrailingIconClicked = {
                    onIntent(CreateWalletIntent.ReferralInputChanged(""))
                },
                state = referralInputState,
                onValueChange = {
                    onIntent(CreateWalletIntent.ReferralInputChanged(it))
                },
                shape = AppTheme.shapes.large,
            )
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 80.dp)
                .padding(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                ),
            text = stringResource(string.common_get_started),
            state = state.nextButtonState,
            onClick = {
                onIntent(CreateWalletIntent.RegionNextClicked)
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun EmailAndPasswordStep(
    state: CreateWalletViewState,
    onIntent: (CreateWalletIntent) -> Unit
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.smallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val emailFocusRequester = FocusRequester()
            val passwordFocusRequester = FocusRequester()
            val passwordConfirmationFocusRequester = FocusRequester()

            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(state.isShowingInvalidEmailError) {
                if (state.isShowingInvalidEmailError) emailFocusRequester.requestFocus()
            }
            LaunchedEffect(state.passwordInputErrors) {
                if (state.passwordInputErrors.isNotEmpty()) passwordFocusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(88.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.size(58.dp),
                    imageResource = Icons.Filled.User
                )
            }

            StandardVerticalSpacer()

            SimpleText(
                text = stringResource(string.create_wallet_step_2_header),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(string.create_wallet_step_2_subheader),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            StandardVerticalSpacer()

            Column {

                SimpleText(
                    text = stringResource(string.sign_up_email),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                TinyVerticalSpacer()

                val emailTextState =
                    if (state.isShowingInvalidEmailError) {
                        TextInputState.Error(stringResource(string.invalid_email))
                    } else TextInputState.Default(null)
                OutlinedTextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester),
                    value = state.emailInput,
                    label = stringResource(string.sign_up_email),
                    state = emailTextState,
                    placeholder = stringResource(string.create_wallet_email_hint),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    onValueChange = {
                        onIntent(CreateWalletIntent.EmailInputChanged(it))
                    },
                    shape = AppTheme.shapes.large,
                )
            }

            StandardVerticalSpacer()

            Column {

                SimpleText(
                    text = stringResource(string.common_create_password),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                TinyVerticalSpacer()

                var isPasswordVisible by remember { mutableStateOf(false) }
                val trailingIconRes = if (isPasswordVisible)
                    R.drawable.ic_visibility_off else R.drawable.ic_visibility_on
                val passwordTextState = if (state.passwordInputErrors.isNotEmpty()) {
                    TextInputState.Error("Insecure")
                } else if (state.passwordInput.isNotEmpty()) {
                    TextInputState.Success("Secure")
                } else {
                    TextInputState.Default(null)
                }

                OutlinedTextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester),
                    value = state.passwordInput,
                    label = stringResource(string.password),
                    state = passwordTextState,
                    placeholder = stringResource(string.create_wallet_password_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    focusedTrailingIcon = ImageResource.Local(trailingIconRes),
                    unfocusedTrailingIcon = ImageResource.Local(trailingIconRes),
                    singleLine = true,
                    onTrailingIconClicked = {
                        isPasswordVisible = !isPasswordVisible
                    },
                    onValueChange = {
                        onIntent(CreateWalletIntent.PasswordInputChanged(it))
                    },
                    shape = AppTheme.shapes.large,
                )

                TinyVerticalSpacer()

                PasswordInstructions(state.passwordInputErrors)
            }

            StandardVerticalSpacer()

            Column {

                SimpleText(
                    text = stringResource(id = string.confirm_password),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                TinyVerticalSpacer()

                var isPasswordVisible by remember { mutableStateOf(false) }

                val trailingIconRes =
                    if (isPasswordVisible)
                        R.drawable.ic_visibility_off
                    else
                        R.drawable.ic_visibility_on

                var passwordConfirmationInput by remember { mutableStateOf("") }

                var showPasswordDoesntMatchError by remember { mutableStateOf(false) }

                val coroutineScope = rememberCoroutineScope()
                OutlinedTextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordConfirmationFocusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                coroutineScope.launch {
                                    delay(300)
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                        },
                    value = passwordConfirmationInput,
                    label = stringResource(string.password),
                    state = if (showPasswordDoesntMatchError) TextInputState.Error(
                        stringResource(string.passwords_do_not_match)
                    ) else TextInputState.Default(null),
                    placeholder = stringResource(string.signup_re_enter_password),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    focusedTrailingIcon = ImageResource.Local(trailingIconRes),
                    unfocusedTrailingIcon = ImageResource.Local(trailingIconRes),
                    singleLine = true,
                    onTrailingIconClicked = {
                        isPasswordVisible = !isPasswordVisible
                    },
                    onValueChange = {
                        passwordConfirmationInput = it
                        showPasswordDoesntMatchError = it.isNotEmpty() && it != state.passwordInput
                        onIntent(CreateWalletIntent.ConfirmPasswordInputChanged(it))
                    },
                    shape = AppTheme.shapes.large,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 80.dp)
                .padding(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                ),
            text = stringResource(string.create_wallet_create_account),
            state = state.nextButtonState,
            onClick = {
                onIntent(CreateWalletIntent.EmailPasswordNextClicked)
            }
        )
    }
}

@Composable
private fun PasswordInstructions(passwordInputErrors: List<CreateWalletPasswordError>) {
    // Your password must contain at least one lowercase letter, one uppercase letter, one number,
    // one special character and be at least 8 characters long.
    val passwordFormatInstructions = buildAnnotatedString {
        append(stringResource(string.password_input_instruction_beginning))

        val oneLowerCaseLetterStyle =
            if (passwordInputErrors.any { it == CreateWalletPasswordError.InvalidPasswordNoLowerCaseFound })
                SpanStyle(color = ComposeColors.Error.toComposeColor())
            else
                SpanStyle(color = ComposeColors.Body.toComposeColor())
        withStyle(style = oneLowerCaseLetterStyle) {
            append(stringResource(string.password_input_instruction_lowercase))
        }

        val oneUpperCaseLetterStyle =
            if (passwordInputErrors.any { it == CreateWalletPasswordError.InvalidPasswordNoUpperCaseFound })
                SpanStyle(color = ComposeColors.Error.toComposeColor())
            else
                SpanStyle(color = ComposeColors.Body.toComposeColor())

        withStyle(style = oneUpperCaseLetterStyle) {
            append(stringResource(string.password_input_instruction_uppercase))
        }

        val oneNumberStyle =
            if (passwordInputErrors.any { it == CreateWalletPasswordError.InvalidPasswordNoNumberFound })
                SpanStyle(color = ComposeColors.Error.toComposeColor())
            else
                SpanStyle(color = ComposeColors.Body.toComposeColor())

        withStyle(style = oneNumberStyle) {
            append(stringResource(string.password_input_instruction_number))
        }

        val oneSpecialCharacterStyle =
            if (passwordInputErrors.any { it == CreateWalletPasswordError.InvalidPasswordNoSpecialCharFound })
                SpanStyle(color = ComposeColors.Error.toComposeColor())
            else
                SpanStyle(color = ComposeColors.Body.toComposeColor())

        withStyle(style = oneSpecialCharacterStyle) {
            append(stringResource(string.password_input_instruction_special_char))
        }

        val atLeastEightCharactersStyle =
            if (passwordInputErrors.any { it == CreateWalletPasswordError.InvalidPasswordTooShort })
                SpanStyle(color = ComposeColors.Error.toComposeColor())
            else
                SpanStyle(color = ComposeColors.Body.toComposeColor())

        withStyle(style = atLeastEightCharactersStyle) {
            append(stringResource(string.password_input_instruction_length))
        }
    }

    SimpleText(
        text = passwordFormatInstructions,
        style = ComposeTypographies.Caption1,
        color = ComposeColors.Body,
        gravity = ComposeGravities.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CreateWalletFailed(
    backOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1F),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                imageResource = Icons.Filled.User.withTint(Color.White)
                    .withBackground(
                        backgroundColor = AppTheme.colors.primary,
                        iconSize = 40.dp,
                        backgroundSize = 72.dp
                    )
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(string.create_wallet_error_title),
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(string.create_wallet_error_description),
                style = AppTheme.typography.body1,
                color = AppTheme.colors.body,
                textAlign = TextAlign.Center
            )
        }

        MinimalPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                ),
            text = stringResource(string.common_go_back),
            onClick = backOnClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCreateWalletFailed() {
    CreateWalletFailed({})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCreateWalletFailedDark() {
    PreviewCreateWalletFailed()
}

@Preview
@Composable
private fun Preview_RegionAndReferral() {
    val state = CreateWalletViewState(
        screen = CreateWalletScreen.REGION_AND_REFERRAL,
        emailInput = "test@blockchain.com",
        isShowingInvalidEmailError = true,
        passwordInput = "Somepassword",
        passwordInputErrors = listOf(CreateWalletPasswordError.InvalidPasswordTooShort),
        countryInputState = CountryInputState.Loaded(countries = countries, selected = countries[1], suggested = null),
        stateInputState = StateInputState.Loading,
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
        showStateBottomSheet = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_RegionAndReferralDark() {
    Preview_RegionAndReferral()
}

@Preview
@Composable
private fun Preview_EmailAndPassword() {
    val state = CreateWalletViewState(
        screen = CreateWalletScreen.EMAIL_AND_PASSWORD,
        emailInput = "test@blockchain.com",
        isShowingInvalidEmailError = true,
        passwordInput = "Somepassword",
        passwordInputErrors = listOf(CreateWalletPasswordError.InvalidPasswordTooShort),
        countryInputState = CountryInputState.Loaded(countries = countries, selected = countries[1], suggested = null),
        stateInputState = StateInputState.Loading,
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
        showStateBottomSheet = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_EmailAndPasswordDark() {
    Preview_EmailAndPassword()
}

private val states = listOf(
    Region.State("US", "Arkansas", true, "AK"),
    Region.State("US", "New York", true, "NY")
)
private val countries = listOf(
    Region.Country("PT", "Portugal", true, emptyList()),
    Region.Country("US", "United States of America", true, states.map { it.stateCode })
)

private fun CreateWalletError.errorMessage(context: Context): String = when (this) {
    CreateWalletError.RecaptchaFailed -> context.getString(string.recaptcha_failed)
    is CreateWalletError.Unknown -> this.message ?: context.getString(
        string.something_went_wrong_try_again
    )
}
