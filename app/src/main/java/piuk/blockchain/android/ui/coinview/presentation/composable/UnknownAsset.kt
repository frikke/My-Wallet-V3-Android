package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Composable
fun UnknownAsset(
    onContactSupportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.xHugeSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1F))

        Image(
            imageResource = ImageResource.Local(R.drawable.ic_coinview_error)
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        Text(
            text = stringResource(com.blockchain.stringResources.R.string.coinview_no_asset_title),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        val descriptionAnnotation = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = AppColors.body
                )
            ) {
                append(stringResource(id = com.blockchain.stringResources.R.string.coinview_no_asset_blurb_1))
            }

            append(" ")

            pushStringAnnotation(
                tag = "support",
                annotation = stringResource(id = com.blockchain.stringResources.R.string.coinview_no_asset_blurb_2)
            )
            withStyle(
                style = SpanStyle(
                    color = AppTheme.colors.primary
                )
            ) {
                append(stringResource(id = com.blockchain.stringResources.R.string.coinview_no_asset_blurb_2))
            }
            pop()
        }

        ClickableText(
            text = descriptionAnnotation,
            style = AppTheme.typography.paragraph2.copy(textAlign = TextAlign.Center),
            onClick = { offset ->
                descriptionAnnotation.getStringAnnotations(
                    tag = "support",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    onContactSupportClick()
                }
            }
        )

        Spacer(modifier = Modifier.weight(1.2F))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUnknownAsset() {
    UnknownAsset {}
}
