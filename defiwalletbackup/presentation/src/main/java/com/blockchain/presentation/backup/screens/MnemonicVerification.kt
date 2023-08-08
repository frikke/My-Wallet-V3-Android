package com.blockchain.presentation.backup.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.backup.UserMnemonicVerificationStatus
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import java.util.Locale

@Composable
fun MnemonicVerification(
    mnemonic: List<SelectableMnemonicWord>,
    mnemonicVerificationStatus: UserMnemonicVerificationStatus,
    wordSelected: (selectableWord: SelectableMnemonicWord) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = AppTheme.dimensions.borderSmall,
                color = when (mnemonicVerificationStatus) {
                    UserMnemonicVerificationStatus.IDLE -> Color.Transparent
                    UserMnemonicVerificationStatus.INCORRECT -> AppColors.error
                },
                shape = AppTheme.shapes.large
            )
            .background(color = AppTheme.colors.backgroundSecondary, shape = AppTheme.shapes.large)
            .padding(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing))
            .heightIn(min = 182.dp),
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        mainAxisSpacing = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing),
        crossAxisSpacing = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)
    ) {
        mnemonic.forEachIndexed { index, selectableWord ->
            MnemonicVerificationWord(index = index.inc(), selectableWord = selectableWord) {
                wordSelected(selectableWord)
            }
        }
    }
}

@Composable
fun MnemonicSelection(
    mnemonic: List<SelectableMnemonicWord>,
    wordSelected: (selectableWord: SelectableMnemonicWord) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth(),
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        mainAxisSpacing = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing),
        crossAxisSpacing = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)
    ) {
        mnemonic.forEach { selectableWord ->
            MnemonicVerificationWord(selectableWord = selectableWord) {
                if (selectableWord.selected.not()) wordSelected(selectableWord)
            }
        }
    }
}

@Composable
fun MnemonicVerificationWord(
    index: Int? = null,
    selectableWord: SelectableMnemonicWord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            // if a word is selected it should still occupy the space but should be hidden
            .alpha(if (selectableWord.selected) 0F else 1F),
        border = BorderStroke(
            width = AppTheme.dimensions.borderSmall,
            color = AppTheme.colors.medium
        ),
        shape = AppTheme.shapes.small,
        backgroundColor = if (index != null) AppColors.background else AppColors.backgroundSecondary,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                )
                .padding(
                    vertical = AppTheme.dimensions.minusculeSpacing,
                    horizontal = AppTheme.dimensions.verySmallSpacing
                ),
            horizontalArrangement = Arrangement.Center
        ) {
            index?.let {
                SimpleText(
                    text = index.toString(),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Muted,
                    gravity = ComposeGravities.End
                )

                Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
            }

            SimpleText(
                text = selectableWord.word,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    }
}

data class SelectableMnemonicWord(
    val id: Int,
    val word: String,
    val selected: Boolean
)

// ///////////////
// PREVIEWS
// ///////////////

private val mnemonic = Locale.getISOCountries().toList().map {
    Locale("", it).isO3Country
}.shuffled().subList(0, 12).map { SelectableMnemonicWord(1, it, false) }

@Preview(name = "Mnemonic Idle", showBackground = true)
@Composable
private fun PreviewMnemonicVerificationIdle() {
    MnemonicVerification(mnemonic = mnemonic, mnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE) {}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMnemonicVerificationIdleDark() {
    PreviewMnemonicVerificationIdle()
}

@Preview(name = "Mnemonic Incorrect", showBackground = true)
@Composable
private fun PreviewMnemonicVerificationIncorrect() {
    MnemonicVerification(mnemonic = mnemonic, mnemonicVerificationStatus = UserMnemonicVerificationStatus.INCORRECT) {}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMnemonicVerificationIncorrectDark() {
    PreviewMnemonicVerificationIncorrect()
}

@Preview(name = "Mnemonic Selection", showBackground = true)
@Composable
private fun PreviewMnemonicSelection() {
    MnemonicSelection(mnemonic = mnemonic) {}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMnemonicSelectionDark() {
    PreviewMnemonicSelection()
}

@Preview
@Composable
private fun PreviewMnemonicVerificationWord() {
    MnemonicVerificationWord(1, SelectableMnemonicWord(1, "blockchain", false)) {}
}
