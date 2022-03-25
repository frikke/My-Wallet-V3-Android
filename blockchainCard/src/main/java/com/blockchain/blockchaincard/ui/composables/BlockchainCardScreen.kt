package com.blockchain.blockchaincard.ui.composables

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.data.BlockchainDebitCardProduct
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviNavHost
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet

@Composable
fun BlockchainCardScreen(viewModel: BlockchainCardViewModel) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val state by stateFlowLifecycleAware.collectAsState(null)

    when(state) {
        is BlockchainCardViewState.OrderOrLinkCard -> {
            OrderOrLinkCard(viewModel)
        }

        is BlockchainCardViewState.OrderCard -> {
            // SelectCardForOrder()
        }

        is BlockchainCardViewState.LinkCard -> {
            //TODO
        }

        is BlockchainCardViewState.ManageCard -> {
            ManageCard((state as BlockchainCardViewState.ManageCard).cardId) // TODO why cast here???
        }
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun BlockchainCardNavHost(
    navigator: BlockchainCardNavigationRouter,
    viewModel: BlockchainCardViewModel,
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val state by stateFlowLifecycleAware.collectAsState(null)

    MviNavHost(
        navigator,
        startDestination = "blockchain_card"
    ) {

        composable("blockchain_card") {
            BlockchainCardScreen(viewModel)
        }

        composable("order_or_link_card") {
            OrderOrLinkCard(viewModel)
        }

        composable("select_card_for_order") {
            SelectCardForOrder(
                onCreateCard = {
                    viewModel.onIntent(
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

        bottomSheet("product_details") {
            ProductDetails((state as BlockchainCardViewState.ShowProductDetails).cardProduct)
        }
    }
}

@Composable
private fun OrderOrLinkCard(
    viewModel: BlockchainCardViewModel
) {
    OrderOrLinkCardContent(
        onOrderCard = { viewModel.onIntent(BlockchainCardIntent.OrderCard) },
        onLinkCard =  { viewModel.onIntent(BlockchainCardIntent.LinkCard) },
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
            contentDescription = "Blockchain Card",
            modifier = Modifier.padding(0.dp, AppTheme.dimensions.xxxPaddingLarge, AppTheme.dimensions.paddingMedium, 0.dp)
        )

        SimpleText(
            text = "Your Gateway To The Blockchain Debit Card",
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            text = "A card that lets you spend and earn in crypto right from your Blockchain account.",
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        Spacer(Modifier.size(115.dp))

        PrimaryButton(
            text = "Order My Card",
            onClick = onOrderCard,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.size(AppTheme.dimensions.paddingSmall))

        MinimalButton(
            text = "Already Have A Card? Link It Here",
            onClick = onLinkCard,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOrderCardScreen() {
    AppTheme(darkTheme = false) {
        AppSurface {
            // OrderOrLinkCard(BlockchainCardModel())
        }
    }
}

@Composable
private fun SelectCardForOrder(onCreateCard: () -> Unit, onSeeProductDetails: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(AppTheme.dimensions.paddingLarge)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_card),
                contentDescription = "Blockchain Card",
                modifier = Modifier.padding(0.dp, AppTheme.dimensions.paddingLarge, AppTheme.dimensions.paddingMedium, 0.dp)
            )

            SimpleText(
                text = "Virtual",
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                text = "Our digital only card, use instantly for online payments. ",
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            AlertButton(
                text = "See Card Details",
                onClick = onSeeProductDetails,
                state = ButtonState.Enabled,
                modifier = Modifier
                    .padding(0.dp, AppTheme.dimensions.xPaddingLarge)
                    .wrapContentWidth()
            )
        }

        PrimaryButton(
            text = "Create Card",
            onClick = onCreateCard,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.paddingLarge)
        )
    }
}

@Composable
private fun ProductDetails(cardProduct: BlockchainDebitCardProduct) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        
        SheetHeader(onClosePress = { /*TODO*/ }, title = "Card Details")
        
        Image(
            painter = painterResource(id = R.drawable.ic_card),
            contentDescription = "Blockchain Card",
            modifier = Modifier.padding(84.dp, 16.dp)
        )

        SimpleText(
            text = "Virtual",
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SmallSectionHeader(text = "Card Benefits (Place Holder)", modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = "Cashback Rewards",
            onClick = {},
            endTag = TagViewState("1%", TagType.Default())
        )

        SmallSectionHeader(text = "Fees", modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = "Delivery Fee",
            onClick = {},
            endTag = TagViewState(cardProduct.price.toStringWithSymbol(), TagType.InfoAlt())
        )

        SmallSectionHeader(text = "Card (Placeholder)", modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = "Contactless Payment",
            secondaryText = "",
            onClick = {},
            endTag = TagViewState("Yes", TagType.Default())
        )
        HorizontalDivider()
        SimpleText(text = "Consumer Financial Protection Bureau", style = ComposeTypographies.Paragraph2, color = ComposeColors.Body, gravity = ComposeGravities.Centre)
        HorizontalDivider()
        DefaultTableRow(
            primaryText = "Short Form Disclosure",
            onClick = {}
        )
        HorizontalDivider()
        SimpleText(text = "Consumer Financial Protection Bureau", style = ComposeTypographies.Paragraph2, color = ComposeColors.Body, gravity = ComposeGravities.Centre)
        DefaultTableRow(
            primaryText = "Terms & Conditions",
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
private fun ManageCard(cardId: String) {
    SimpleText(
        text = "Card ID: $cardId",
        style = ComposeTypographies.Title1,
        color = ComposeColors.Title,
        gravity = ComposeGravities.Centre
    )
}

