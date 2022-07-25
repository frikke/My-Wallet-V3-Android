package com.blockchain.blockchaincard.ui.composables.ordercard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.InfoButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.system.Webview
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800

@Composable
fun OrderCard(
    viewModel: OrderCardViewModel
) {
    OrderCardIntro(
        onOrderCard = {
            viewModel.onIntent(
                BlockchainCardIntent.OrderCardKYCAddress
            )
        }
    )
}

@Composable
fun OrderCardIntro(onOrderCard: () -> Unit) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.padding(AppTheme.dimensions.xPaddingLarge)
    ) {
        Image(
            painter = painterResource(id = R.drawable.card_intro),
            contentDescription = stringResource(id = R.string.blockchain_card),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )

        Spacer(Modifier.size(AppTheme.dimensions.paddingLarge))

        SimpleText(
            text = stringResource(R.string.card_intro_title),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        Spacer(Modifier.size(AppTheme.dimensions.paddingSmall))

        SimpleText(
            text = stringResource(id = R.string.order_card_intro),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.Centre
        )

        Spacer(Modifier.size(AppTheme.dimensions.xxxPaddingLarge))

        PrimaryButton(
            text = stringResource(id = R.string.order_my_card),
            onClick = onOrderCard,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderCardIntroPreview() {
    OrderCardIntro { }
}

@Composable
fun OrderCardAddressKYC(onContinue: () -> Unit, onCheckBillingAddress: () -> Unit, shortAddress: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(top = AppTheme.dimensions.xPaddingLarge)
        ) {
            SimpleText(
                text = stringResource(R.string.verify_your_address),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier.padding(horizontal = AppTheme.dimensions.paddingLarge)
            )

            SimpleText(
                text = stringResource(R.string.verify_your_address_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier.padding(horizontal = AppTheme.dimensions.paddingLarge)
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.paddingLarge)
            )

            if (!shortAddress.isNullOrEmpty()) {
                DefaultTableRow(
                    primaryText = stringResource(R.string.residential_address),
                    secondaryText = shortAddress,
                    onClick = onCheckBillingAddress,
                )
            } else {
                ShimmerLoadingTableRow()
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(R.string.common_next),
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderCardAddressKYCPreview() {
    OrderCardAddressKYC(
        onContinue = {},
        onCheckBillingAddress = {},
        shortAddress = "123 Main St, New York, NY 10001"
    )
}

const val SSN_LENGTH = 9

@Composable
fun OrderCardSsnKYC(onContinue: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {

        var ssn by remember { mutableStateOf("") }

        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(top = AppTheme.dimensions.xPaddingLarge)
        ) {
            SimpleText(
                text = stringResource(R.string.verify_your_identity),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier.padding(horizontal = AppTheme.dimensions.paddingLarge)
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

            SimpleText(
                text = stringResource(R.string.verify_your_identity_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier.padding(horizontal = AppTheme.dimensions.paddingLarge)
            )

            TextInput(
                value = ssn,
                label = stringResource(R.string.ssn_title),
                placeholder = stringResource(R.string.ssn_hint),
                onValueChange = { if (it.length <= SSN_LENGTH) ssn = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.padding(
                    horizontal = AppTheme.dimensions.paddingMedium,
                    vertical = AppTheme.dimensions.paddingLarge
                ),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(R.string.common_next),
                onClick = { onContinue(ssn) },
                state = if (ssn.isNotEmpty() && ssn.length == SSN_LENGTH) ButtonState.Enabled else ButtonState.Disabled,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderCardSsnKYCPreview() {
    OrderCardSsnKYC(onContinue = {})
}

@Composable
fun OrderCardContent(
    termsAndConditionsSeen: Boolean = false,
    onCreateCard: () -> Unit,
    onSeeProductDetails: () -> Unit,
    onSeeTermsAndConditions: () -> Unit,
) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier.padding(AppTheme.dimensions.paddingLarge)
    ) {
        Image(
            painter = painterResource(id = R.drawable.card_front),
            contentDescription = stringResource(id = R.string.blockchain_card)
        )

        SimpleText(
            text = stringResource(id = R.string.virtual),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            text = stringResource(id = R.string.order_card_intro),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        InfoButton(
            text = stringResource(R.string.see_card_details),
            onClick = onSeeProductDetails,
            state = ButtonState.Enabled,
            modifier = Modifier
                .padding(
                    vertical = AppTheme.dimensions.xPaddingLarge
                )
                .wrapContentWidth()
        )

        Spacer(Modifier.size(dimensionResource(id = R.dimen.epic_margin)))

        val termsAndConditionsCheckboxState = remember { mutableStateOf(CheckboxState.Unchecked) }

        Row(
            modifier = Modifier.padding(bottom = AppTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                state = termsAndConditionsCheckboxState.value,
                onCheckChanged = { checked ->
                    if (checked) {
                        if (termsAndConditionsSeen) {
                            termsAndConditionsCheckboxState.value = CheckboxState.Checked
                        } else {
                            onSeeTermsAndConditions()
                        }
                    } else {
                        termsAndConditionsCheckboxState.value = CheckboxState.Unchecked
                    }
                },
            )
            SimpleText(
                text = stringResource(id = R.string.terms_and_conditions_label),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Muted,
                gravity = ComposeGravities.Start,
                modifier = Modifier.clickable {
                    onSeeTermsAndConditions()
                }
            )
        }

        PrimaryButton(
            text = stringResource(id = R.string.create_card),
            onClick = onCreateCard,
            modifier = Modifier.fillMaxWidth(),
            state = when (termsAndConditionsCheckboxState.value) {
                CheckboxState.Checked -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOrderCardContent() {
    AppTheme(darkTheme = false) {
        AppSurface {
            OrderCardContent(false, { }, {}, {})
        }
    }
}

@Composable
fun ProductDetails(
    onCloseProductDetailsBottomSheet: () -> Unit,
    onSeeProductLegalInfo: () -> Unit
) {

    val backgroundColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark800
    }

    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(backgroundColor)
    ) {

        SheetHeader(
            title = stringResource(id = R.string.virtual_card),
            startImageResource = ImageResource.Local(
                id = R.drawable.credit_card,
                contentDescription = null,
            ),
            onClosePress = onCloseProductDetailsBottomSheet
        )

        DefaultTableRow(
            primaryText = stringResource(id = R.string.no_fees),
            secondaryText = stringResource(id = R.string.no_fees_description),
            startImageResource = ImageResource.Local(
                id = R.drawable.flag,
                contentDescription = null,
            ),
            endImageResource = ImageResource.None,
            onClick = {},
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        DefaultTableRow(
            primaryText = stringResource(id = R.string.crypto_rewards),
            secondaryText = stringResource(id = R.string.crypto_rewards_description),
            startImageResource = ImageResource.Local(
                id = R.drawable.present,
                contentDescription = null,
            ),
            endImageResource = ImageResource.None,
            onClick = {},
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        DefaultTableRow(
            primaryText = stringResource(id = R.string.legal_info_title),
            startImageResource = ImageResource.Local(
                id = R.drawable.list_bullets,
                contentDescription = null,
            ),
            onClick = onSeeProductLegalInfo,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProductDetails() {
    AppTheme(darkTheme = false) {
        AppSurface {
            ProductDetails({}, {})
        }
    }
}

@Composable
fun ProductLegalInfo(
    onCloseProductLegalInfoBottomSheet: () -> Unit,
    onClickShortFormDisclosure: () -> Unit,
    onClickTermsAndConditions: () -> Unit,
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark800
    }

    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(backgroundColor)
    ) {
        SheetHeader(
            title = stringResource(id = R.string.legal_info_title),
            startImageResource = ImageResource.Local(
                id = R.drawable.list_bullets,
                contentDescription = null,
            ),
            onClosePress = onCloseProductLegalInfoBottomSheet
        )

        SmallSectionHeader(
            text = stringResource(R.string.consumer_financial_protection_bureau),
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.short_form_disclosure),
            onClick = onClickShortFormDisclosure
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        SmallSectionHeader(
            text = stringResource(R.string.blockchain_terms_and_conditions),
            modifier = Modifier.fillMaxWidth()
        )
        DefaultTableRow(
            primaryText = stringResource(R.string.terms_and_conditions),
            onClick = onClickTermsAndConditions
        )
    }
}

@Preview
@Composable
fun PreviewProductLegalInfo() {
    AppTheme(darkTheme = false) {
        AppSurface {
            ProductLegalInfo({}, {}, {})
        }
    }
}

@Composable
fun CardCreationInProgress() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        CircularProgressIndicator()
        SimpleText(
            text = stringResource(R.string.processing), style = ComposeTypographies.Title3, color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCardCreationInProgress() {
    AppTheme(darkTheme = false) {
        AppSurface {
            CircularProgressBar()
        }
    }
}

@Composable
fun CardCreationSuccess(onFinish: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.xxxPaddingLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.credit_card_success),
                contentDescription = stringResource(R.string.card_created),
                modifier = Modifier.wrapContentWidth(),
            )
            SimpleText(
                text = stringResource(R.string.card_created),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
            SimpleText(
                text = stringResource(id = R.string.continue_to_card_dashboard),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(id = R.string.go_to_dashboard),
                state = ButtonState.Enabled,
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCardCreationSuccess() {
    AppTheme(darkTheme = false) {
        AppSurface {
            CardCreationSuccess({})
        }
    }
}

@Composable
fun CardCreationFailed(errorTitle: String, errorDescription: String, onTryAgain: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.paddingLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.credit_card_failed),
                contentDescription = stringResource(R.string.card_created),
                modifier = Modifier.wrapContentWidth(),
            )
            SimpleText(
                text = errorTitle,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                text = errorDescription,
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(id = R.string.common_try_again),
                state = ButtonState.Enabled,
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCardCreationFailed() {
    AppTheme(darkTheme = false) {
        AppSurface {
            CardCreationFailed(
                errorTitle = "Ups something went wrong",
                errorDescription = "Please try again later",
                onTryAgain = {}
            )
        }
    }
}

@Composable
fun LegalDocument(legalDocument: BlockchainCardLegalDocument, onContinue: () -> Unit) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.paddingSmall)
    ) {
        Webview(
            url = legalDocument.url,
            modifier = Modifier
                .padding(top = AppTheme.dimensions.paddingMedium)
                .weight(0.9f)
        )

        PrimaryButton(
            text = stringResource(id = R.string.common_ok),
            state = ButtonState.Enabled,
            onClick = onContinue,
            modifier = Modifier
                .padding(AppTheme.dimensions.paddingLarge)
                .fillMaxWidth()
                .weight(0.1f)
        )
    }
}
