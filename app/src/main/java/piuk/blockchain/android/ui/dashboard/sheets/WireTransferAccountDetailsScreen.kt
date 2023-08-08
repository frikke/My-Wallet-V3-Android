package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.Copy
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.theme.clickableWithIndication
import com.blockchain.domain.wiretransfer.WireTransferDetails
import com.blockchain.domain.wiretransfer.WireTransferDetailsAction
import com.blockchain.domain.wiretransfer.WireTransferDetailsFooter
import com.blockchain.domain.wiretransfer.WireTransferDetailsSection
import com.blockchain.domain.wiretransfer.WireTransferDetailsSectionEntry

@Composable
fun WireTransferAccountDetailsScreen(
    isForLink: Boolean,
    currency: String,
    details: WireTransferDetails,
    backClicked: () -> Unit,
    onEntryCopied: (WireTransferDetailsSectionEntry) -> Unit
) {
    Column {
        SheetHeader(
            title = if (isForLink) {
                stringResource(com.blockchain.stringResources.R.string.add_bank_with_currency, currency)
            } else {
                stringResource(com.blockchain.stringResources.R.string.deposit_currency, currency)
            },
            onClosePress = backClicked
        )

        Column(
            Modifier
                .background(AppColors.background)
                .padding(horizontal = AppTheme.dimensions.smallSpacing)
                .verticalScroll(rememberScrollState())
        ) {
            details.sections.forEach { section ->
                SmallVerticalSpacer()
                Section(section, onEntryCopied)
            }

            SmallVerticalSpacer()
            details.footers.forEach { footer ->
                SmallVerticalSpacer()
                Footer(footer)
            }
            SmallVerticalSpacer()
        }
    }
}

@Composable
fun ColumnScope.Section(
    section: WireTransferDetailsSection,
    onEntryCopied: (WireTransferDetailsSectionEntry) -> Unit
) {
    SimpleText(
        text = section.name,
        style = ComposeTypographies.Body2,
        color = ComposeColors.Body,
        gravity = ComposeGravities.Start
    )

    Column(
        Modifier
            .padding(top = AppTheme.dimensions.tinySpacing)
            .background(
                color = AppTheme.colors.backgroundSecondary,
                shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)
            )
    ) {
        section.entries.forEachIndexed { index, entry ->
            if (index != 0) HorizontalDivider(Modifier.fillMaxWidth())

            SectionEntry(entry) {
                onEntryCopied(entry)
            }
        }
    }
}

@Composable
fun Footer(footer: WireTransferDetailsFooter) {
    Column {
        if (footer.isImportant) {
            CardAlert(
                title = footer.title,
                subtitle = footer.message,
                alertType = AlertType.Warning,
                isDismissable = false
            )
        } else {
            Row {
                val icon = footer.icon
                if (!icon.isNullOrEmpty()) {
                    Image(ImageResource.Remote(icon))
                    SmallHorizontalSpacer()
                }

                Column(
                    Modifier
                        .weight(1f)
                ) {
                    if (footer.title.isNotEmpty()) {
                        SimpleText(
                            modifier = Modifier.fillMaxWidth(),
                            text = footer.title,
                            style = ComposeTypographies.Paragraph2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                    }
                    SimpleText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.dimensions.composeSmallestSpacing),
                        text = footer.message,
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )
                }
            }
        }

        val context = LocalContext.current
        footer.actions.forEach { action ->
            MinimalPrimarySmallButton(
                text = action.title,
                onClick = {
                    if (!action.url.isNullOrEmpty()) {
                        Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            .also { context.startActivity(it) }
                    }
                }
            )
        }
    }
}

@Composable
fun SectionEntry(
    entry: WireTransferDetailsSectionEntry,
    onEntryCopied: () -> Unit
) {
    var isHelpOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (entry.title.isNotEmpty()) {
                Row(Modifier.fillMaxWidth()) {
                    SimpleText(
                        text = entry.title,
                        style = ComposeTypographies.Caption1,
                        color = if (entry.isImportant) ComposeColors.Warning else ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    if (!entry.help.isNullOrEmpty()) {
                        Image(
                            modifier = Modifier
                                .padding(start = AppTheme.dimensions.tinySpacing)
                                .clickableWithIndication { isHelpOpen = !isHelpOpen },
                            imageResource = Icons.Filled.Question.withSize(AppTheme.dimensions.smallSpacing)
                        )
                    }
                }

                SmallestVerticalSpacer()
            }

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = entry.message,
                style = ComposeTypographies.Paragraph2,
                color = if (entry.isImportant) ComposeColors.Warning else ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            if (isHelpOpen) {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = entry.help!!,
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
            }
        }

        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        Image(
            modifier = Modifier.clickableWithIndication {
                clipboardManager.setText(AnnotatedString(entry.message))
                onEntryCopied()
            },
            imageResource = Icons.Copy.withTint(AppTheme.colors.muted)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun PreviewFooter() {
    Footer(
        FOOTER
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun PreviewFooterImportant() {
    Footer(
        FOOTER_IMPORTANT
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun PreviewSectionEntry() {
    SectionEntry(
        SECTION_ENTRY,
        {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun PreviewSectionEntryImportant() {
    SectionEntry(
        SECTION_ENTRY_IMPORTANT,
        {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun PreviewSection() {
    Column {
        Section(
            SECTION,
            {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun PreviewWireTransferAccountDetailsScreen() {
    WireTransferAccountDetailsScreen(
        isForLink = false,
        "USD",
        EXAMPLE_USD,
        {},
        {}
    )
}

private val FOOTER = WireTransferDetailsFooter(
    title = "Business Name",
    message = "Blockchain.com, Inc.",
    isImportant = true,
    icon = "",
    actions = listOf(
        WireTransferDetailsAction(
            title = "Terms & Conditions",
            url = "https://www.google.com"
        )
    )
)
private val FOOTER_IMPORTANT = WireTransferDetailsFooter(
    title = "",
    message = "By depositing funds to this account, you agree to Terms & Conditions of Modulr, our banking partner.",
    isImportant = false,
    icon = "",
    actions = listOf(
        WireTransferDetailsAction(
            title = "Terms & Conditions",
            url = "https://www.google.com"
        )
    )
)
private val SECTION_ENTRY = WireTransferDetailsSectionEntry(
    title = "Business Name",
    message = "Blockchain.com, Inc.",
    isImportant = false,
    help = null
)
private val SECTION_ENTRY_IMPORTANT = WireTransferDetailsSectionEntry(
    title = "Reference ID (Required)",
    message = "BCDOEVC3",
    isImportant = true,
    help = "Some helper text"
)
private val SECTION = WireTransferDetailsSection(
    name = "Beneficiary",
    entries = listOf(SECTION_ENTRY, SECTION_ENTRY_IMPORTANT)
)
private val EXAMPLE_USD = WireTransferDetails(
    sections = listOf(SECTION, SECTION),
    footers = listOf(FOOTER_IMPORTANT, FOOTER)
)
