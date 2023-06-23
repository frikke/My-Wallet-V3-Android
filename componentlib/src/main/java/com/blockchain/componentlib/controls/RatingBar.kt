package com.blockchain.componentlib.controls

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
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Star
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun RatingBar(
    count: Int = 5,
    imageFilled: ImageResource.Local,
    imageOutline: ImageResource.Local,
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
                imageResource = if (index <= rating) {
                    imageFilled
                } else {
                    imageOutline
                }.withSize(AppTheme.dimensions.hugeSpacing)
            )

            if (index < range.last) {
                Spacer(modifier = Modifier.width(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
            }
        }
    }
}

@Preview
@Composable
fun PreviewRatingBar() {
    RatingBar(
        count = 5,
        imageFilled = Icons.Filled.Star.withTint(AppColors.warningMuted),
        imageOutline = Icons.Star.withTint(AppColors.muted),
        initialRating = 3
    ) {}
}
