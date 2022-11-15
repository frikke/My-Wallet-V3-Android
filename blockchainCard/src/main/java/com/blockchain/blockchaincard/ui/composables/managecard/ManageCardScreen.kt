package com.blockchain.blockchaincard.ui.composables.managecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.blockchaincard.R
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddressType
import com.blockchain.blockchaincard.domain.models.BlockchainCardBrand
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderState
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatement
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionState
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransactionType
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
import com.blockchain.coincore.AccountBalance
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.DestructivePrimaryButton
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.control.DropdownMenuSearch
import com.blockchain.componentlib.control.Radio
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.divider.VerticalDivider
import com.blockchain.componentlib.lazylist.PaginatedLazyColumn
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.system.Webview
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.ToggleTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.GOOGLE_PAY_BUTTON_BORDER
import com.blockchain.componentlib.theme.GOOGLE_PAY_BUTTON_DIVIDER
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.theme.UltraLight
import com.blockchain.componentlib.theme.White
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.getMonthName
import com.blockchain.utils.toFormattedDateTime
import com.blockchain.utils.toFormattedExpirationDate
import com.blockchain.utils.toLocalTime
import com.blockchain.utils.toShortMonthYearDate
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

@Composable
fun ManageCard(
    card: BlockchainCard?,
    cardWidgetUrl: String?,
    linkedAccountBalance: AccountBalance?,
    isBalanceLoading: Boolean,
    isTransactionListRefreshing: Boolean,
    transactionList: List<BlockchainCardTransaction>?,
    googleWalletState: BlockchainCardGoogleWalletStatus,
    cardOrderState: BlockchainCardOrderState?,
    onViewCardSelector: () -> Unit,
    onManageCardDetails: (BlockchainCard) -> Unit,
    onFundingAccountClicked: () -> Unit,
    onRefreshBalance: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onSeeTransactionDetails: (BlockchainCardTransaction) -> Unit,
    onRefreshTransactions: () -> Unit,
    onRefreshCardWidgetUrl: () -> Unit,
    onAddFunds: () -> Unit,
    onAddToGoogleWallet: () -> Unit,
    onActivateCard: () -> Unit,
    onWebMessageReceived: (String) -> Unit,
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshBalance()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.dimensions.tinySpacing)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.smallSpacing
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                card?.let {
                    SimpleText(
                        text = stringResource(id = card.type.getStringResource()),
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                MinimalButton(
                    text = stringResource(id = R.string.my_cards),
                    onClick = onViewCardSelector,
                    modifier = Modifier
                        .wrapContentWidth()
                        .weight(1.4f),
                    minHeight = 16.dp,
                    shape = AppTheme.shapes.extraLarge
                )
            }

            if (card?.status == BlockchainCardStatus.ACTIVE || card?.status == BlockchainCardStatus.LOCKED) {
                when (cardWidgetUrl) {
                    null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(
                                horizontal = AppTheme.dimensions.smallSpacing,
                                vertical = AppTheme.dimensions.xHugeSpacing
                            )
                        )
                    }

                    "" -> {
                        SimpleText(
                            text = stringResource(R.string.bc_card_unable_to_load_card),
                            style = ComposeTypographies.Body1,
                            color = ComposeColors.Dark,
                            gravity = ComposeGravities.Centre,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppTheme.dimensions.standardSpacing)
                        )

                        SimpleText(
                            text = stringResource(R.string.bc_card_tap_here_to_try_again),
                            style = ComposeTypographies.Caption1,
                            color = ComposeColors.Primary,
                            gravity = ComposeGravities.Centre,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppTheme.dimensions.tinySpacing)
                                .clickable {
                                    onRefreshCardWidgetUrl()
                                }
                        )
                    }

                    else -> {
                        Webview(
                            url = cardWidgetUrl,
                            disableScrolling = true,
                            onWebMessageReceived = onWebMessageReceived,
                            overrideTextZoom = true,
                            modifier = Modifier
                                .padding(
                                    top = AppTheme.dimensions.smallSpacing
                                )
                                .requiredHeight(355.dp)
                                .requiredWidth(400.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

            card?.let {
                if (card.status == BlockchainCardStatus.UNACTIVATED) {
                    Box(
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = AppTheme.colors.light,
                            shape = RoundedCornerShape(16.dp)
                        )
                    ) {
                        when (cardOrderState?.status) {
                            BlockchainCardOrderStatus.PROCESSED,
                            BlockchainCardOrderStatus.PROCESSING -> {
                                DefaultTableRow(
                                    primaryText = buildAnnotatedString {
                                        append(
                                            stringResource(R.string.bc_card_processing_title)
                                        )
                                    },
                                    onClick = {},
                                    secondaryText = buildAnnotatedString {
                                        append(stringResource(R.string.bc_card_processing_subtitle))
                                    },
                                    startImageResource = ImageResource.Local(id = R.drawable.ic_send),
                                    endImageResource = ImageResource.None
                                )
                            }

                            BlockchainCardOrderStatus.SHIPPED -> {
                                DefaultTableRow(
                                    primaryText = buildAnnotatedString {
                                        append(
                                            stringResource(R.string.bc_card_shipped_title)
                                        )
                                    },
                                    onClick = onActivateCard,
                                    secondaryText = buildAnnotatedString {
                                        append(stringResource(R.string.bc_card_shipped_subtitle))
                                    },
                                    startImageResource = ImageResource.Local(id = R.drawable.credit_card),
                                )
                            }

                            BlockchainCardOrderStatus.DELIVERED -> {
                                DefaultTableRow(
                                    primaryText = buildAnnotatedString {
                                        append(
                                            stringResource(R.string.bc_card_delivered_title)
                                        )
                                    },
                                    onClick = {},
                                    secondaryText = buildAnnotatedString {
                                        append(
                                            stringResource(R.string.bc_card_delivered_subtitle)
                                        )
                                    },
                                    startImageResource = ImageResource.Local(id = R.drawable.credit_card),
                                )
                            }
                            else -> {}
                        }
                    }

                    Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
                }
            }

            Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = UltraLight
            ) {
                if (linkedAccountBalance != null)
                    Column(modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)) {

                        val hasFunds = when (linkedAccountBalance.total) {
                            is FiatValue -> linkedAccountBalance.totalFiat.isPositive
                            is CryptoValue -> linkedAccountBalance.total.isPositive
                            else -> false
                        }

                        if (!hasFunds) {
                            Box(
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = AppTheme.colors.warning,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            ) {
                                DefaultTableRow(
                                    primaryText = stringResource(R.string.bc_card_out_of_funds_top_up),
                                    onClick = onAddFunds
                                )
                            }

                            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
                        }

                        SimpleText(
                            text = stringResource(id = R.string.bc_card_transaction_payment_method),
                            style = ComposeTypographies.Paragraph1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start
                        )

                        Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

                        FundingAccount(
                            accountBalance = linkedAccountBalance,
                            onFundingAccountClicked = onFundingAccountClicked,
                        )
                    }
                else if (isBalanceLoading)
                    ShimmerLoadingTableRow()
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        when (googleWalletState) {
            BlockchainCardGoogleWalletStatus.NOT_ADDED -> GooglePayButton(
                onClick = onAddToGoogleWallet,
                modifier = Modifier.requiredWidth(153.dp)
            )
            BlockchainCardGoogleWalletStatus.ADDED -> {}
            BlockchainCardGoogleWalletStatus.ADD_IN_PROGRESS -> CircularProgressBar()
            BlockchainCardGoogleWalletStatus.ADD_SUCCESS -> GooglePayButtonAddSuccess()
            BlockchainCardGoogleWalletStatus.ADD_FAILED -> GooglePayButtonAddFailed()
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    AppTheme.dimensions.smallSpacing
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.dimensions.smallSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                SimpleText(
                    text = stringResource(R.string.bc_card_transactions_title),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                    modifier = Modifier.wrapContentWidth(),
                    isMultiline = false
                )

                if (transactionList != null && transactionList.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))

                    MinimalButton(
                        text = stringResource(R.string.bc_card_see_all),
                        onClick = onSeeAllTransactions,
                        modifier = Modifier
                            .wrapContentWidth()
                            .weight(1f),
                        minHeight = 16.dp,
                        shape = AppTheme.shapes.extraLarge
                    )
                }
            }

            when {
                transactionList == null -> {
                    ShimmerLoadingTableRow()
                }
                transactionList.isEmpty() -> {

                    Image(
                        painter = painterResource(id = R.drawable.empty_transactions_graphic),
                        contentDescription = stringResource(R.string.recent_purchases_here),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.standardSpacing),
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_empty_transactions_title),
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.tinySpacing)
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_empty_transactions_description),
                        style = ComposeTypographies.Paragraph1,
                        color = ComposeColors.Dark,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.tinySpacing)
                    )

                    SimpleText(
                        text = stringResource(R.string.bc_card_tap_here_to_refresh),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Primary,
                        gravity = ComposeGravities.Centre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = AppTheme.dimensions.tinySpacing,
                                bottom = AppTheme.dimensions.standardSpacing
                            )
                            .clickable {
                                onRefreshTransactions()
                            }
                    )
                }
                else -> {
                    CardTransactionList(
                        transactionList = transactionList,
                        onSeeTransactionDetails = onSeeTransactionDetails
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            SimpleText(
                text = stringResource(R.string.bc_card_dashboard_legal_disclaimer),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Dark,
                gravity = ComposeGravities.Centre
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewManageCard() {
    ManageCard(
        card = BlockchainCard(
            id = "",
            type = BlockchainCardType.PHYSICAL,
            last4 = "",
            expiry = "",
            brand = BlockchainCardBrand.VISA,
            status = BlockchainCardStatus.UNACTIVATED,
            orderStatus = null,
            createdAt = ""
        ),
        cardWidgetUrl = null,
        linkedAccountBalance = null,
        isBalanceLoading = false,
        isTransactionListRefreshing = false,
        transactionList = null,
        googleWalletState = BlockchainCardGoogleWalletStatus.NOT_ADDED,
        cardOrderState = BlockchainCardOrderState(
            status = BlockchainCardOrderStatus.PROCESSING,
            address = BlockchainCardAddress(
                line1 = "",
                line2 = "",
                postCode = "",
                city = "",
                state = "",
                country = "",
                BlockchainCardAddressType.SHIPPING
            )
        ),
        onViewCardSelector = {},
        onManageCardDetails = {},
        onFundingAccountClicked = {},
        onRefreshBalance = {},
        onSeeAllTransactions = {},
        onSeeTransactionDetails = {},
        onRefreshTransactions = {},
        onRefreshCardWidgetUrl = {},
        onAddFunds = {},
        onAddToGoogleWallet = {},
        onActivateCard = {},
        onWebMessageReceived = {},
    )
}

@Composable
fun GooglePayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(AppTheme.dimensions.borderSmall, GOOGLE_PAY_BUTTON_BORDER),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Black),
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = R.drawable.add_to_googlepay_button_content),
            contentDescription = stringResource(R.string.add_to_google_pay),
            modifier = Modifier.wrapContentSize()
        )
    }
}

