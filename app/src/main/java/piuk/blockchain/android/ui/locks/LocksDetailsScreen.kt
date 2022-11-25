package piuk.blockchain.android.ui.locks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import com.blockchain.domain.paymentmethods.model.FundsLock
import com.blockchain.domain.paymentmethods.model.FundsLocks
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.math.BigInteger
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import piuk.blockchain.android.R

@Composable
fun LocksDetailsScreen(
    locks: FundsLocks,
    backClicked: () -> Unit,
    learnMoreClicked: () -> Unit,
    contactSupportClicked: () -> Unit,
    okClicked: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .background(White)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .weight(1f)
        ) {
            NavigationBar(
                title = stringResource(R.string.funds_locked_details_toolbar),
                onBackButtonClick = backClicked,
            )

            SimpleText(
                modifier = Modifier.padding(
                    top = AppTheme.dimensions.standardSpacing,
                    start = AppTheme.dimensions.smallSpacing,
                ),
                text = stringResource(R.string.funds_locked_details_title),
                style = ComposeTypographies.Caption2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
            )
            SimpleText(
                modifier = Modifier.padding(
                    top = AppTheme.dimensions.composeSmallestSpacing,
                    start = AppTheme.dimensions.smallSpacing,
                ),
                text = locks.onHoldTotalAmount.toStringWithSymbol(),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
            )

            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.standardSpacing)
            )

            LazyColumn(
                Modifier.weight(1f, fill = false)
            ) {
                items(locks.locks) { item ->
                    Item(item)
                    HorizontalDivider(Modifier.fillMaxWidth())
                }
            }

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.smallSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing,
                    ),
                text = stringResource(R.string.funds_locked_summary_text),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
            )

            SmallMinimalButton(
                modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
                text = stringResource(R.string.common_learn_more),
                onClick = learnMoreClicked,
            )
        }

        Column(Modifier.fillMaxWidth()) {
            MinimalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing),
                text = stringResource(R.string.contact_support),
                onClick = contactSupportClicked,
            )

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTheme.dimensions.standardSpacing,
                        vertical = AppTheme.dimensions.smallSpacing,
                    ),
                text = stringResource(R.string.common_ok),
                onClick = okClicked,
            )
        }
    }
}

@Composable
private fun Item(
    item: FundsLock
) {
    TableRow(
        contentStart = {
            val asset = item.buyAmount?.currency ?: item.amount.currency
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(AppTheme.dimensions.standardSpacing),
                imageResource = ImageResource.Remote(
                    url = asset.logo,
                    shape = if (asset.type == CurrencyType.CRYPTO) CircleShape else RectangleShape,
                ),
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppTheme.dimensions.smallSpacing)
            ) {
                val actionText = item.buyAmount?.currency?.name?.let {
                    stringResource(R.string.funds_locked_details_item_action_buy_title, it)
                } ?: stringResource(R.string.funds_locked_details_item_action_deposit_title, item.amount.currency.name)
                SimpleText(
                    text = actionText,
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                )

                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
                val dateString = dateFormatter.format(item.date)
                SimpleText(
                    modifier = Modifier.padding(top = AppTheme.dimensions.smallestSpacing),
                    text = stringResource(R.string.funds_locked_details_item_action_available, dateString),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                )
            }
        },
        contentEnd = {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                SimpleText(
                    text = item.amount.toStringWithSymbol(),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.End,
                )

                val buyAmount = item.buyAmount
                if (buyAmount != null) {
                    SimpleText(
                        modifier = Modifier.padding(top = AppTheme.dimensions.smallestSpacing),
                        text = buyAmount.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.End,
                    )
                }
            }
        }
    )
