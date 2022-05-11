package com.blockchain.componentlib.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource

@Composable
fun RatingBar(
    count: Int,
    @DrawableRes imageFilled: Int,
    @DrawableRes imageOutline: Int,
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {

//    val int = rememb

    Row {
        val range: IntRange = 1..count

        (1..count).forEach { index ->
            Image(
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
    RatingBar(5, R.drawable.ic_star_filled, R.drawable.ic_star, 3)
}