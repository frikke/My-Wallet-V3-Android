package piuk.blockchain.android.ui.linkbank.yapily.permission.composables

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
import com.blockchain.componentlib.button.DestructiveMinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.core.payments.model.YapilyInstitution
import piuk.blockchain.android.R
import java.net.URL

@Composable
fun YapilyPermissionScreen(
    institution: YapilyInstitution,
    termsOfServiceOnclick: () -> Unit,
    privacyPolicyOnClick: () -> Unit,
    approveOnClick: () -> Unit,
    denyOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(R.dimen.huge_margin),
                vertical = dimensionResource(R.dimen.small_margin)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1F))

        Image(
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.asset_icon_size_large)),
            imageResource = institution.iconLink?.toString()
                ?.let { ImageResource.Remote(url = it) }
                ?: ImageResource.None
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_margin)))

        Text(
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.title3,
            color = Grey900,
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.yapily_permission_link_to_bank, institution.name)
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.smallest_margin)))

        TermAndPrivacyText(
            termsOfServiceOnclick = termsOfServiceOnclick,
            privacyPolicyOnClick = privacyPolicyOnClick
        )

        Spacer(modifier = Modifier.height(100.dp))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.common_approve),
            onClick = approveOnClick
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_margin)))

        DestructiveMinimalButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.common_deny),
            onClick = denyOnClick
        )
    }
}

@Composable
private fun TermAndPrivacyText(
    termsOfServiceOnclick: () -> Unit,
    privacyPolicyOnClick: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Grey900)) {
            append(stringResource(id = R.string.yapily_permission_confirmation))
        }

        append(" ")

        pushStringAnnotation(
            tag = "tos",
            annotation = "https://developer.android.com"
        )
        withStyle(style = SpanStyle(color = Blue600)) {
            append(stringResource(id = R.string.yapily_permission_terms_service))
        }
        pop()

        append(" & ")

        pushStringAnnotation(
            tag = "privacy",
            annotation = "https://developer.android.com"
        )
        withStyle(style = SpanStyle(color = Blue600)) {
            append(stringResource(id = R.string.yapily_permission_privacy_policy))
        }
        pop()
    }

    ClickableText(
        modifier = Modifier.fillMaxWidth(),
        style = AppTheme.typography.paragraph1.copy(textAlign = TextAlign.Center),
        text = annotatedText,
        onClick = { offset ->
            // We check if there is an *URL* annotation attached to the text
            // at the clicked position
            annotatedText.getStringAnnotations(
                tag = "tos", start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                termsOfServiceOnclick()
            }

            annotatedText.getStringAnnotations(
                tag = "privacy", start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                privacyPolicyOnClick()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewYapilyPermissionScreen() {
    YapilyPermissionScreen(
        institution = YapilyInstitution(
            listOf(),
            "Institution Name",
            "id",
            URL("https://images.yapily.com/image/0291d873-f7d5-4696-bda8-6ea17a788bb1?size=0")
        ),
        termsOfServiceOnclick = {},
        privacyPolicyOnClick = {},
        approveOnClick = {},
        denyOnClick = {}
    )
}