//    ConstraintLayout(
//        Modifier
//            .fillMaxWidth()
//            .background(White)
//            .padding(all = AppTheme.dimensions.smallSpacing)
//    ) {
//        val (
//            iconRef,
//            actionRef,
//            fiatAmountRef,
//            dateRef,
//            cryptoAmountRef,
//        ) = createRefs()
//
//        val asset = item.buyAmount?.currency ?: item.amount.currency
//        Image(
//            modifier = Modifier
//                .constrainAs(iconRef) {
//                    top.linkTo(parent.top)
//                    bottom.linkTo(parent.bottom)
//                    start.linkTo(parent.start)
//                }
//                .size(AppTheme.dimensions.standardSpacing),
//            imageResource = ImageResource.Remote(
//                url = asset.logo,
//                shape = if (asset.type == CurrencyType.CRYPTO) CircleShape else null,
//            ),
//        )
//
//        val actionText = item.buyAmount?.currency?.name?.let {
//            stringResource(R.string.funds_locked_details_item_action_buy_title, it)
//        } ?: stringResource(R.string.funds_locked_details_item_action_deposit_title, item.amount.currency.name)
//        SimpleText(
//            modifier = Modifier
//                .constrainAs(actionRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(iconRef.end)
//                    bottom.linkTo(dateRef.top)
//                }
//                .padding(start = AppTheme.dimensions.smallSpacing),
//            text = actionText,
//            style = ComposeTypographies.Paragraph2,
//            color = ComposeColors.Title,
//            gravity = ComposeGravities.Start,
//        )
//
//        SimpleText(
//            modifier = Modifier
//                .constrainAs(fiatAmountRef) {
//                    top.linkTo(parent.top)
//                    end.linkTo(parent.end)
//                    bottom.linkTo(cryptoAmountRef.top)
//                },
//            text = item.amount.toStringWithSymbol(),
//            style = ComposeTypographies.Paragraph2,
//            color = ComposeColors.Title,
//            gravity = ComposeGravities.End,
//        )
//
//        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
//        val dateString = dateFormatter.format(item.date)
//        SimpleText(
//            modifier = Modifier
//                .constrainAs(dateRef) {
//                    top.linkTo(actionRef.bottom)
//                    start.linkTo(iconRef.end)
//                    bottom.linkTo(parent.bottom)
//                }
//                .padding(
//                    top = AppTheme.dimensions.smallestSpacing,
//                    start = AppTheme.dimensions.smallSpacing,
//                ),
//            text = stringResource(R.string.funds_locked_details_item_action_available, dateString),
//            style = ComposeTypographies.Paragraph1,
//            color = ComposeColors.Body,
//            gravity = ComposeGravities.Start,
//        )
//
//        SimpleText(
//            modifier = Modifier
//                .constrainAs(cryptoAmountRef) {
//                    top.linkTo(fiatAmountRef.bottom)
//                    end.linkTo(parent.end)
//                    bottom.linkTo(parent.bottom)
//                    visibility = if (item.buyAmount != null) Visibility.Visible else Visibility.Gone
//                }
//                .padding(top = AppTheme.dimensions.smallestSpacing),
//            text = item.buyAmount?.toStringWithSymbol().orEmpty(),
//            style = ComposeTypographies.Paragraph1,
//            color = ComposeColors.Body,
//            gravity = ComposeGravities.End,
//        )
//
//    }
}

@Composable
@Preview(heightDp = 800)
private fun PreviewScreen1() {
    val locks = FundsLocks(
        onHoldTotalAmount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(1000)),
        locks = listOf(
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(1000)),
                date = ZonedDateTime.now(),
                buyAmount = null,
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("EUR"), BigInteger.valueOf(500)),
                date = ZonedDateTime.now(),
                buyAmount = null,
            ),
        )
    )
    LocksDetailsScreen(
        locks = locks,
        backClicked = {},
        learnMoreClicked = {},
        contactSupportClicked = {},
        okClicked = {},
    )
}

@Composable
@Preview(heightDp = 800)
private fun PreviewScreen2() {
    val locks = FundsLocks(
        onHoldTotalAmount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(1000)),
        locks = listOf(
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(1000)),
                date = ZonedDateTime.now(),
                buyAmount = null,
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("EUR"), BigInteger.valueOf(500)),
                date = ZonedDateTime.now(),
                buyAmount = null,
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("EUR"), BigInteger.valueOf(500)),
                date = ZonedDateTime.now(),
                buyAmount = null,
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("EUR"), BigInteger.valueOf(500)),
                date = ZonedDateTime.now(),
                buyAmount = null,
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("GBP"), BigInteger.valueOf(5500)),
                date = ZonedDateTime.now(),
                buyAmount = Money.fromMinor(CryptoCurrency.BTC, BigInteger.valueOf(1000)),
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("EUR"), BigInteger.valueOf(1500)),
                date = ZonedDateTime.now(),
                buyAmount = Money.fromMinor(CryptoCurrency.ETHER, BigInteger.valueOf(2000)),
            ),
            FundsLock(
                amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(3500)),
                date = ZonedDateTime.now(),
                buyAmount = Money.fromMinor(CryptoCurrency.XLM, BigInteger.valueOf(2000)),
            ),
        )
    )
    LocksDetailsScreen(
        locks = locks,
        backClicked = {},
        learnMoreClicked = {},
        contactSupportClicked = {},
        okClicked = {},
    )
}

@Composable
@Preview
private fun PreviewItemDeposit() {
    Item(
        FundsLock(
            amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(3500)),
            date = ZonedDateTime.now(),
            buyAmount = null,
        )
    )
}

@Composable
@Preview
private fun PreviewItemBuy() {
    Item(
        FundsLock(
            amount = Money.fromMinor(FiatCurrency.fromCurrencyCode("USD"), BigInteger.valueOf(3500)),
            date = ZonedDateTime.now(),
            buyAmount = Money.fromMinor(CryptoCurrency.XLM, BigInteger.valueOf(2000)),
        )
    )
}
