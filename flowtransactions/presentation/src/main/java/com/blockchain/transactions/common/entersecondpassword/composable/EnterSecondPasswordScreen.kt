package com.blockchain.transactions.common.entersecondpassword.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.coincore.Coincore
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.entersecondpassword.EnterSecondPasswordArgs
import org.koin.androidx.compose.get

@Composable
fun EnterSecondPasswordScreen(
    args: EnterSecondPasswordArgs,
    coincore: Coincore = get(scope = payloadScope),
    onAccountSecondPasswordValidated: (CryptoAccountWithBalance, secondPassword: String) -> Unit,
    onBackPressed: () -> Unit,
) {
    val sourceAccount = args.sourceAccount.data ?: return

    var isShowingError by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    Column {
        SheetFlatHeader(
            icon = StackedIcon.None,
            title = stringResource(R.string.common_swap_from),
            onCloseClick = onBackPressed
        )

        Content(
            isShowingError = isShowingError,
            passwordInput = passwordInput,
            passwordInputChanged = {
                passwordInput = it
                isShowingError = false
            },
            confirmClicked = {
                isShowingError = false
                val password = passwordInput
                val isValid = coincore.validateSecondPassword(password)
                if (isValid) {
                    onAccountSecondPasswordValidated(sourceAccount, password)
                } else {
                    isShowingError = true
                }
            },
        )
    }
}

@Composable
private fun Content(
    isShowingError: Boolean,
    passwordInput: String,
    passwordInputChanged: (String) -> Unit,
    confirmClicked: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.standardSpacing),
    ) {

        Spacer(Modifier.weight(1f))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.transfer_second_pswd_desc),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre,
        )

        StandardVerticalSpacer()

        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = passwordInput,
            placeholder = stringResource(R.string.transfer_second_pswd_hint),
            state = if (isShowingError) {
                TextInputState.Error(stringResource(R.string.invalid_password))
            } else {
                TextInputState.Default(null)
            },
            onValueChange = passwordInputChanged,
        )

        StandardVerticalSpacer()

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.common_next),
            onClick = confirmClicked,
        )

        Spacer(Modifier.weight(2f))
    }
}

@Preview
@Composable
private fun Preview() {
    Content(
        isShowingError = false,
        passwordInput = "Some password",
        passwordInputChanged = {},
        confirmClicked = {},
    )
}