@Composable
fun GooglePayButtonAddSuccess(modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {},
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(AppTheme.dimensions.borderSmall, GOOGLE_PAY_BUTTON_BORDER),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Black),
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Image(
            painter = painterResource(id = R.drawable.googlepay_button_content),
            contentDescription = stringResource(R.string.add_to_google_pay),
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.width(AppTheme.dimensions.tinySpacing))
        VerticalDivider(
            dividerColor = GOOGLE_PAY_BUTTON_DIVIDER,
            modifier = Modifier.fillMaxHeight()
        )
        Spacer(modifier = Modifier.width(AppTheme.dimensions.tinySpacing))

        SimpleText(
            text = stringResource(R.string.card_added_to_google_pay),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Light,
            gravity = ComposeGravities.Centre
        )
    }
}

@Composable
fun GooglePayButtonAddFailed(modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {},
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(AppTheme.dimensions.borderSmall, GOOGLE_PAY_BUTTON_BORDER),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Black),
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Image(
            painter = painterResource(id = R.drawable.googlepay_button_content),
            contentDescription = stringResource(R.string.add_to_google_pay),
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.width(AppTheme.dimensions.tinySpacing))
        VerticalDivider(
            dividerColor = GOOGLE_PAY_BUTTON_DIVIDER,
            modifier = Modifier.fillMaxHeight()
        )
        Spacer(modifier = Modifier.width(AppTheme.dimensions.tinySpacing))

        SimpleText(
            text = stringResource(R.string.failed_to_add_card_to_google_pay),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Error,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewGooglePayButtonAddFailed() {
    GooglePayButtonAddFailed()
}

@Preview(showBackground = true)
@Composable
private fun PreviewGooglePayButtonAddSuccess() {
    GooglePayButtonAddSuccess()
}

@Preview(showBackground = true)
@Composable
private fun PreviewGooglePayButton() {
    GooglePayButton(onClick = { /*TODO*/ })
}

@Composable
fun CardSelector(
    cards: List<BlockchainCard>,
    defaultCardId: String,
    onOrderCard: () -> Unit,
    onManageCard: (BlockchainCard) -> Unit,
    onViewCard: (BlockchainCard) -> Unit,
    onSetCardAsDefault: (String) -> Unit,
    onRefreshCards: () -> Unit
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshCards()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedIndex by remember {
        val indexOfDefaultCard = cards.indexOfFirst { defaultCardId == it.id }
        if (indexOfDefaultCard > 0) {
            mutableStateOf(indexOfDefaultCard)
        } else {
            mutableStateOf(0)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.dimensions.smallSpacing
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleText(
                text = stringResource(R.string.my_card),
                style = ComposeTypographies.Body2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
            )

            MinimalButton(
                text = "Add Card",
                onClick = onOrderCard,
                modifier = Modifier
                    .wrapContentWidth(),
                minHeight = 16.dp,
                shape = AppTheme.shapes.small
            )
        }

        LazyColumn(
            modifier = Modifier.padding(vertical = AppTheme.dimensions.smallSpacing),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing),
        ) {
            itemsIndexed(items = cards) { index, card ->
                CardSelectorItem(
                    cardType = card.type,
                    last4digits = card.last4,
                    cardStatus = card.status,
                    expDate = card.expiry,
                    isSelected = selectedIndex == index,
                    isDefault = defaultCardId == card.id,
                    onManageCard = {
                        selectedIndex = index
                        onManageCard(card)
                    },
                    onViewCard = {
                        selectedIndex = index
                        onViewCard(card)
                    },
                    onSetCardAsDefault = { setAsDefault ->
                        if (setAsDefault) onSetCardAsDefault(card.id)
                        else onSetCardAsDefault("")
                    },
                    modifier = Modifier.clickable {
                        selectedIndex = index
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCardSelector() {

    val cards = listOf(
        BlockchainCard(
            id = "1111",
            type = BlockchainCardType.PHYSICAL,
            last4 = "1234",
            expiry = "1222",
            brand = BlockchainCardBrand.VISA,
            status = BlockchainCardStatus.ACTIVE,
            orderStatus = null,
            createdAt = "1999-11-11"
        ),
        BlockchainCard(
            id = "1111",
            type = BlockchainCardType.VIRTUAL,
            last4 = "1234",
            expiry = "1222",
            brand = BlockchainCardBrand.VISA,
            status = BlockchainCardStatus.ACTIVE,
            orderStatus = null,
            createdAt = "1999-11-11"
        ),
        BlockchainCard(
            id = "1111",
            type = BlockchainCardType.VIRTUAL,
            last4 = "1234",
            expiry = "1222",
            brand = BlockchainCardBrand.VISA,
            status = BlockchainCardStatus.ACTIVE,
            orderStatus = null,
            createdAt = "1999-11-11"
        ),
        BlockchainCard(
            id = "1111",
            type = BlockchainCardType.VIRTUAL,
            last4 = "1234",
            expiry = "1222",
            brand = BlockchainCardBrand.VISA,
            status = BlockchainCardStatus.ACTIVE,
            orderStatus = null,
            createdAt = "1999-11-11"
        ),
    )

    CardSelector(
        cards = cards,
        onManageCard = {},
        onViewCard = {},
        onOrderCard = {},
        defaultCardId = "",
        onSetCardAsDefault = {},
        onRefreshCards = {}
    )
}

@Composable
fun CardSelectorItem(
    cardType: BlockchainCardType,
    cardStatus: BlockchainCardStatus,
    last4digits: String,
    expDate: String,
    isSelected: Boolean,
    isDefault: Boolean,
    modifier: Modifier = Modifier,
    onManageCard: () -> Unit,
    onViewCard: () -> Unit,
    onSetCardAsDefault: (Boolean) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {

        Card(
            modifier = modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = if (isSelected) 2.dp else 0.dp,
            shape = AppTheme.shapes.medium,
            backgroundColor = AppTheme.colors.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.standardSpacing)
                    .heightIn(max = 112.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (cardType) {
                    BlockchainCardType.PHYSICAL -> {
                        Image(
                            painter = painterResource(id = R.drawable.card_front_physical),
                            contentDescription = "Blockchain Card",
                            contentScale = ContentScale.Inside
                        )
                    }
                    BlockchainCardType.VIRTUAL -> {
                        Image(
                            painter = painterResource(id = R.drawable.card_front_virtual),
                            contentDescription = "Blockchain Card",
                            contentScale = ContentScale.Inside
                        )
                    }
                }

                Spacer(Modifier.width(AppTheme.dimensions.verySmallSpacing))

                Column(modifier = Modifier.fillMaxWidth()) {

                    Row(modifier = Modifier.fillMaxWidth()) {

                        Column(modifier = Modifier.weight(1f)) {

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SimpleText(
                                    text = stringResource(id = cardType.getStringResource()),
                                    style = ComposeTypographies.Paragraph2,
                                    color = ComposeColors.Title,
                                    gravity = ComposeGravities.Start
                                )

                                SimpleText(
                                    text = "•••• $last4digits",
                                    style = ComposeTypographies.Paragraph2,
                                    color = ComposeColors.Title,
                                    gravity = ComposeGravities.Start
                                )
                            }

                            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallestSpacing))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SimpleText(
                                    text = stringResource(id = cardStatus.getStringResource()),
                                    style = ComposeTypographies.Paragraph1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.Start
                                )

                                SimpleText(
                                    text = expDate.toFormattedExpirationDate(),
                                    style = ComposeTypographies.Paragraph1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.Start
                                )
                            }
                        }

                        Spacer(Modifier.width(AppTheme.dimensions.tinySpacing))

                        Radio(state = if (isSelected) RadioButtonState.Selected else RadioButtonState.Unselected)
                    }

                    Spacer(Modifier.height(AppTheme.dimensions.smallSpacing))

                    Row(modifier = Modifier.requiredHeightIn(min = 32.dp)) {
                        // Manage Button
                        if (cardStatus != BlockchainCardStatus.TERMINATED) {
                            MinimalButton(
                                text = stringResource(id = R.string.manage),
                                onClick = onManageCard,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.width(AppTheme.dimensions.tinySpacing))

                        if (isSelected) {
                            // View Button
                            PrimaryButton(
                                text = stringResource(id = R.string.view),
                                onClick = onViewCard,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (isSelected && cardStatus != BlockchainCardStatus.TERMINATED) {

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = modifier
                    .clickable { onSetCardAsDefault(!isDefault) }
                    .border(1.dp, Grey000, AppTheme.shapes.medium)
            ) {
                Checkbox(
                    modifier = Modifier.padding(dimensionResource(R.dimen.very_small_spacing)),
                    state = if (isDefault) CheckboxState.Checked else CheckboxState.Unchecked,
                    onCheckChanged = onSetCardAsDefault
                )

                SimpleText(
                    text = stringResource(R.string.bc_card_make_default),
                    modifier = Modifier
                        .weight(1f, false)
                        .padding(AppTheme.dimensions.smallSpacing),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCardSelectorItem() {
    CardSelectorItem(
        cardType = BlockchainCardType.PHYSICAL,
        last4digits = "1234",
        cardStatus = BlockchainCardStatus.ACTIVE,
        expDate = "1222",
        isSelected = true,
        isDefault = false,
        onManageCard = {},
        onViewCard = {},
        onSetCardAsDefault = {}
    )
}

@Composable
fun ManageCardDetails(
    last4digits: String,
    onToggleLockCard: (Boolean) -> Unit,
    cardStatus: BlockchainCardStatus,
    onSeePersonalDetails: () -> Unit,
    onSeeTransactionControls: () -> Unit,
    onSeeSupport: () -> Unit,
    onSeeDocuments: () -> Unit,
    onTerminateCard: () -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {

        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.manage_card),
            shouldShowDivider = false
        )

        // Card details
        CardDetailsBottomSheetElement(
            cardStatus = cardStatus,
            last4digits = last4digits,
            modifier = Modifier.padding(
                AppTheme.dimensions.standardSpacing,
                AppTheme.dimensions.smallSpacing
            )
        )

        SmallVerticalSpacer()

        SimpleText(
            text = stringResource(R.string.account),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        SmallestVerticalSpacer()

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Basic Info
                DefaultTableRow(
                    primaryText = stringResource(R.string.basic_info),
                    onClick = onSeePersonalDetails,
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Basic Info
                DefaultTableRow(
                    primaryText = stringResource(R.string.documents),
                    onClick = onSeeDocuments,
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }

        TinyVerticalSpacer()

        // Actions
        SimpleText(
            text = stringResource(R.string.actions),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        SmallestVerticalSpacer()

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Lock card
                ToggleTableRow(
                    onCheckedChange = onToggleLockCard,
                    isChecked = cardStatus == BlockchainCardStatus.LOCKED,
                    primaryText = stringResource(R.string.bc_card_freeze),
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Support
                DefaultTableRow(
                    primaryText = stringResource(R.string.support),
                    onClick = onSeeSupport,
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                // Terminate Card
                DefaultTableRow(
                    primaryText = stringResource(R.string.terminate_card),
                    onClick = onTerminateCard,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewManageCardDetails() {
    ManageCardDetails("***3458", {}, BlockchainCardStatus.ACTIVE, {}, {}, {}, {}, {}, {})
}

@Composable
fun CardTransactionList(
    transactionList: List<BlockchainCardTransaction>,
    onSeeTransactionDetails: (transaction: BlockchainCardTransaction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Grey000),
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            transactionList.forEachIndexed { index, transaction ->
                CardTransactionItem(
                    merchantName = transaction.merchantName,
                    timestamp = transaction.userTransactionTime,
                    amount = transaction.fundingAmount.toStringWithSymbol(),
                    state = transaction.state,
                    isRefund = transaction.type == BlockchainCardTransactionType.REFUND,
                    onClick = { onSeeTransactionDetails(transaction) }
                )
                if (index < transactionList.lastIndex)
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
fun PreviewCardTransactionList() {

    val transactionList = listOf(
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.REFUND,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        )
    )

    CardTransactionList(
        transactionList = transactionList,
        onSeeTransactionDetails = {}
    )
}

@Composable
fun CardTransactionHistory(
    pendingTransactions: List<BlockchainCardTransaction>,
    completedTransactionsGroupedByMonth: Map<String?, List<BlockchainCardTransaction>>,
    onSeeTransactionDetails: (transaction: BlockchainCardTransaction) -> Unit,
    onGetNextPage: () -> Unit,
    onRefreshTransactions: () -> Unit,
    isTransactionListRefreshing: Boolean,
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isTransactionListRefreshing),
        onRefresh = onRefreshTransactions
    ) {
        PaginatedLazyColumn(
            modifier = Modifier.padding(AppTheme.dimensions.standardSpacing),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.standardSpacing),
            onGetNextPage = onGetNextPage
        ) {
            if (pendingTransactions.isNotEmpty()) {
                item {
                    SimpleText(
                        text = stringResource(id = R.string.bc_card_transaction_pending),
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Grey000),
                        elevation = 0.dp,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            pendingTransactions.forEachIndexed { index, transaction ->
                                CardTransactionItem(
                                    merchantName = transaction.merchantName,
                                    timestamp = transaction.userTransactionTime,
                                    amount = transaction.fundingAmount.toStringWithSymbol(),
                                    state = transaction.state,
                                    isRefund = transaction.type == BlockchainCardTransactionType.REFUND,
                                    onClick = { onSeeTransactionDetails(transaction) }
                                )
                                if (index < pendingTransactions.lastIndex)
                                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            if (completedTransactionsGroupedByMonth.isNotEmpty()) {
                completedTransactionsGroupedByMonth.forEach { (month, transactions) ->
                    if (month != null) {
                        item {
                            SimpleText(
                                text = month,
                                style = ComposeTypographies.Body2,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.Start
                            )

                            Spacer(modifier = Modifier.height(AppTheme.dimensions.tinySpacing))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Grey000),
                                elevation = 0.dp,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column {
                                    transactions.forEachIndexed { index, transaction ->
                                        CardTransactionItem(
                                            merchantName = transaction.merchantName,
                                            timestamp = transaction.userTransactionTime,
                                            amount = transaction.fundingAmount.toStringWithSymbol(),
                                            state = transaction.state,
                                            isRefund = transaction.type == BlockchainCardTransactionType.REFUND,
                                            onClick = { onSeeTransactionDetails(transaction) }
                                        )

                                        if (index < transactions.lastIndex)
                                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCardTransactionHistory() {
    val transactionList = listOf(
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-06-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-06-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.DECLINED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.REFUND,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.PENDING,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        ),
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-07-11T17:50:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.PENDING,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            declineReason = null,
            networkConversionRate = null
        )
    )

    val pendingTransactions = transactionList.filter {
        it.state == BlockchainCardTransactionState.PENDING
    }
    val completedTransactionsGroupedByMonth = transactionList.filter {
        it.state != BlockchainCardTransactionState.PENDING
    }.groupBy {
        it.userTransactionTime.fromIso8601ToUtc()?.getMonthName()
    }

    CardTransactionHistory(
        pendingTransactions = pendingTransactions,
        completedTransactionsGroupedByMonth = completedTransactionsGroupedByMonth,
        onSeeTransactionDetails = {},
        onRefreshTransactions = {},
        isTransactionListRefreshing = false,
        onGetNextPage = {}
    )
}

@Composable
fun CardTransactionItem(
    merchantName: String,
    timestamp: String,
    amount: String,
    state: BlockchainCardTransactionState,
    isRefund: Boolean,
    onClick: () -> Unit,
) {
    val transactionTitle: AnnotatedString
    val transactionAmount: AnnotatedString
    val transactionTimestamp: AnnotatedString?
    val transactionIcon: ImageResource

    val transactionTimestampFormatted = timestamp.fromIso8601ToUtc()?.toLocalTime()?.toFormattedDateTime()

    if (isRefund) {
        transactionTitle = buildAnnotatedString {
            append(stringResource(R.string.bc_card_transaction_refund_title, merchantName))
        }
        transactionAmount = buildAnnotatedString { append("+$amount") }
        transactionTimestamp = buildAnnotatedString { transactionTimestampFormatted?.let { append(it) } }
        transactionIcon = ImageResource.LocalWithBackground(
            R.drawable.ic_receive,
            backgroundColour = R.color.paletteBaseLight,
            iconTintColour = R.color.paletteBaseTextTitle,
            alpha = 1F
        )
    } else if (state == BlockchainCardTransactionState.DECLINED || state == BlockchainCardTransactionState.CANCELLED) {
        transactionTitle = buildAnnotatedString { append(merchantName) }
        transactionAmount = buildAnnotatedString {
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append("-$amount")
            }
        }
        transactionTimestamp = buildAnnotatedString {
            withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                append(stringResource(id = state.getStringResource()))
            }
        }
        transactionIcon = ImageResource.LocalWithBackground(
            R.drawable.ic_minus,
            backgroundColour = R.color.paletteBaseLight,
            iconTintColour = R.color.paletteBaseTextTitle,
            alpha = 1F
        )
    } else {
        transactionTitle = buildAnnotatedString { append(merchantName) }
        transactionAmount = buildAnnotatedString { append("-$amount") }
        transactionTimestamp = buildAnnotatedString { transactionTimestampFormatted?.let { append(it) } }
        transactionIcon = ImageResource.LocalWithBackground(
            R.drawable.ic_minus,
            backgroundColour = R.color.paletteBaseLight,
            iconTintColour = R.color.paletteBaseTextTitle,
            alpha = 1F
        )
    }

    DefaultTableRow(
        startImageResource = transactionIcon,
        primaryText = transactionTitle,
        secondaryText = transactionTimestamp,
        endText = transactionAmount,
        endImageResource = ImageResource.None,
        onClick = onClick,
    )
}

@Preview
@Composable
fun PreviewCardTransactionItem() {
    CardTransactionItem(
        merchantName = "Starbucks",
        timestamp = "2020-01-01T00:00:00.000Z",
        amount = "-$1.00",
        state = BlockchainCardTransactionState.COMPLETED,
        isRefund = false,
        onClick = { }
    )
}

@Composable
fun CardTransactionDetails(
    cardTransaction: BlockchainCardTransaction,
    last4digits: String,
    onCloseBottomSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.transaction_details_title),
            shouldShowDivider = false
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.standardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val isRefund = cardTransaction.type == BlockchainCardTransactionType.REFUND

            val transactionAmount =
                when {
                    isRefund -> {
                        buildAnnotatedString {
                            append("+${cardTransaction.originalAmount.toStringWithSymbol()}")
                        }
                    }
                    cardTransaction.state == BlockchainCardTransactionState.DECLINED -> {
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                append("-${cardTransaction.originalAmount.toStringWithSymbol()}")
                            }
                        }
                    }
                    else -> {
                        buildAnnotatedString { append("-${cardTransaction.originalAmount.toStringWithSymbol()}") }
                    }
                }

            val merchantName = cardTransaction.merchantName

            val transactionDateTime =
                cardTransaction.userTransactionTime.fromIso8601ToUtc()?.toLocalTime()?.toFormattedDateTime() ?: ""

            val transactionStatus =
                if (cardTransaction.state == BlockchainCardTransactionState.DECLINED) {
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                            append(cardTransaction.state.toString())
                        }
                    }
                } else {
                    buildAnnotatedString {
                        append(cardTransaction.state.toString())
                    }
                }

            val transactionPaymentMethod = cardTransaction.fundingAmount.currency.networkTicker

            val transactionFee = cardTransaction.fee.toStringWithSymbol()

            val fundingTransactionAmount = cardTransaction.fundingAmount.toStringWithSymbol()

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            // Transaction Amount
            SimpleText(
                text = transactionAmount,
                style = ComposeTypographies.Title1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            // Merchant Name
            SimpleText(
                text = merchantName,
                style = ComposeTypographies.Body2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )

            // Transaction timestamp
            SimpleText(
                text = transactionDateTime,
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            // Transaction Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Grey000),
                shape = RoundedCornerShape(6.dp),
                elevation = 0.dp
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)
                ) {
                    SimpleText(
                        text = stringResource(R.string.bc_card_transaction_status),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start,
                    )

                    SimpleText(
                        text = transactionStatus,
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.smallSpacing))

            // Other Transaction details
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Grey000),
                shape = RoundedCornerShape(6.dp),
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Card last 4 digits
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimensions.smallSpacing)
                    ) {
                        SimpleText(
                            text = stringResource(id = R.string.card),
                            style = ComposeTypographies.Body1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start
                        )

                        SimpleText(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = AppTheme.colors.primary)) {
                                    append("•••• $last4digits")
                                }
                            },
                            style = ComposeTypographies.Body2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())

                    // Payment Method
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimensions.smallSpacing)
                    ) {
                        SimpleText(
                            text = stringResource(R.string.bc_card_transaction_payment_method),
                            style = ComposeTypographies.Body1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start
                        )

                        SimpleText(
                            text = transactionPaymentMethod,
                            style = ComposeTypographies.Body2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())

                    // Fee
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimensions.smallSpacing)
                    ) {
                        SimpleText(
                            text = stringResource(R.string.bc_card_transaction_fee),
                            style = ComposeTypographies.Body1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start
                        )

                        SimpleText(
                            text = transactionFee,
                            style = ComposeTypographies.Body2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())

                    // Original transaction amount (total - fees)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimensions.smallSpacing)
                    ) {
                        SimpleText(
                            text = stringResource(
                                id = R.string.bc_card_transaction_original_amount,
                                transactionPaymentMethod
                            ),
                            style = ComposeTypographies.Body1,
                            color = ComposeColors.Body,
                            gravity = ComposeGravities.Start
                        )

                        SimpleText(
                            text = fundingTransactionAmount,
                            style = ComposeTypographies.Body2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCardTransactionDetails() {
    CardTransactionDetails(
        BlockchainCardTransaction(
            merchantName = "Coffee Beans Inc.",
            originalAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            userTransactionTime = "2020-06-21T12:00:00.000Z",
            type = BlockchainCardTransactionType.PAYMENT,
            state = BlockchainCardTransactionState.COMPLETED,
            id = "123456789",
            cardId = "123456789",
            fundingAmount = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(100.00)),
            reversedAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            counterAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            clearedFundingAmount = FiatValue.zero(FiatCurrency.fromCurrencyCode("USD")),
            fee = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("USD"), BigDecimal(0.20)),
            declineReason = null,
            networkConversionRate = null
        ),
        onCloseBottomSheet = {},
        last4digits = "1234"
    )
}

@Composable
fun TransactionControls(onCloseBottomSheet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.transaction_controls),
            shouldShowDivider = false
        )

        // Pin Settings
        SmallSectionHeader(text = stringResource(R.string.pin_settings), modifier = Modifier.fillMaxWidth())
        DefaultTableRow(
            primaryText = stringResource(R.string.manage_pin),
            secondaryText = stringResource(R.string.manage_pin_description),
            onClick = {},
        )

        // Security Settings
        SmallSectionHeader(text = stringResource(R.string.security_settings), modifier = Modifier.fillMaxWidth())
        ToggleTableRow(
            primaryText = stringResource(R.string.swipe_payments),
            secondaryText = stringResource(R.string.swipe_payments_description),
            onCheckedChange = {}
        )
        ToggleTableRow(
            primaryText = stringResource(R.string.contactless_payments),
            secondaryText = stringResource(R.string.contactless_payments_description),
            onCheckedChange = {}
        )
        DefaultTableRow(
            primaryText = stringResource(R.string.transaction_amount_limit),
            secondaryText = stringResource(R.string.transaction_amount_limit_description),
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewTransactionControls() {
    TransactionControls({})
}

@Composable
fun PersonalDetails(
    shortAddress: String?,
    onCheckBillingAddress: () -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(id = R.string.personal_details),
            shouldShowDivider = false
        )

        if (!shortAddress.isNullOrEmpty()) {
            // Address
            DefaultTableRow(
                primaryText = stringResource(R.string.billing_address),
                secondaryText = shortAddress,
                onClick = onCheckBillingAddress,
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(
                        horizontal = AppTheme.dimensions.smallSpacing,
                        vertical = AppTheme.dimensions.xHugeSpacing
                    )
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewPersonalDetails() {
    PersonalDetails("614 Lorimer Street, Sacramento CA", {}) {}
}

@Composable
fun BillingAddress(
    address: BlockchainCardAddress,
    stateList: List<Region.State>?,
    onUpdateAddress: (BlockchainCardAddress) -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    // content
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing)
    ) {
        var addressLine1 by remember {
            mutableStateOf(address.line1)
        }
        var addressLine2 by remember {
            mutableStateOf(address.line2)
        }
        var city by remember {
            mutableStateOf(address.city)
        }
        var state by remember {
            mutableStateOf(address.state)
        }
        var postalCode by remember {
            mutableStateOf(address.postCode)
        }
        var country by remember {
            mutableStateOf("US") // TODO(labreu): design doesn't support other countries yet
        }
        var selectedState by remember {
            mutableStateOf(stateList?.find { it.stateCode == address.state }?.name ?: "")
        }
        val isStateValid = remember {
            mutableStateOf(false)
        }

        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.billing_address),
            shouldShowDivider = false
        )

        // Address line 1
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            text = stringResource(R.string.address_line_1),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = addressLine1,
            onValueChange = {
                addressLine1 = it
            },
            placeholder = { Text(stringResource(R.string.address_placeholder_1)) },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        // Address line 2
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            text = stringResource(R.string.address_line_2),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = addressLine2,
            onValueChange = {
                addressLine2 = it
            },
            placeholder = { Text(stringResource(R.string.address_placeholder_2)) },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        // City
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            text = stringResource(R.string.address_city),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = city,
            onValueChange = {
                city = it
            },
            placeholder = { Text(stringResource(R.string.address_city_placeholder)) },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        // State & Zip
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // State
            Column(modifier = Modifier.weight(1f)) {
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    text = stringResource(R.string.address_state),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )

                stateList?.map { it.name }?.let { stateNameList ->
                    DropdownMenuSearch(
                        value = TextFieldValue(selectedState),
                        onValueChange = {
                            selectedState = it.text
                        },
                        initialSuggestions = stateNameList.toMutableList(),
                    )
                }
            }

            Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

            // Postal code
            Column(modifier = Modifier.weight(1f)) {
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    text = stringResource(R.string.address_zip),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = {
                        postalCode = it
                    },
                    placeholder = { Text(stringResource(R.string.address_zip_placeholder)) },
                    singleLine = true,
                    textStyle = AppTheme.typography.body1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Grey000,
                        unfocusedBorderColor = Grey000
                    )
                )
            }
        }
        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        val isFormValid = (
            addressLine1.isNotEmpty() &&
                city.isNotEmpty() &&
                state.isNotEmpty() &&
                postalCode.isNotEmpty() &&
                (stateList.isNullOrEmpty() || selectedState in stateList.map { it.name })
            )

        // Save
        PrimaryButton(
            text = stringResource(R.string.save),
            state = if (isFormValid) ButtonState.Enabled else ButtonState.Disabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_spacing),
                    top = AppTheme.dimensions.tinySpacing,
                    end = dimensionResource(id = R.dimen.standard_spacing),
                    bottom = dimensionResource(id = R.dimen.standard_spacing)
                ),
            onClick = {
                onUpdateAddress(
                    BlockchainCardAddress(
                        line1 = addressLine1,
                        line2 = addressLine2,
                        postCode = postalCode,
                        city = city,
                        state = stateList?.find { it.name == selectedState }?.stateCode.orEmpty(),
                        country = country,
                        addressType = BlockchainCardAddressType.BILLING
                    )
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewBillingAddress() {
    BillingAddress(
        address = BlockchainCardAddress(
            line1 = "1242 Johnnyappleseed Ln.",
            line2 = "1234 Road Street",
            postCode = "94592",
            city = "Walnut Creek",
            state = "CA",
            country = "USA",
            addressType = BlockchainCardAddressType.BILLING
        ),
        stateList = emptyList(),
        onUpdateAddress = {},
        onCloseBottomSheet = {}
    )
}

@Composable
fun BillingAddressUpdated(
    success: Boolean,
    error: BlockchainCardError? = null,
    onDismiss: () -> Unit,
    onCloseBottomSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(R.string.billing_address),
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.xHugeSpacing))

        if (success) {
            BillingAddressUpdatedSuccess()
        } else {
            val errorTitle = when (error) {
                is BlockchainCardError.UXBlockchainCardError -> error.uxError.title
                else -> stringResource(R.string.address_update_failed)
            }

            val errorDescription = when (error) {
                is BlockchainCardError.UXBlockchainCardError -> error.uxError.description
                else -> stringResource(R.string.address_update_failed_description)
            }

            BillingAddressUpdatedFailed(errorTitle, errorDescription)
        }

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.xHugeSpacing))

        PrimaryButton(
            text = stringResource(id = R.string.common_confirm),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_spacing),
                    top = AppTheme.dimensions.tinySpacing,
                    end = dimensionResource(id = R.dimen.standard_spacing),
                    bottom = dimensionResource(id = R.dimen.standard_spacing)
                ),
            onClick = onDismiss,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBillingAddressUpdated() {
    BillingAddressUpdated(success = true, null, onDismiss = {}, onCloseBottomSheet = {})
}

@Composable
fun BillingAddressUpdatedSuccess() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.address_updated_success),
            contentDescription = stringResource(R.string.address_updated),
            modifier = Modifier.wrapContentWidth(),
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.address_update_success),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.address_update_success_description),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBillingAddressUpdatedSuccess() {
    BillingAddressUpdatedSuccess()
}

@Composable
fun BillingAddressUpdatedFailed(errorTitle: String, errorDescription: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.ic_error),
            contentDescription = stringResource(R.string.address_updated),
            modifier = Modifier
                .wrapContentWidth()
                .size(74.dp),
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = errorTitle,
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.tinySpacing))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = errorDescription,
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBillingAddressUpdatedError() {
    BillingAddressUpdatedFailed(errorTitle = "Error", errorDescription = "Something went wrong")
}

