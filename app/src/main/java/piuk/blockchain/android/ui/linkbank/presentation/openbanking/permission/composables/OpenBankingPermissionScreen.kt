package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalErrorButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import java.net.URL
import piuk.blockchain.android.urllinks.URL_OPEN_BANKING_PRIVACY_POLICY

@Composable
fun OpenBankingPermissionScreen(
    institution: YapilyInstitution,
    termsOfServiceLink: String,
    urlOnclick: (String) -> Unit,
    approveOnClick: () -> Unit,
    denyOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(com.blockchain.componentlib.R.dimen.huge_spacing),
                vertical = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1F))

        Image(
            modifier = Modifier
                .size(dimensionResource(id = com.blockchain.common.R.dimen.asset_icon_size_large)),
            imageResource = institution.iconLink?.toString()
                ?.let { ImageResource.Remote(url = it) }
                ?: ImageResource.None
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)))

        Text(
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.title3,
            color = Grey900,
            textAlign = TextAlign.Center,
            text = stringResource(
                id = com.blockchain.stringResources.R.string.open_banking_permission_link_to_bank,
                institution.name
            )
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)))

        TermAndPrivacyText(
            termsOfServiceLink = termsOfServiceLink,
            urlOnclick = urlOnclick
        )

        Spacer(modifier = Modifier.height(100.dp))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = com.blockchain.stringResources.R.string.common_approve),
            onClick = approveOnClick
        )

        Spacer(modifier = Modifier.height(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        MinimalErrorButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = com.blockchain.stringResources.R.string.common_deny),
            onClick = denyOnClick
        )
    }
}

@Composable
private fun TermAndPrivacyText(
    termsOfServiceLink: String,
    urlOnclick: (String) -> Unit
) {
    val tosTag = "TOS"
    val privacyTag = "PRIVACY"
    val annotatedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Grey900)) {
            append(stringResource(id = com.blockchain.stringResources.R.string.open_banking_permission_confirmation))
        }

        append(" ")

        pushStringAnnotation(
            tag = tosTag,
            annotation = termsOfServiceLink
        )
        withStyle(style = SpanStyle(color = Blue600)) {
            append(stringResource(id = com.blockchain.stringResources.R.string.open_banking_permission_terms_service))
        }
        pop()

        append(" & ")

        pushStringAnnotation(
            tag = privacyTag,
            annotation = URL_OPEN_BANKING_PRIVACY_POLICY
        )
        withStyle(style = SpanStyle(color = Blue600)) {
            append(stringResource(id = com.blockchain.stringResources.R.string.open_banking_permission_privacy_policy))
        }
        pop()
    }

    ClickableText(
        modifier = Modifier.fillMaxWidth(),
        style = AppTheme.typography.paragraph1.copy(textAlign = TextAlign.Center),
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = tosTag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                urlOnclick(annotation.item)
            }

            annotatedText.getStringAnnotations(
                tag = privacyTag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                urlOnclick(annotation.item)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewOpenBankingPermissionScreen() {
    OpenBankingPermissionScreen(
        institution = YapilyInstitution(
            listOf(),
            "Institution Name",
            "id",
            URL("https://images.yapily.com/image/0291d873-f7d5-4696-bda8-6ea17a788bb1?size=0")
        ),
        termsOfServiceLink = "link",
        urlOnclick = {},
        approveOnClick = {},
        denyOnClick = {}
    )
}
