package com.blockchain.blockchaincard.ui.composables

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.ui.BlockchainCardIntent
import com.blockchain.blockchaincard.ui.BlockchainCardModel
import com.blockchain.blockchaincard.ui.BlockchainCardState
import com.blockchain.blockchaincard.ui.CardState
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun BlockchainCardScreen(model: BlockchainCardModel) {

    val state by model.state.subscribeAsState(BlockchainCardState(CardState.UNKNOWN))

    when(state.cardState) {
        CardState.UNKNOWN -> {
            // TODO ????
        }
        CardState.NOT_ORDERED -> {
            OrderOrLinkCard(model)
        }
        CardState.CREATED -> {
            // TODO Manage Card Screen
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun MainNavHost(navController: NavHostController, model: BlockchainCardModel) {

    // Coroutine that subscribes to navigation target changes
    LaunchedEffect("navigation") {
        model.navigator.sharedFlow.onEach {
            navController.navigate(it.first.label, it.second)
        }.launchIn(this)
    }

    // Set animations for entering and exiting a screen and setup the navigation routes
    NavHost(
        navController = navController,
        startDestination = "blockchain_card"
    ) {
        composable("order_or_link_card") {
            OrderOrLinkCard(model)
        }

        composable("imgur_post_detail_screen") {
            SelectCardForOrder()
        }
    }
}

@Composable
private fun OrderOrLinkCard(
    model: BlockchainCardModel
) {

    val onOrderCard = {
        model.process(BlockchainCardIntent.OrderCard) // TODO maybe the model should provide this
    }
    val onLinkCard = {
        model.process(BlockchainCardIntent.LinkCard)
    }

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
private fun SelectCardForOrder() {
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
        }

        PrimaryButton(
            text = "Create Card",
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.paddingLarge)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSelectCardForOrder() {
    AppTheme(darkTheme = false) {
        AppSurface {
            SelectCardForOrder()
        }
    }
}