@Composable
fun Support(
    onClickCardLost: () -> Unit,
    onClickFAQ: () -> Unit,
    onClickContactSupport: () -> Unit,
    onCloseBottomSheet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(id = R.string.support),
            shouldShowDivider = false
        )

        // Support
        SmallSectionHeader(text = stringResource(R.string.support_title), modifier = Modifier.fillMaxWidth())

        // Card Lost
        DefaultTableRow(
            primaryText = stringResource(R.string.card_lost),
            secondaryText = stringResource(R.string.card_lost_description),
            onClick = onClickCardLost,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        // FAQ
        DefaultTableRow(
            primaryText = stringResource(R.string.visit_faq),
            secondaryText = stringResource(R.string.visit_faq_description),
            onClick = onClickFAQ,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        // Contact Support
        DefaultTableRow(
            primaryText = stringResource(R.string.contact_support),
            secondaryText = stringResource(R.string.contact_support_description),
            onClick = onClickContactSupport,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewSupport() {
    Support({}, {}, {}, {})
}

@Composable
fun Documents(
    cardStatements: List<BlockchainCardStatement>?,
    legalDocuments: List<BlockchainCardLegalDocument>?,
    onViewStatement: (BlockchainCardStatement) -> Unit,
    onViewLegalDocument: (BlockchainCardLegalDocument) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {

        SimpleText(
            text = stringResource(R.string.bc_card_statements),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        SmallestVerticalSpacer()

        if (cardStatements != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Grey000),
                elevation = 0.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                LazyColumn {
                    itemsIndexed(cardStatements) { index, statement ->
                        DefaultTableRow(
                            primaryText = statement.date.toShortMonthYearDate(),
                            onClick = { onViewStatement(statement) },
                        )

                        if (index < cardStatements.lastIndex)
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            CircularProgressBar()
        }

        SmallVerticalSpacer()

        SimpleText(
            text = stringResource(R.string.legal_documents),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        SmallestVerticalSpacer()

        if (legalDocuments != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Grey000),
                elevation = 0.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                LazyColumn {
                    itemsIndexed(legalDocuments) { index, document ->
                        DefaultTableRow(
                            primaryText = document.displayName,
                            onClick = { onViewLegalDocument(document) },
                        )

                        if (index < legalDocuments.lastIndex)
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            CircularProgressBar()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDocuments() {
    Documents(
        cardStatements = listOf(
            BlockchainCardStatement(
                date = "09/2022",
                id = "1111"
            ),
            BlockchainCardStatement(
                date = "08/2022",
                id = "1111"
            ),
            BlockchainCardStatement(
                date = "07/2022",
                id = "1111"
            ),
        ),
        legalDocuments = listOf(
            BlockchainCardLegalDocument(
                displayName = "Terms and Conditions",
                name = "",
                url = "",
                version = "",
                acceptedVersion = null,
                required = false,
                seen = false,
            ),
            BlockchainCardLegalDocument(
                displayName = "Privacy Policy",
                name = "",
                url = "",
                version = "",
                acceptedVersion = null,
                required = false,
                seen = false,
            ),
            BlockchainCardLegalDocument(
                displayName = "Fees and Limits",
                name = "",
                url = "",
                version = "",
                acceptedVersion = null,
                required = false,
                seen = false,
            ),
        ),
        onViewStatement = {},
        onViewLegalDocument = {}
    )
}

@Composable
fun TerminateCard(last4digits: String, onConfirmCloseCard: () -> Unit, onCloseBottomSheet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        SheetHeader(
            onClosePress = onCloseBottomSheet,
            title = stringResource(id = R.string.terminate_card),
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.standardSpacing))

        Image(
            painter = painterResource(id = R.drawable.credit_card_failed),
            contentDescription = stringResource(R.string.card_created),
            modifier = Modifier.wrapContentWidth(),
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.smallSpacing))

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.terminate_card_number, last4digits),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.terminate_card_warning),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.Centre
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.smallSpacing))

        var closeCardConfirmationText by remember { mutableStateOf("") }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.terminate_card_confirm_description),
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
            },
            value = closeCardConfirmationText,
            onValueChange = {
                closeCardConfirmationText = it
            },
            placeholder = {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.terminate_card_confirmation_text),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Muted,
                    gravity = ComposeGravities.Start
                )
            },
            singleLine = true,
            textStyle = AppTheme.typography.body1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = ComposeColors.Title.toComposeColor(),
                focusedBorderColor = Grey000,
                unfocusedBorderColor = Grey000
            )
        )

        Spacer(modifier = Modifier.padding(AppTheme.dimensions.standardSpacing))

        // Close card
        DestructivePrimaryButton(
            text = stringResource(id = R.string.terminate_card),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_spacing),
                    top = AppTheme.dimensions.tinySpacing,
                    end = dimensionResource(id = R.dimen.standard_spacing),
                    bottom = dimensionResource(id = R.dimen.standard_spacing)
                ),
            onClick = onConfirmCloseCard,
            state = if (closeCardConfirmationText == stringResource(R.string.terminate_card_confirmation_text))
                ButtonState.Enabled
            else
                ButtonState.Disabled
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewCloseCard() {
    TerminateCard("1234", {}, {})
}

