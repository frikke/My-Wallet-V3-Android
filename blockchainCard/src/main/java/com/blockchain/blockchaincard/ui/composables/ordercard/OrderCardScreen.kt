package com.blockchain.blockchaincard.ui.composables.ordercard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.InfoButton
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey000

@Composable
fun OrderOrLinkCard(
    viewModel: OrderCardViewModel
) {
    OrderOrLinkCardContent(
        onOrderCard = { viewModel.onIntent(BlockchainCardIntent.OrderCard) },
        onLinkCard = { viewModel.onIntent(BlockchainCardIntent.LinkCard) },
    )
}

@Composable
private fun OrderOrLinkCardContent(
    onOrderCard: () -> Unit,
    onLinkCard: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(AppTheme.dimensions.paddingLarge)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_graphic_cards),
            contentDescription = stringResource(id = R.string.blockchain_card),
            modifier = Modifier.padding(
                top = AppTheme.dimensions.xxxPaddingLarge,
                end = AppTheme.dimensions.paddingMedium
            )
        )

        SimpleText(
            text = stringResource(id = R.string.order_card_intro_primary),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            text = stringResource(id = R.string.order_card_intro_secundary),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        Spacer(Modifier.size(dimensionResource(id = R.dimen.epic_margin)))

        PrimaryButton(
            text = stringResource(id = R.string.order_my_card),
            onClick = onOrderCard,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.size(AppTheme.dimensions.paddingSmall))

        MinimalButton(
            text = stringResource(id = R.string.link_card_here),
            onClick = onLinkCard,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOrderOrLinkCardContent() {
    AppTheme(darkTheme = false) {
        AppSurface {
            OrderOrLinkCardContent({ }, {})
        }
    }
}

@Composable
fun SelectCardForOrder(onCreateCard: () -> Unit, onSeeProductDetails: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(AppTheme.dimensions.paddingLarge)
        ) {
            Image(
                painter = painterResource(id = R.drawable.card_no_details),
                contentDescription = stringResource(id = R.string.blockchain_card),
                modifier = Modifier.padding(
                    start = AppTheme.dimensions.paddingMedium,
                    top = AppTheme.dimensions.paddingLarge,
                    end = AppTheme.dimensions.paddingMedium
                )
            )

            SimpleText(
                text = stringResource(id = R.string.virtual),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                text = stringResource(R.string.virtual_card_product_description),
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
        }

        PrimaryButton(
            text = stringResource(R.string.create_card),
            onClick = onCreateCard,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.paddingLarge)
        )
    }
}

@Composable
fun ProductDetails(cardProduct: BlockchainCardProduct?, onCloseProductDetailsBottomSheet: () -> Unit) {

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

        SheetHeader(onClosePress = onCloseProductDetailsBottomSheet, title = stringResource(R.string.card_details))

        Column(modifier = Modifier.background(Grey000)) {
            Image(
                painter = painterResource(id = R.drawable.card_no_details),
                contentDescription = stringResource(id = R.string.blockchain_card),
                modifier = Modifier.padding(
                    start = AppTheme.dimensions.xxPaddingLarge,
                    top = AppTheme.dimensions.paddingMedium,
                    end = dimensionResource(id = R.dimen.paddingEpic)
                )
            )

            SimpleText(
                text = stringResource(id = R.string.virtual),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre,
                modifier = Modifier.padding(
                    bottom = AppTheme.dimensions.paddingMedium
                )
            )
        }

        SmallSectionHeader(text = stringResource(R.string.card_benefits), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.cashback_rewards),
            onClick = {},
            endTag = TagViewState("1%", TagType.Default()) // TODO (labreu): remove placeholder
        )

        SmallSectionHeader(text = stringResource(R.string.fees), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.annual_fees),
            onClick = {},
            endTag = TagViewState(stringResource(R.string.no_fee), TagType.Success())
        )
        DefaultTableRow(
            primaryText = stringResource(R.string.delivery_fee),
            onClick = {},
            endTag = TagViewState(stringResource(R.string.no_fee), TagType.Success())
        )

        SmallSectionHeader(text = stringResource(R.string.card), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.contactless_payment),
            onClick = {},
            endTag = TagViewState(stringResource(R.string.yes), TagType.Default())
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        SmallSectionHeader(
            text = stringResource(R.string.consumer_financial_protection_bureau),
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.short_form_disclosure),
            onClick = {}
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        SmallSectionHeader(
            text = stringResource(R.string.blockchain_terms_and_conditions),
            modifier = Modifier.fillMaxWidth()
        )
        DefaultTableRow(
            primaryText = stringResource(R.string.terms_and_conditions),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSelectCardForOrder() {
    AppTheme(darkTheme = false) {
        AppSurface {
            SelectCardForOrder({}, {})
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
            SimpleText(
                text = stringResource(R.string.card_created),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
            SimpleText(
                text = stringResource(R.string.your_card_is_now_linked),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {
            androidx.compose.material.Button(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.paddingSmall),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gpay_save_card),
                    contentDescription = stringResource(R.string.gpay_save_to_phone),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }

            MinimalButton(
                text = stringResource(R.string.do_it_later),
                state = ButtonState.Enabled,
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.paddingSmall)
            )
        }
    }
}

@Composable
fun CardCreationFailed() {
    Column(Modifier.fillMaxWidth()) {
        SimpleText(
            text = stringResource(R.string.failed),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Error,
            gravity = ComposeGravities.Centre
        )
    }
}
