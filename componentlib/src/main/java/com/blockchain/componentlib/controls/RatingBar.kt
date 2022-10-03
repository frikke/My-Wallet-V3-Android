package com.blockchain.componentlib.controls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource

@Composable
fun RatingBar(
    count: Int = 5,
    @DrawableRes imageFilled: Int,
    @DrawableRes imageOutline: Int,
    initialRating: Int = 0,
    onRatingChanged: (rating: Int) -> Unit
) {
    var rating by remember { mutableStateOf(initialRating) }

    Row {
        val range: IntRange = 1..count

        (1..count).forEach { index ->
            Image(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        rating = index
                        onRatingChanged(rating)
                    },
                imageResource = ImageResource.Local(
                    if (index <= rating) imageFilled
                    else imageOutline
                )
            )

            if (index < range.last) {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.tiny_margin)))
            }
        }
    }
}

@Preview
@Composable
fun PreviewRatingBar() {
    RatingBar(5, R.drawable.ic_star_filled, R.drawable.ic_star, 3) {}
}