@Composable
private fun CardDetailsBottomSheetElement(
    cardStatus: BlockchainCardStatus,
    last4digits: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Grey100, RoundedCornerShape(8.dp))
            .background(UltraLight)
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.padding(horizontal = AppTheme.dimensions.tinySpacing)) {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.virtual_card),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )

                val cardStatusLabel =
                    if (cardStatus == BlockchainCardStatus.LOCKED) stringResource(R.string.bc_card_locked)
                    else stringResource(R.string.ready_to_use)

                val cardStatusColor =
                    if (cardStatus == BlockchainCardStatus.LOCKED) ComposeColors.Warning
                    else ComposeColors.Success

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = cardStatusLabel,
                    style = ComposeTypographies.Caption2,
                    color = cardStatusColor,
                    gravity = ComposeGravities.Start
                )
            }
        }

        SimpleText(
            text = "***$last4digits",
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewCardDetailsBottomSheetElement() {
    CardDetailsBottomSheetElement(BlockchainCardStatus.ACTIVE, "***3458")
}

@Composable
fun FundingAccountActionChooser(onAddFunds: () -> Unit, onChangeAsset: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = AppTheme.dimensions.standardSpacing)
            .fillMaxWidth()
    ) {

        SheetHeader(onClosePress = onClose, title = stringResource(R.string.select_one))

        Spacer(modifier = Modifier.padding(vertical = AppTheme.dimensions.smallSpacing))

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_card_add_funds_title),
                secondaryText = stringResource(R.string.bc_card_add_funds_description),
                startImageResource = ImageResource.Local(
                    id = R.drawable.add_funds_icon,
                    contentDescription = stringResource(R.string.bc_card_add_funds_title)
                ),
                onClick = onAddFunds,
            )
        }

        Spacer(modifier = Modifier.padding(vertical = AppTheme.dimensions.smallestSpacing))

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Grey000),
            elevation = 0.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.bc_card_change_asset_title),
                secondaryText = stringResource(R.string.bc_card_change_asset_description),
                startImageResource = ImageResource.Local(
                    id = R.drawable.change_asset_icon,
                    contentDescription = stringResource(R.string.bc_card_change_asset_title),
                ),
                onClick = onChangeAsset,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFundingAccountActionChooser() {
    FundingAccountActionChooser({}, {}, {})
}

