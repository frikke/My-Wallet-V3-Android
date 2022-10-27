package com.blockchain.blockchaincard.ui.composables.ordercard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddressType
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ExpandableSimpleText
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.system.LinearProgressBar
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.system.Webview
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.Tag
import com.blockchain.componentlib.tag.TagSize
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.UltraLight
import com.blockchain.componentlib.theme.White
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OrderCard(
    viewModel: OrderCardViewModel
) {
    OrderCardIntro(
        onOrderCard = {
            viewModel.onIntent(
                BlockchainCardIntent.HowToOrderCard
            )
        }
    )
}

@Composable
fun OrderCardIntro(onOrderCard: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(AppTheme.dimensions.largeSpacing)
        ) {
            Image(
                painter = painterResource(id = R.drawable.card_intro),
                contentDescription = stringResource(id = R.string.blockchain_card),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )

            Spacer(Modifier.size(AppTheme.dimensions.standardSpacing))

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.card_intro_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(Modifier.size(AppTheme.dimensions.tinySpacing))

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.order_card_intro),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Muted,
                gravity = ComposeGravities.Centre
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(id = R.string.order_my_card),
                onClick = onOrderCard,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            SimpleText(
                text = stringResource(R.string.bc_card_order_intro_legal_disclaimer),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Dark,
                gravity = ComposeGravities.Centre
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderCardIntroPreview() {
    OrderCardIntro { }
}

@Composable
fun HowToOrderCard(onCloseBottomSheet: () -> Unit, onContinue: () -> Unit) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        White
    } else {
        Dark800
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
            .background(backgroundColor)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = CenterHorizontally
    ) {
        SheetHeader(
            title = stringResource(id = R.string.blockchain_card),
            onClosePress = onCloseBottomSheet,
            shouldShowDivider = false
        )

        Image(
            painter = painterResource(id = R.drawable.card_with_badge),
            contentDescription = stringResource(id = R.string.blockchain_card)
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.mediumSpacing))

        SimpleText(
            text = stringResource(R.string.bc_card_how_to_order_title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        SimpleText(
            text = stringResource(R.string.bc_card_how_to_order_secondary_title),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            SimpleText(
                text = stringResource(R.string.bc_card_instructions_title),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )

            Tag(
                text = stringResource(R.string.bc_card_order_time_estimation),
                size = TagSize.Primary,
                defaultBackgroundColor = Grey000,
                defaultTextColor = AppTheme.colors.title,
                startImageResource = ImageResource.None,
                endImageResource = ImageResource.None,
                onClick = {},
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = AppTheme.dimensions.borderSmall, color = AppTheme.colors.body)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_card_verify_address_identity),
                secondaryText = stringResource(R.string.bc_card_verify_address_identity_description),
                startImageResource = ImageResource.Local(id = R.drawable.ic_one_circle),
                endImageResource = ImageResource.None,
                onClick = {},
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = AppTheme.dimensions.borderSmall, color = AppTheme.colors.body)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_choose_order_card),
                secondaryText = stringResource(R.string.bc_card_choose_order_description),
                startImageResource = ImageResource.Local(id = R.drawable.ic_two_circle),
                endImageResource = ImageResource.None,
                onClick = {},
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = 1.dp, color = Grey000)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_card_personal_data_privacy_title),
                secondaryText = stringResource(R.string.bc_card_personal_data_privacy_description),
                startImageResource = ImageResource.Local(id = R.drawable.ic_security),
                endImageResource = ImageResource.None,
                onClick = {},
                backgroundColor = UltraLight
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        PrimaryButton(
            text = stringResource(R.string.common_get_started),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHowToOrderCard() {
    HowToOrderCard({}, {})
}

@Composable
fun OrderCardAddressKYC(
    onContinue: () -> Unit,
    onCheckBillingAddress: () -> Unit,
    line1: String?,
    city: String?,
    postalCode: String?,
    isAddressLoading: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(top = AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(R.string.address_verification_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

            SimpleText(
                text = stringResource(R.string.address_verification_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
            )

            if (!isAddressLoading) {

                Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

                SimpleText(
                    text = stringResource(R.string.bc_card_kyc_address_input_title),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.dimensions.standardSpacing)
                )

                Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.dimensions.standardSpacing),
                    border = BorderStroke(1.dp, Grey000),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 0.dp
                ) {
                    line1?.let {
                        DefaultTableRow(
                            primaryText = line1,
                            secondaryText = "$city, $postalCode",
                            onClick = onCheckBillingAddress,
                            endImageResource = ImageResource.Local(
                                R.drawable.ic_edit,
                                colorFilter = ColorFilter.tint(AppTheme.colors.primary)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

                SimpleText(
                    text = stringResource(R.string.bc_card_kyc_commercial_address_not_accepted),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.dimensions.standardSpacing)
                )
            } else {
                ShimmerLoadingTableRow()
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
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
        line1 = "123 Main St, New York, NY 10001",
        city = "Sacramento",
        postalCode = "CA 93401",
        isAddressLoading = false,
    )
}

const val SSN_LENGTH = 9

@Composable
fun OrderCardSsnKYC(onContinue: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {

        var ssn by remember { mutableStateOf("") }

        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(top = AppTheme.dimensions.largeSpacing)
        ) {
            SimpleText(
                text = stringResource(R.string.verify_your_identity),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

            SimpleText(
                text = stringResource(R.string.verify_your_identity_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
            )

            var hideSSN by remember { mutableStateOf(true) }
            val trailingIcon = if (hideSSN) {
                ImageResource.Local(
                    id = R.drawable.ic_visible_off_filled,
                    colorFilter = ColorFilter.tint(
                        Grey400
                    )
                )
            } else {
                ImageResource.Local(
                    id = R.drawable.ic_visible_filled,
                    colorFilter = ColorFilter.tint(
                        Grey400
                    )
                )
            }

            OutlinedTextInput(
                value = ssn,
                label = stringResource(R.string.ssn_title),
                placeholder = stringResource(R.string.ssn_hint),
                onValueChange = { if (it.length <= SSN_LENGTH) ssn = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (hideSSN) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.padding(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    top = AppTheme.dimensions.standardSpacing
                ),
                unfocusedTrailingIcon = trailingIcon,
                focusedTrailingIcon = trailingIcon,
                onTrailingIconClicked = { hideSSN = !hideSSN }
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

            SimpleText(
                text = stringResource(R.string.bc_card_kyc_ssn_secured_with_encryption),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
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

@OptIn(ExperimentalPagerApi::class, ExperimentalAnimationApi::class)
@Composable
fun CardProductPicker(
    cardProducts: List<BlockchainCardProduct>,
    onContinue: (BlockchainCardProduct) -> Unit,
    onSeeProductDetails: () -> Unit,
) {

    Box(modifier = Modifier.fillMaxSize()) {

        var selectedProduct by remember { mutableStateOf(cardProducts.first()) }

        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.standardSpacing)
        ) {

            val pagerState = rememberPagerState()

            LaunchedEffect(pagerState) {
                // Collect from the pager state a snapshotFlow reading the currentPage
                snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
                    selectedProduct = cardProducts[page]
                }
            }

            HorizontalPager(count = cardProducts.size, state = pagerState) { page ->
                val productImage = when (cardProducts[page].type) {
                    BlockchainCardType.VIRTUAL -> R.drawable.card_front_virtual_big
                    BlockchainCardType.PHYSICAL -> R.drawable.card_front_physical_big
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = productImage),
                        contentScale = ContentScale.Crop,
                        contentDescription = stringResource(id = R.string.blockchain_card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTheme.dimensions.epicSpacing)
                    )
                }
            }

            AnimatedContent(
                targetState = selectedProduct,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150, delayMillis = 90)) +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(150, delayMillis = 90)) with
                        fadeOut(animationSpec = tween(90))
                }
            ) { selectedProduct ->
                val productTitle = when (selectedProduct.type) {
                    BlockchainCardType.VIRTUAL -> stringResource(R.string.virtual)
                    BlockchainCardType.PHYSICAL -> stringResource(R.string.physical)
                }

                val productDescription = when (selectedProduct.type) {
                    BlockchainCardType.VIRTUAL -> stringResource(R.string.bc_card_virtual_card_description)
                    BlockchainCardType.PHYSICAL -> stringResource(R.string.bc_card_physical_card_description)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = CenterHorizontally
                ) {
                    SimpleText(
                        modifier = Modifier.fillMaxWidth(),
                        text = productTitle,
                        style = ComposeTypographies.Title2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )

                    SmallVerticalSpacer()

                    SimpleText(
                        modifier = Modifier.fillMaxWidth(),
                        text = productDescription,
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )

                    SmallVerticalSpacer()

                    MinimalButton(
                        text = stringResource(R.string.see_card_benefits),
                        onClick = onSeeProductDetails,
                        state = ButtonState.Enabled,
                        modifier = Modifier.wrapContentWidth(),
                        shape = AppTheme.shapes.extraLarge
                    )
                }
            }

            SmallVerticalSpacer()

            HorizontalPagerIndicator(
                pagerState = pagerState,
                activeColor = ComposeColors.Primary.toComposeColor(),
                modifier = Modifier
                    .align(CenterHorizontally),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {

            SimpleText(
                text = stringResource(R.string.bc_card_dashboard_legal_disclaimer),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Dark,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            PrimaryButton(
                text = stringResource(id = R.string.common_continue),
                onClick = {
                    onContinue(selectedProduct)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCardProductPicker() {
    AppTheme(darkTheme = false) {
        AppSurface {
            CardProductPicker(
                cardProducts = listOf(
                    BlockchainCardProduct(
                        productCode = "PHYSICAL",
                        price = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
                        brand = BlockchainCardBrand.VISA,
                        type = BlockchainCardType.PHYSICAL
                    ),
                    BlockchainCardProduct(
                        productCode = "VIRTUAL",
                        price = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
                        brand = BlockchainCardBrand.VISA,
                        type = BlockchainCardType.VIRTUAL
                    )
                ),
                onContinue = {},
                onSeeProductDetails = {}
            )
        }
    }
}

@Composable
fun ProductDetails(
    onCloseProductDetailsBottomSheet: () -> Unit
) {

    val backgroundColor = if (!isSystemInDarkTheme()) {
        White
    } else {
        Dark800
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
    ) {

        SheetHeader(
            title = stringResource(id = R.string.card_benefits),
            onClosePress = onCloseProductDetailsBottomSheet,
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        SimpleText(
            text = stringResource(R.string.card_benefits),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = 1.dp, color = Grey000)
        ) {
            DefaultTableRow(
                primaryText = stringResource(id = R.string.no_fees_title),
                secondaryText = stringResource(id = R.string.no_fees_description),
                startImageResource = ImageResource.Local(id = R.drawable.ic_flag),
                endImageResource = ImageResource.None,
                onClick = {},
                backgroundColor = UltraLight
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = 1.dp, color = Grey000)
        ) {
            DefaultTableRow(
                primaryText = stringResource(id = R.string.crypto_back_title),
                secondaryText = stringResource(id = R.string.crypto_back_description),
                startImageResource = ImageResource.Local(id = R.drawable.ic_gift),
                endImageResource = ImageResource.None,
                onClick = {},
                backgroundColor = UltraLight
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        // Spend Limits
        SimpleText(
            text = stringResource(R.string.bc_card_spend_limits),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = 1.dp, color = Grey000),
            backgroundColor = UltraLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                // Daily
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.smallSpacing)
                ) {
                    SimpleText(
                        text = stringResource(R.string.common_daily),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = "$2,500", // TODO(labreu): hardcoded until BE provides this
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Monthly
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.smallSpacing)
                ) {
                    SimpleText(
                        text = stringResource(R.string.common_monthly),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = "$75,000", // TODO(labreu): hardcoded until BE provides this
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Per Transaction
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimensions.smallSpacing)
                ) {
                    SimpleText(
                        text = stringResource(R.string.common_per_transaction),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    SimpleText(
                        text = "$2,500", // TODO(labreu): hardcoded until BE provides this
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        SimpleText(
            text = stringResource(R.string.bc_card_daily_transaction_limit_description),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        SimpleText(
            text = stringResource(R.string.bc_card_atm_limits),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

        Card(
            shape = AppTheme.shapes.medium,
            elevation = 0.dp,
            border = BorderStroke(width = 1.dp, color = Grey000),
            backgroundColor = UltraLight
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                SimpleText(
                    text = stringResource(R.string.common_withdrawal),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )

                SimpleText(
                    text = "$1,000/daily", // TODO(labreu): hardcoded until BE provides this
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        SimpleText(
            text = stringResource(R.string.bc_card_atm_limits_disclaimer),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        SimpleText(
            text = stringResource(R.string.bc_card_benefits_disclaimer),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Dark,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProductDetails() {
    AppTheme(darkTheme = false) {
        AppSurface {
            ProductDetails({})
        }
    }
}

@Composable
fun ReviewAndSubmit(
    firstAndLastName: String?,
    shippingAddress: BlockchainCardAddress?,
    cardProductType: BlockchainCardType?,
    isLegalDocReviewComplete: Boolean = false,
    onChangeShippingAddress: () -> Unit,
    onSeeLegalDocuments: () -> Unit,
    onCreateCard: () -> Unit,
    onChangeSelectedProduct: () -> Unit,
) {

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.dimensions.smallSpacing, horizontal = AppTheme.dimensions.standardSpacing)
        ) {

            SimpleText(
                text = stringResource(R.string.bc_card_review_and_submit_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            SimpleText(
                text = stringResource(R.string.bc_card_review_and_submit_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

            // Full Name
            SimpleText(
                text = stringResource(R.string.full_name),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

            firstAndLastName?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    border = BorderStroke(1.dp, Grey000),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 0.dp
                ) {
                    DefaultTableRow(
                        primaryText = firstAndLastName,
                        secondaryText = stringResource(R.string.bc_card_want_to_update_name),
                        onClick = {},
                        endImageResource = ImageResource.None
                    )
                }
            }

            if (cardProductType == BlockchainCardType.PHYSICAL) {

                Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

                // Shipping Address
                SimpleText(
                    text = stringResource(R.string.bc_card_shipping_address_input_title),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    border = BorderStroke(1.dp, Grey000),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 0.dp
                ) {
                    shippingAddress?.let {
                        DefaultTableRow(
                            primaryText = shippingAddress.line1,
                            secondaryText = "${shippingAddress.city}, ${shippingAddress.postCode}",
                            onClick = onChangeShippingAddress,
                            endImageResource = ImageResource.Local(
                                R.drawable.ic_edit,
                                colorFilter = ColorFilter.tint(AppTheme.colors.primary)
                            )
                        )
                    }
                }
            }

            // Card Selected
            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            SimpleText(
                text = stringResource(R.string.bc_card_selected_input_title),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                border = BorderStroke(1.dp, Grey000),
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp
            ) {
                cardProductType?.let {
                    val productName = when (cardProductType) {
                        BlockchainCardType.VIRTUAL -> stringResource(R.string.bc_card_virtual)
                        BlockchainCardType.PHYSICAL -> stringResource(R.string.bc_card_physical)
                    }

                    val productImage = when (cardProductType) {
                        BlockchainCardType.VIRTUAL -> ImageResource.Local(R.drawable.card_front_virtual)
                        BlockchainCardType.PHYSICAL -> ImageResource.Local(R.drawable.card_front_physical)
                    }

                    DefaultTableRow(
                        primaryText = productName,
                        secondaryText = stringResource(R.string.bc_card_visa_title),
                        onClick = onChangeSelectedProduct,
                        startImageResource = productImage,
                        endImageResource = ImageResource.Local(
                            R.drawable.ic_edit,
                            colorFilter = ColorFilter.tint(AppTheme.colors.primary)
                        )
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
                .align(Alignment.BottomCenter),
            horizontalAlignment = CenterHorizontally
        ) {

            val termsAndConditionsCheckboxState = remember { mutableStateOf(CheckboxState.Unchecked) }
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(bottom = AppTheme.dimensions.largeSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    modifier = Modifier.padding(AppTheme.dimensions.tinySpacing),
                    state = termsAndConditionsCheckboxState.value,
                    onCheckChanged = { checked ->
                        if (checked) {
                            if (isLegalDocReviewComplete) {
                                termsAndConditionsCheckboxState.value = CheckboxState.Checked
                            } else {
                                onSeeLegalDocuments()
                            }
                        } else {
                            termsAndConditionsCheckboxState.value = CheckboxState.Unchecked
                        }
                    },
                )

                ExpandableSimpleText(
                    text = stringResource(id = R.string.bc_card_terms_and_conditions_label),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeeLegalDocuments() },
                    maxLinesWhenCollapsed = 3
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
}

@Preview(showBackground = true)
@Composable
fun PreviewReviewAndSubmit() {
    ReviewAndSubmit(
        firstAndLastName = "Jason Bourne",
        shippingAddress = BlockchainCardAddress(
            line1 = "123 Main St",
            line2 = "Apt 1",
            city = "New York",
            state = "NY",
            postCode = "4444",
            country = "US",
            addressType = BlockchainCardAddressType.SHIPPING
        ),
        cardProductType = BlockchainCardType.PHYSICAL,
        onChangeShippingAddress = { /*TODO*/ },
        onSeeLegalDocuments = { /*TODO*/ },
        onCreateCard = { /*TODO*/ },
        onChangeSelectedProduct = { /*TODO*/ },
    )
}

@Composable
fun ProductLegalInfo(
    legalDocuments: List<BlockchainCardLegalDocument>,
    onCloseProductLegalInfoBottomSheet: () -> Unit,
    onSeeLegalDocument: (BlockchainCardLegalDocument) -> Unit
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        White
    } else {
        Dark800
    }

    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
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

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            itemsIndexed(items = legalDocuments) { index, legalDocument ->
                DefaultTableRow(
                    primaryText = legalDocument.displayName,
                    onClick = { onSeeLegalDocument(legalDocument) },
                )

                if (index < legalDocuments.lastIndex)
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
fun PreviewProductLegalInfo() {
    AppTheme(darkTheme = false) {
        AppSurface {
            ProductLegalInfo(emptyList(), {}, {})
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
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.processing),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Body,
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
                .padding(AppTheme.dimensions.xHugeSpacing),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.credit_card_success),
                contentDescription = stringResource(R.string.card_created),
                modifier = Modifier.wrapContentWidth(),
            )
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.card_created),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.continue_to_card_dashboard),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
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
                .padding(AppTheme.dimensions.standardSpacing),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.credit_card_failed),
                contentDescription = stringResource(R.string.card_created),
                modifier = Modifier.wrapContentWidth(),
            )
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = errorTitle,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = errorDescription,
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
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
fun LegalDocumentsViewer(
    legalDocuments: List<BlockchainCardLegalDocument>,
    onLegalDocSeen: (documentName: String) -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.tinySpacing)
    ) {

        var currentDocumentIndex by remember { mutableStateOf(0) }

        val currentDocument = legalDocuments[currentDocumentIndex]

        Webview(
            url = currentDocument.url,
            modifier = Modifier
                .padding(AppTheme.dimensions.smallSpacing)
                .weight(0.9f),
            onPageLoaded = { onLegalDocSeen(currentDocument.name) }
        )

        if (currentDocumentIndex < legalDocuments.size - 1) {
            MinimalButton(
                text = stringResource(id = R.string.common_next) +
                    " (${currentDocumentIndex + 1}/${legalDocuments.size})",
                state = ButtonState.Enabled,
                onClick = { currentDocumentIndex++ },
                modifier = Modifier
                    .padding(AppTheme.dimensions.standardSpacing)
                    .fillMaxWidth()
                    .weight(0.1f)
            )
        } else {
            PrimaryButton(
                text = stringResource(id = R.string.done),
                state = ButtonState.Enabled,
                onClick = onFinish,
                modifier = Modifier
                    .padding(AppTheme.dimensions.standardSpacing)
                    .fillMaxWidth()
                    .weight(0.1f)
            )
        }

        LinearProgressBar(
            progress = (currentDocumentIndex + 1) / legalDocuments.size.toFloat(),
        )
    }
}

@Composable
fun LegalDocument(legalDocument: BlockchainCardLegalDocument) {
    Webview(
        url = legalDocument.url,
        modifier = Modifier
            .padding(top = AppTheme.dimensions.smallSpacing)
    )
}
