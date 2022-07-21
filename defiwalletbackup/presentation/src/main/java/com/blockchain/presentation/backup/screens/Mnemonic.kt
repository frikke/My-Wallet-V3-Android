package com.blockchain.presentation.backup.screens

import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.presentation.R
import java.util.Locale

private const val COLUMN_COUNT = 3

@Composable
fun Mnemonic(mnemonic: List<String>) {
    Mnemonic(mnemonic = mnemonic, hidable = false)
}

@Composable
fun HidableMnemonic(mnemonic: List<String>) {
    Mnemonic(mnemonic = mnemonic, hidable = true)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun Mnemonic(mnemonic: List<String>, hidable: Boolean) {
    var hidden by remember { mutableStateOf(hidable) }

    ConstraintLayout {
        val (grid, mask, icon) = createRefs()
        LazyVerticalGrid(
            modifier = Modifier
                .constrainAs(grid) {
                    start.linkTo(parent.start)
                }
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Grey100,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiSmall))
                )
                .background(color = Color.White, shape = RoundedCornerShape(dimensionResource(R.dimen.tiny_margin)))
                .run {
                    if (hidable) {
                        pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> hidden = false
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> hidden = true
                            }
                            true
                        }
                    } else this
                },
            contentPadding = PaddingValues(
                horizontal = dimensionResource(R.dimen.small_margin),
                vertical = dimensionResource(R.dimen.small_margin)
            ),
            cells = GridCells.Fixed(count = COLUMN_COUNT),
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

        if (hidable && hidden) {
            Box(
                modifier = Modifier
                    .constrainAs(mask) {
                        start.linkTo(grid.start)
                        top.linkTo(grid.top)
                        end.linkTo(grid.end)
                        bottom.linkTo(grid.bottom)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .background(Grey000)
            )

            Image(
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(grid.start)
                        top.linkTo(grid.top)
                        end.linkTo(grid.end)
                        bottom.linkTo(grid.bottom)
                    },
                imageResource = ImageResource.Local(R.drawable.ic_visibility_off)
            )
        }
    }
}

@Composable
fun MnemonicWord(index: Int, word: String) {
    Row {
        Text(
            modifier = Modifier.width(dimensionResource(R.dimen.standard_margin)),
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

@Preview(name = "Hidable Mnemonic", showBackground = true)
@Composable
fun PreviewHidableMnemonic() {
    HidableMnemonic(mnemonic = mnemonic)
}

@Preview
@Composable
fun PreviewMnemonicWord() {
    MnemonicWord(1, "blockchain")
}