@Composable
fun CardActivationSuccess(onFinish: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.xHugeSpacing),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.credit_card_success),
                contentDescription = stringResource(R.string.bc_card_activation_success_title),
                modifier = Modifier.wrapContentWidth(),
            )
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.bc_card_activation_success_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.bc_card_activation_success_subtitle),
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
            horizontalAlignment = Alignment.CenterHorizontally
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
fun PreviewCardActivationSuccess() {
    CardActivationSuccess({})
}

@Composable
fun AccountPicker(
    eligibleTradingAccountBalances: List<AccountBalance>,
    onAccountSelected: (String) -> Unit,
    onCloseBottomSheet: () -> Unit,
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        White
    } else {
        Dark800
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        SimpleText(
            text = stringResource(R.string.spend_from),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.standardSpacing)
        )

        AccountsContent(eligibleTradingAccountBalances, onAccountSelected)
    }
}

@Composable
fun AccountsContent(
    eligibleTradingAccountBalances: List<AccountBalance>,
    onAccountSelected: (String) -> Unit,
) {
    if (eligibleTradingAccountBalances.isNotEmpty()) {
        LazyColumn {
            itemsIndexed(
                items = eligibleTradingAccountBalances,
                itemContent = { index, balance ->
                    AccountItem(
                        accountBalance = balance,
                        onAccountSelected = onAccountSelected
                    )
                    if (index < eligibleTradingAccountBalances.lastIndex)
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                }
            )
        }
    } else {
        SimpleText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.no_accounts_eligible_for_linking),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )
    }
}

