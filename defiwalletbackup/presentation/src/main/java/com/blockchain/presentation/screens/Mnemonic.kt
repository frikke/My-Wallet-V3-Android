package com.blockchain.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.presentation.R
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Mnemonic(mnemonic: List<String>) {
    LazyVerticalGrid(
        modifier = Modifier.border(width = 1.dp, color = Grey100),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.small_margin),
            vertical = dimensionResource(R.dimen.small_margin)
        ),
        cells = GridCells.Fixed(count = 3),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_margin)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallest_margin))
    ) {
        itemsIndexed(
            items = mnemonic,
            itemContent = { index, word ->
                MnemonicWord(index = index.inc(), word = word)
            }
        )
    }
}

@Composable
fun MnemonicWord(index: Int, word: String) {
    Row {
        Text(
            modifier = Modifier.width(20.dp),
            text = index.toString(),
            style = AppTheme.typography.paragraphMono,
            color = Grey400,
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = word,
            style = AppTheme.typography.paragraphMono,
            color = Grey900,
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

private val mnemonic = Locale.getISOCountries().toList().map {
    Locale("", it).isO3Country
}.shuffled().subList(0, 12)

@Preview(name = "Mnemonic", showBackground = true)
@Composable
fun PreviewMnemonic() {
    Mnemonic(mnemonic = mnemonic)
}

@Preview
@Composable
fun PreviewMnemonicWord() {
    MnemonicWord(1, "blockchain")
}
