package com.blockchain.presentation.backup.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
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

@OptIn(ExperimentalComposeUiApi::class)
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
                .background(color = Color.White, shape = AppTheme.shapes.large)
                .border(
                    width = AppTheme.dimensions.borderSmall,
                    color = Grey100,
                    shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
                )
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
            contentPadding = PaddingValues(AppTheme.dimensions.smallSpacing),
            columns = GridCells.Fixed(count = COLUMN_COUNT),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.standardSpacing),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallestSpacing)
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
                    .border(
                        width = AppTheme.dimensions.borderSmall,
                        color = Grey100,
                        shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
                    )
                    .background(color = Grey000, shape = AppTheme.shapes.large)
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
        SimpleText(
            modifier = Modifier.width(dimensionResource(R.dimen.standard_spacing)),
            text = index.toString(),
            style = ComposeTypographies.ParagraphMono,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.End
        )

        TinyHorizontalSpacer()

        SimpleText(
            text = word,
            style = ComposeTypographies.ParagraphMono,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
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