@Composable
fun FundingAccount(accountBalance: AccountBalance, onFundingAccountClicked: () -> Unit) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Grey000),
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        when (accountBalance.total) {
            is FiatValue -> {
                FiatAccountItem(
                    currencyName = accountBalance.totalFiat.currency.name,
                    currencyTicker = accountBalance.totalFiat.currency.networkTicker,
                    currentBalance = accountBalance.totalFiat.toStringWithSymbol(),
                    currencyLogo = accountBalance.totalFiat.currency.logo,
                    endImageResource = ImageResource.Local(
                        id = R.drawable.ic_chevron_end,
                        contentDescription = null,
                    ),
                    onClick = onFundingAccountClicked
                )
            }
            is CryptoValue -> {
                CryptoAccountItem(
                    currencyName = accountBalance.total.currency.name,
                    currencyTicker = accountBalance.total.currency.networkTicker,
                    currentBalance = accountBalance.total.toStringWithSymbol(),
                    currentBalanceInFiat = accountBalance.totalFiat.toStringWithSymbol(),
                    currencyLogo = accountBalance.total.currency.logo,
                    endImageResource = ImageResource.Local(
                        id = R.drawable.ic_chevron_end,
                        contentDescription = null,
                    ),
                    onClick = onFundingAccountClicked
                )
            }
        }
    }
}

