package com.blockchain.blockchaincard.ui.composables

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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCardProduct
import com.blockchain.blockchaincard.viewmodel.BlockchainCardDestination
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.DestructivePrimaryButton
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
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalMaterialApi::class)
@Composable
fun BlockchainCardNavHost(
    viewModel: BlockchainCardViewModel,
    startDestination: BlockchainCardDestination,
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val state by stateFlowLifecycleAware.collectAsState(null)
    MviNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = BlockchainCardNavigationRouter(rememberNavController()),
        startDestination = startDestination,
    ) {

        composable(BlockchainCardDestination.OrderOrLinkCardDestination) {
            OrderOrLinkCard(viewModel)
        }

        composable(BlockchainCardDestination.SelectCardForOrderDestination) {
            SelectCardForOrder(
                onCreateCard = {
                    viewModel.onIntent(
                        // TODO(labreu): once staging API is not harcoded, remove this
                        BlockchainCardIntent.CreateCard(
                            productCode = "VIRTUAL1",
                            ssn = "111111110"
                        )
                    )
                },
                onSeeProductDetails = {
                    viewModel.onIntent(
                        BlockchainCardIntent.OnSeeProductDetails
                    )
                }
            )
        }

        composable(BlockchainCardDestination.CreateCardInProgressDestination) {
            CardCreationInProgress()
        }

        composable(BlockchainCardDestination.CreateCardSuccessDestination) {
            CardCreationSuccess(
                onFinish = {
                    viewModel.onIntent(BlockchainCardIntent.ManageCard)
                }
            )
        }

        composable(BlockchainCardDestination.CreateCardFailedDestination) {
            CardCreationFailed()
        }

        bottomSheet(BlockchainCardDestination.SeeProductDetailsDestination) {
            ProductDetails(
                cardProduct = state?.cardProduct,
                onCloseProductDetailsBottomSheet = {
                    viewModel.onIntent(BlockchainCardIntent.HideProductDetailsBottomSheet)
                }
            )
        }

        // Manage Card Screens
        composable(BlockchainCardDestination.ManageCardDestination) {
            ManageCard(cardId = state?.cardId, onDeleteCard = { viewModel.onIntent(BlockchainCardIntent.DeleteCard) })
        }
    }
}

@Composable
private fun OrderOrLinkCard(
    viewModel: BlockchainCardViewModel
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
            contentDescription = stringResource(id = R.string.order_card_title),
            modifier = Modifier.padding(
                0.dp,
                AppTheme.dimensions.xxxPaddingLarge,
                AppTheme.dimensions.paddingMedium,
                0.dp
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

        Spacer(Modifier.size(115.dp))

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
private fun SelectCardForOrder(onCreateCard: () -> Unit, onSeeProductDetails: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(AppTheme.dimensions.paddingLarge)
        ) {
            Image(
                painter = painterResource(id = R.drawable.card),
                contentDescription = stringResource(id = R.string.order_card_title),
                modifier = Modifier.padding(
                    AppTheme.dimensions.paddingMedium,
                    AppTheme.dimensions.paddingLarge,
                    AppTheme.dimensions.paddingMedium,
                    0.dp
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
                    .padding(0.dp, AppTheme.dimensions.xPaddingLarge)
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
private fun ProductDetails(cardProduct: BlockchainDebitCardProduct?, onCloseProductDetailsBottomSheet: () -> Unit) {

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

        SheetHeader(onClosePress = onCloseProductDetailsBottomSheet, title = "Card Details")

        Column(modifier = Modifier.background(Color(0xFFFAFBFF))) {
            Image(
                painter = painterResource(id = R.drawable.card),
                contentDescription = stringResource(id = R.string.order_card_title),
                modifier = Modifier.padding(
                    84.dp,
                    AppTheme.dimensions.paddingMedium,
                    84.dp,
                    0.dp
                )
            )

            SimpleText(
                text = stringResource(id = R.string.virtual),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre,
                modifier = Modifier.padding(
                    0.dp,
                    0.dp,
                    0.dp,
                    AppTheme.dimensions.paddingMedium
                )
            )
        }

        SmallSectionHeader(text = stringResource(R.string.card_benefits), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.cashback_rewards),
            onClick = {},
            endTag = TagViewState("1%", TagType.Default())
        )

        SmallSectionHeader(text = stringResource(R.string.fees), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.annual_fees),
            onClick = {},
            endTag = TagViewState("No Fee", TagType.Success())
        )
        DefaultTableRow(
            primaryText = stringResource(R.string.delivery_fee),
            onClick = {},
            endTag = TagViewState("No Fee", TagType.Success())
        )

        SmallSectionHeader(text = stringResource(R.string.card), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.contactless_payment),
            onClick = {},
            endTag = TagViewState(stringResource(R.string.yes), TagType.Default())
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        SmallSectionHeader(text = stringResource(R.string.consumer_financial_protection_bureau), modifier = Modifier.fillMaxWidth())
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.short_form_disclosure),
            onClick = {}
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        SmallSectionHeader(text = stringResource(R.string.blockchain_terms_and_conditions), modifier = Modifier.fillMaxWidth())
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
private fun CardCreationInProgress() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        CircularProgressIndicator()
        SimpleText(
            text = "Processing", style = ComposeTypographies.Title3, color = ComposeColors.Body,
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
private fun CardCreationSuccess(onFinish: () -> Unit) {
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
private fun CardCreationFailed() {
    Column(Modifier.fillMaxWidth()) {
        SimpleText(
            text = stringResource(R.string.failed),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Error,
            gravity = ComposeGravities.Centre
        )
    }
}

@Composable
private fun ManageCard(cardId: String?, onDeleteCard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        SimpleText(
            text = "Card ID: $cardId",
            style = ComposeTypographies.Title1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        DestructivePrimaryButton(text = stringResource(R.string.delete_card), onClick = onDeleteCard)
    }
}
