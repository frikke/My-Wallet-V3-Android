package com.blockchain.blockchaincard.ui.composables.managecard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.blockchaincard.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.DestructiveMinimalButton
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.ToggleTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000

@Composable
fun ManageCard(cardId: String?, onManageCardDetails: () -> Unit, onDeleteCard: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.paddingMedium
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.card_with_dummy_details),
                contentDescription = stringResource(id = R.string.blockchain_card),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.paddingLarge
                    ),
                contentScale = ContentScale.FillWidth
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTheme.dimensions.paddingMedium,
                        end = AppTheme.dimensions.paddingMedium,
                        bottom = AppTheme.dimensions.paddingMedium
                    )
            ) {
                MinimalButton(
                    text = stringResource(R.string.manage),
                    onClick = onManageCardDetails,
                    icon = ImageResource.Local(R.drawable.ic_nav_settings),
                    modifier = Modifier.weight(1f)
                )
                MinimalButton(
                    text = stringResource(R.string.top_up),
                    onClick = { /*TODO*/ },
                    icon = ImageResource.Local(R.drawable.ic_bottom_nav_plus),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        DefaultTableRow(
            primaryText = stringResource(R.string.choose_payment_method),
            secondaryText = stringResource(R.string.fund_your_card_purchases),
            onClick = {},
            startImageResource = ImageResource.Local(
                id = R.drawable.ic_question,
                contentDescription = null
            )
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        DefaultTableRow(
            primaryText = stringResource(R.string.cashback_rewards),
            secondaryText = stringResource(R.string.earn_crypto_on_purchases),
            onClick = {},
            startImageResource = ImageResource.Local(
                id = R.drawable.ic_question,
                contentDescription = null
            )
        )

        SmallSectionHeader(text = stringResource(R.string.card_benefits), modifier = Modifier.fillMaxWidth())

        SimpleText(
            text = stringResource(R.string.recent_purchases_here),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Dark,
            gravity = ComposeGravities.Centre
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewManageCard() {
    ManageCard("", {}, {})
}

@Composable
private fun ManageCardDetails(onDeleteCard: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        CardDetailsBottomSheetElement(
            Modifier.padding(
                AppTheme.dimensions.paddingLarge,
                AppTheme.dimensions.paddingMedium
            )
        )

        // Todo GPay save to phone button should be its own composable
        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTheme.dimensions.paddingLarge,
                    end = AppTheme.dimensions.paddingLarge,
                    bottom = AppTheme.dimensions.paddingLarge
                ),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_gpay_save_card),
                contentDescription = stringResource(R.string.gpay_save_to_phone),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }

        DefaultTableRow(
            primaryText = stringResource(R.string.copy_card_number),
            secondaryText = stringResource(R.string.copy_to_clipboard),
            onClick = {},
            endImageResource = ImageResource.Local(
                id = R.drawable.ic_copy,
                contentDescription = null
            )
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        ToggleTableRow(
            onCheckedChange = {},
            primaryText = stringResource(R.string.lock_card),
            secondaryText = stringResource(R.string.temporarily_lock_card)
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        DefaultTableRow(
            primaryText = stringResource(R.string.support),
            secondaryText = stringResource(R.string.get_help_with_card_issues),
            onClick = {}
        )

        DestructiveMinimalButton(
            text = stringResource(R.string.delete_card),
            onClick = onDeleteCard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.paddingLarge)
        ) // Todo add trashcan icon
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewManageCardDetails() {
    ManageCardDetails({})
}

@Composable
private fun CardDetailsBottomSheetElement(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Grey000)
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(id = R.drawable.card_icon),
                contentDescription = stringResource(R.string.blockchain_card),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.FillHeight
            )

            Column(modifier = Modifier.padding(horizontal = AppTheme.dimensions.paddingSmall)) {
                SimpleText(
                    text = stringResource(R.string.virtual_card),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )

                SimpleText(
                    text = stringResource(R.string.ready_to_use),
                    style = ComposeTypographies.Caption2,
                    color = ComposeColors.Dark,
                    gravity = ComposeGravities.Start
                )
            }
        }

        SimpleText(
            text = "***3458", // TODO(labreu): remove place holder
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.End,
            modifier = Modifier.padding(AppTheme.dimensions.paddingMedium)
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewCardDetailsBottomSheetElement() {
    CardDetailsBottomSheetElement()
}