@Composable
fun AccountItem(accountBalance: AccountBalance, onAccountSelected: (String) -> Unit) {
    when (accountBalance.total) {
        is FiatValue -> {
            FiatAccountItem(
                currencyName = accountBalance.totalFiat.currency.name,
                currencyTicker = accountBalance.totalFiat.currency.networkTicker,
                currentBalance = accountBalance.totalFiat.toStringWithSymbol(),
                currencyLogo = accountBalance.totalFiat.currency.logo,
                onClick = { onAccountSelected(accountBalance.totalFiat.currency.networkTicker) }
            )
        }
        is CryptoValue -> {
            CryptoAccountItem(
                currencyName = accountBalance.total.currency.name,
                currencyTicker = accountBalance.total.currency.networkTicker,
                currentBalance = accountBalance.total.toStringWithSymbol(),
                currentBalanceInFiat = accountBalance.totalFiat.toStringWithSymbol(),
                currencyLogo = accountBalance.total.currency.logo,
                onClick = { onAccountSelected(accountBalance.total.currency.networkTicker) }
            )
        }
    }
}

@Composable
fun CryptoAccountItem(
    currencyName: String,
    currencyTicker: String,
    currentBalance: String,
    currentBalanceInFiat: String,
    currencyLogo: String,
    endImageResource: ImageResource = ImageResource.None,
    onClick: () -> Unit,
) {
    BalanceTableRow(
        titleStart = buildAnnotatedString { append(currencyName) },
        bodyStart = buildAnnotatedString { append(stringResource(id = R.string.custodial_wallet_default_label_1)) },
        titleEnd = buildAnnotatedString { append(currentBalance) },
        bodyEnd = buildAnnotatedString { append(currentBalanceInFiat) },
        startImageResource = ImageResource.Remote(
            url = currencyLogo,
            contentDescription = null,
            shape = RoundedCornerShape(2.dp)
        ),
        endImageResource = endImageResource,
        tags = emptyList(),
        onClick = onClick
    )
}

