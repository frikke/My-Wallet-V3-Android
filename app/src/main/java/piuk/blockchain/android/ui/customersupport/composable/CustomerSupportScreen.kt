package piuk.blockchain.android.ui.customersupport.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import piuk.blockchain.android.BuildConfig

@Composable
fun CustomerSupportScreen(
    onDismiss: () -> Unit,
    contactUsClicked: () -> Unit,
    faqClicked: () -> Unit,
    copyCommitHash: (hash: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AppColors.background,
                AppTheme.shapes.large.topOnly()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = onDismiss,
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_spacing)))
        Image(
            imageResource = ImageResource.Local(R.drawable.ic_blockchain),
            modifier = Modifier.size(dimensionResource(R.dimen.size_huge))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_spacing)))

        Text(
            text = stringResource(id = com.blockchain.stringResources.R.string.customer_support_title),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.tiny_spacing)))

        Text(
            text = stringResource(id = com.blockchain.stringResources.R.string.customer_support_description),
            style = AppTheme.typography.paragraph1,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.title,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.standard_spacing),
                end = dimensionResource(R.dimen.standard_spacing)
            )
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.standard_spacing)))

        Text(
            text = stringResource(
                id = com.blockchain.stringResources.R.string.app_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            ),
            style = AppTheme.typography.caption1,
            textAlign = TextAlign.Center,
            color = AppColors.body,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.standard_spacing),
                    end = dimensionResource(R.dimen.standard_spacing)
                )
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.small_spacing)))

        MinimalPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.standard_spacing),
                    end = dimensionResource(R.dimen.standard_spacing)
                ),
            text = stringResource(id = com.blockchain.stringResources.R.string.customer_support_contact_us),
            onClick = contactUsClicked
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.small_spacing)))

        MinimalPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.standard_spacing),
                    end = dimensionResource(R.dimen.standard_spacing)
                ),
            text = stringResource(id = com.blockchain.stringResources.R.string.customer_support_faq),
            onClick = faqClicked
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.small_spacing)))

        if (BuildConfig.DEBUG) {
            Text(
                text = "commit hash: ${BuildConfig.COMMIT_HASH} (click to copy)",
                style = AppTheme.typography.caption1,
                textAlign = TextAlign.Center,
                color = AppColors.body,
                modifier = Modifier
                    .clickable { copyCommitHash(BuildConfig.COMMIT_HASH) }
                    .padding(
                        start = dimensionResource(R.dimen.standard_spacing),
                        end = dimensionResource(R.dimen.standard_spacing)
                    )

            )

            Spacer(Modifier.size(dimensionResource(R.dimen.small_spacing)))
        }
    }
}

@Preview
@Composable
fun PreviewCustomerSupportScreen() {
    CustomerSupportScreen({}, {}, {}, {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCustomerSupportScreenDark() {
    PreviewCustomerSupportScreen()
}
