package piuk.blockchain.android.ui.customersupport.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.sheets.CustomButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.sheets.toCustomButtonComposable
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey700
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R

@Composable
fun CustomerSupportScreen(
    onDismiss: () -> Unit,
    contactUsClicked: () -> Unit,
    faqClicked: () -> Unit,
    copyCommitHash: (hash: String) -> Unit
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark800
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(dimensionResource(id = R.dimen.tiny_margin))),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = onDismiss,
            shouldShowDivider = false
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        Image(
            imageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_blockchain),
            modifier = Modifier.size(dimensionResource(R.dimen.size_huge))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))

        Text(
            text = stringResource(id = R.string.customer_support_title),
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title,
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = stringResource(id = R.string.customer_support_description),
            style = AppTheme.typography.paragraph1,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.title,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.standard_margin),
                end = dimensionResource(R.dimen.standard_margin)
            )
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.standard_margin)))

        Text(
            text = stringResource(
                id = R.string.app_version,
                " ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            ),
            style = AppTheme.typography.caption1,
            textAlign = TextAlign.Center,
            color = Grey700,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.standard_margin),
                    end = dimensionResource(R.dimen.standard_margin)
                )
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))

        CustomButton(
            text = stringResource(id = R.string.customer_support_contact_us),
            onClick = contactUsClicked,
            type = ButtonType.MINIMAL
        ).toCustomButtonComposable().invoke(this)

        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))

        CustomButton(
            text = stringResource(id = R.string.customer_support_faq),
            onClick = faqClicked,
            type = ButtonType.MINIMAL
        ).toCustomButtonComposable().invoke(this)

        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))

        if (BuildConfig.DEBUG) {
            Text(
                text = "commit hash: ${BuildConfig.COMMIT_HASH} (click to copy)",
                style = AppTheme.typography.caption1,
                textAlign = TextAlign.Center,
                color = Grey700,
                modifier = Modifier
                    .clickable { copyCommitHash(BuildConfig.COMMIT_HASH) }
                    .padding(
                        start = dimensionResource(R.dimen.standard_margin),
                        end = dimensionResource(R.dimen.standard_margin)
                    )

            )

            Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        }
    }
}

@Preview(name = "Customer Support", showBackground = true)
@Composable
fun PreviewCustomerSupportScreen() {
    CustomerSupportScreen({}, {}, {}, {})
}