@Composable
fun FiatAccountItem(
    currencyName: String,
    currencyTicker: String,
    currentBalance: String,
    currencyLogo: String,
    endImageResource: ImageResource = ImageResource.None,
    onClick: () -> Unit,
) {
    BalanceTableRow(
        titleStart = buildAnnotatedString { append(currencyName) },
        titleEnd = buildAnnotatedString { append(currentBalance) },
        startImageResource = ImageResource.Remote(
            url = currencyLogo,
            contentDescription = null,
            shape = RoundedCornerShape(2.dp)
        ),
        endImageResource = endImageResource,
        tags = emptyList(),
        onClick = onClick
    )
}

@Composable
fun SupportPage() {
    // TODO (labreu): For now, all support pages point to FAQ page
    Webview(
        url = "https://www.blockchain.com/faq",
        modifier = Modifier
            .padding(top = AppTheme.dimensions.smallSpacing)
    )
}

@Composable
fun CardActivationPage(cardActivationUrl: String?, onCardActivated: () -> Unit) {
    cardActivationUrl?.let { url ->
        Webview(
            url = url,
            urlRedirectHandler = { redirectUrl ->
                if (redirectUrl == "https://blockchain.com/app/card-issuing/activated") {
                    onCardActivated()
                    true // don't load the URL
                } else {
                    false // proceed loading the redirect
                }
            },
            useWideViewPort = false,
            modifier = Modifier
                .padding(top = AppTheme.dimensions.smallSpacing)
        )
    } ?: CircularProgressBar()
}
