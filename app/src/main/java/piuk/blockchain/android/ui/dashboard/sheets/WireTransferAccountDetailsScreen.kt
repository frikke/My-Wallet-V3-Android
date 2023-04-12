package piuk.blockchain.android.ui.dashboard.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.Copy
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.theme.clickableWithIndication
import com.blockchain.domain.wiretransfer.WireTransferDetails
import com.blockchain.domain.wiretransfer.WireTransferDetailsFooter
import com.blockchain.domain.wiretransfer.WireTransferDetailsSection
import com.blockchain.domain.wiretransfer.WireTransferDetailsSectionEntry
import piuk.blockchain.android.R

@Composable
fun WireTransferAccountDetailsScreen(
    isForLink: Boolean,
    currency: String,
    details: WireTransferDetails,
    backClicked: () -> Unit,
    onEntryCopied: (WireTransferDetailsSectionEntry) -> Unit,
) {
    Column {
        SheetHeader(
            title = if (isForLink) {
                stringResource(R.string.add_bank_with_currency, currency)
            } else {
                stringResource(R.string.deposit_currency, currency)
            },
            onClosePress = backClicked,
            shouldShowDivider = true
        )

        Column(
            Modifier
                .background(AppTheme.colors.light)
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
fun Section(
    section: WireTransferDetailsSection,
    onEntryCopied: (WireTransferDetailsSectionEntry) -> Unit,
) {
    SimpleText(
        text = section.name,
        style = ComposeTypographies.Body2,
        color = ComposeColors.Body,
        gravity = ComposeGravities.Start,
    )

    Column(
        Modifier
            .padding(top = AppTheme.dimensions.tinySpacing)
            .background(
                color = AppTheme.colors.background,
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
    if (footer.isImportant) {
        CardAlert(
            title = footer.title,
            subtitle = footer.message,
            alertType = AlertType.Warning,
            isDismissable = false,
        )
    } else {
        Row {
            val icon = footer.icon
            if (!icon.isNullOrEmpty()) {
                Image(imageResource = ImageResource.Remote(icon))
            }

            Column(
                Modifier
                    .padding(start = AppTheme.dimensions.smallSpacing)
                    .weight(1f)
            ) {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = footer.title,
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start,
                )
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.dimensions.composeSmallestSpacing),
                    text = footer.message,
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
                )
            }
        }
    }
}

@Composable
fun SectionEntry(
    entry: WireTransferDetailsSectionEntry,
    onEntryCopied: () -> Unit,
) {
    var isHelpOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (entry.title.isNotEmpty()) {
                Row(Modifier.fillMaxWidth()) {
                    SimpleText(
                        text = entry.title,
                        style = ComposeTypographies.Caption1,
                        color = if (entry.isImportant) ComposeColors.Warning else ComposeColors.Body,
                        gravity = ComposeGravities.Start,
                    )

                    if (!entry.help.isNullOrEmpty()) {
                        Image(
                            modifier = Modifier
                                .padding(start = AppTheme.dimensions.tinySpacing)
                                .clickableWithIndication { isHelpOpen = !isHelpOpen },
                            imageResource = Icons.Filled.Question.withSize(AppTheme.dimensions.smallSpacing),
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
                gravity = ComposeGravities.Start,
            )

            if (isHelpOpen) {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = entry.help!!,
                    style = ComposeTypographies.Caption1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start,
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

@Preview
@Composable
fun PreviewFooter() {
    Footer(
        FOOTER
    )
}

@Preview
@Composable
fun PreviewSectionEntry() {
    SectionEntry(
        SECTION_ENTRY,
        {},
    )
}

@Preview
@Composable
fun PreviewSectionEntryImportant() {
    SectionEntry(
        SECTION_ENTRY_IMPORTANT,
        {},
    )
}

@Preview
@Composable
fun PreviewSection() {
    Section(
        SECTION,
        {},
    )
}

@Preview
@Composable
fun PreviewWireTransferAccountDetailsScreen() {
    WireTransferAccountDetailsScreen(
        isForLink = false,
        "USD",
        EXAMPLE_USD,
        {},
        {},
    )
}

private val FOOTER = WireTransferDetailsFooter(
    title = "Business Name",
    message = "Blockchain.com, Inc.",
    isImportant = false,
    icon = "",
)
private val SECTION_ENTRY = WireTransferDetailsSectionEntry(
    title = "Business Name",
    message = "Blockchain.com, Inc.",
    isImportant = false,
    help = null,
)
private val SECTION_ENTRY_IMPORTANT = WireTransferDetailsSectionEntry(
    title = "Reference ID (Required)",
    message = "BCDOEVC3",
    isImportant = true,
    help = "Some helper text",
)
private val SECTION = WireTransferDetailsSection(
    name = "Beneficiary",
    entries = listOf(SECTION_ENTRY, SECTION_ENTRY_IMPORTANT)
)
private val EXAMPLE_USD = WireTransferDetails(
    sections = listOf(SECTION, SECTION),
    footers = listOf(FOOTER, FOOTER)
)
