package piuk.blockchain.android.cards.cvv

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.icon.ScreenStatusIcon
import com.blockchain.componentlib.icons.Card
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Security
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import piuk.blockchain.android.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SecurityCodeScreen(
    viewState: StateFlow<SecurityCodeViewState>,
    onIntent: (SecurityCodeIntent) -> Unit,
    onBackButtonClick: () -> Unit
) {
    val state by viewState.collectAsStateLifecycleAware()
    val context = LocalContext.current

    val scaffoldState = rememberScaffoldState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            NavigationBar(
                title = stringResource(id = com.blockchain.stringResources.R.string.security),
                onBackButtonClick = onBackButtonClick
            )
        }
    ) { padding ->
        LaunchedEffect(state.error) {
            state.error?.let { error ->
                keyboardController?.hide()
                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                    message = error.errorMessage(context),
                    duration = SnackbarDuration.Indefinite,
                    actionLabel = context.getString(com.blockchain.stringResources.R.string.common_retry)
                )
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    onBackButtonClick()
                }
            }
        }

        Column(
            modifier = Modifier
                .background(AppColors.background)
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = AppTheme.dimensions.standardSpacing
                )
        ) {
            Spacer(Modifier.height(AppTheme.dimensions.largeSpacing))

            ScreenStatusIcon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                main = Icons.Filled.Card.withTint(AppColors.title),
                tag = Icons.Filled.Security.withTint(AppColors.title)
            )

            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.security_code),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre,
                modifier = Modifier
                    .padding(top = AppTheme.dimensions.smallSpacing)
                    .fillMaxWidth()
            )
            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.security_code_info, state.cvvLength),
                style = ComposeTypographies.BodyMono,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
                modifier = Modifier
                    .padding(top = AppTheme.dimensions.smallSpacing)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(AppTheme.dimensions.hugeSpacing))

            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.cvv_code),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .padding(top = AppTheme.dimensions.smallSpacing)
                    .fillMaxWidth()
            )

            TextInput(
                secondaryBackground = true,
                value = state.cvv,
                placeholder = StringBuilder().apply { repeat(state.cvvLength) { append("0") } }.toString(),
                maxLength = state.cvvLength,
                trailingIcon = ImageResource.Local(
                    id = R.drawable.ic_lock,
                    colorFilter = ColorFilter.tint(Grey400),
                    size = 20.dp
                ),
                onValueChange = {
                    onIntent(SecurityCodeIntent.CvvInputChanged(it))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onIntent(SecurityCodeIntent.NextClicked)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(AppTheme.dimensions.smallSpacing))

            Row(
                modifier = Modifier
                    .background(color = AppColors.backgroundSecondary, shape = RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .border(1.dp, AppTheme.colors.light, shape = RoundedCornerShape(8.dp))
            ) {
                if (state.cardDetailsLoading) {
                    ShimmerLoadingTableRow()
                } else {
                    Image(
                        imageResource = state.cardIcon,
                        modifier = Modifier
                            .padding(AppTheme.dimensions.smallSpacing)
                            .align(Alignment.CenterVertically)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterVertically)
                            .padding(vertical = AppTheme.dimensions.smallSpacing)
                    ) {
                        SimpleText(
                            text = state.cardName.orEmpty(),
                            style = ComposeTypographies.Paragraph2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SimpleText(
                            text = stringResource(
                                com.blockchain.stringResources.R.string.security_last_card_digits,
                                state.lastCardDigits.orEmpty()
                            ),
                            style = ComposeTypographies.Paragraph1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_next),
                state = state.nextButtonState,
                onClick = { onIntent(SecurityCodeIntent.NextClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = com.blockchain.componentlib.R.dimen.large_spacing),
                        bottom = dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing)
                    )
            )
        }
    }
}

private fun UpdateSecurityCodeError.errorMessage(context: Context): String = when (this) {
    is UpdateSecurityCodeError.CardDetailsFailed ->
        if (message.isNullOrEmpty()) context.getString(
            com.blockchain.stringResources.R.string.something_went_wrong_try_again
        ) else message

    is UpdateSecurityCodeError.UpdateCvvFailed ->
        if (message.isNullOrEmpty()) context.getString(
            com.blockchain.stringResources.R.string.something_went_wrong_try_again
        ) else message
}

@Composable
@Preview
fun SecurityCodeScreenPreview() {
    AppTheme {
        SecurityCodeScreen(
            viewState = MutableStateFlow(
                SecurityCodeViewState(
                    cardDetailsLoading = false,
                    nextButtonState = ButtonState.Enabled,
                    cvv = "123",
                    cardName = "My very expensive card",
                    lastCardDigits = "456",
                    cardIcon = ImageResource.Local(id = R.drawable.ic_card_icon, contentDescription = null)
                )
            ),
            onIntent = {},
            onBackButtonClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityCodeScreenPreviewDark() {
    SecurityCodeScreenPreview()
}
