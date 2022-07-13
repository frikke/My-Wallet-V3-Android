package com.blockchain.presentation.backup.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.Red600
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.UserMnemonicVerificationStatus
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import java.util.Locale

@Composable
fun MnemonicVerification(
    mnemonic: List<SelectableMnemonicWord>,
    mnemonicVerificationStatus: UserMnemonicVerificationStatus,
    wordSelected: (selectableWord: SelectableMnemonicWord) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (mnemonicVerificationStatus) {
                    UserMnemonicVerificationStatus.IDLE -> Grey100
                    UserMnemonicVerificationStatus.INCORRECT -> Red600
                }.exhaustive,
                shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiSmall))
            )
            .background(color = Grey000, shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiSmall)))
            .padding(dimensionResource(R.dimen.small_margin))
            .heightIn(min = 182.dp),
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        mainAxisSpacing = dimensionResource(R.dimen.tiny_margin),
        crossAxisSpacing = dimensionResource(R.dimen.tiny_margin)
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
    wordSelected: (selectableWord: SelectableMnemonicWord) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth(),
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        mainAxisSpacing = dimensionResource(R.dimen.tiny_margin),
        crossAxisSpacing = dimensionResource(R.dimen.tiny_margin)
    ) {
        mnemonic.forEach { selectableWord ->
            MnemonicVerificationWord(selectableWord = selectableWord) {
                if (selectableWord.selected.not()) wordSelected(selectableWord)
            }
        }
    }
}

@Composable
fun MnemonicVerificationWord(index: Int? = null, selectableWord: SelectableMnemonicWord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            // if a word is selected it should still occupy the space but should be hidden
            .alpha(if (selectableWord.selected) 0F else 1F),
        border = BorderStroke(
            width = 1.dp,
            color = Grey100,
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiSmallest)),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                )
                .padding(
                    vertical = dimensionResource(R.dimen.minuscule_margin),
                    horizontal = dimensionResource(R.dimen.very_small_margin)
                ),
            horizontalArrangement = Arrangement.Center,
        ) {
            index?.let {
                Text(
                    text = index.toString(),
                    style = AppTheme.typography.paragraphMono,
                    color = Grey400,
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))
            }

            Text(
                text = selectableWord.word,
                style = AppTheme.typography.paragraphMono,
                color = Grey900,
            )
        }
    }
}

data class SelectableMnemonicWord(
    val id: Int,
    val word: String,
    val selected: Boolean,
)

// ///////////////
// PREVIEWS
// ///////////////

private val mnemonic = Locale.getISOCountries().toList().map {
    Locale("", it).isO3Country
}.shuffled().subList(0, 12).map { SelectableMnemonicWord(1, it, false) }

@Preview(name = "Mnemonic Idle", showBackground = true)
@Composable
fun PreviewMnemonicVerificationIdle() {
    MnemonicVerification(mnemonic = mnemonic, mnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE) {}
}

@Preview(name = "Mnemonic Incorrect", showBackground = true)
@Composable
fun PreviewMnemonicVerificationIncorrect() {
    MnemonicVerification(mnemonic = mnemonic, mnemonicVerificationStatus = UserMnemonicVerificationStatus.INCORRECT) {}
}

@Preview(name = "Mnemonic Selection", showBackground = true)
@Composable
fun PreviewMnemonicSelection() {
    MnemonicSelection(mnemonic = mnemonic) {}
}

@Preview
@Composable
fun PreviewMnemonicVerificationWord() {
    MnemonicVerificationWord(1, SelectableMnemonicWord(1, "blockchain", false)) {}
}
