package piuk.blockchain.android.ui.settings.notificationpreferences.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.settings.notificationpreferences.component.PreferenceLoadingError
import piuk.blockchain.android.ui.settings.notificationpreferences.component.PreferenceLoadingProgress
import piuk.blockchain.android.ui.settings.notificationpreferences.component.PreferenceToggleRow

@Composable
fun NotificationPreferenceDetailsScreen(
    state: NotificationPreferenceDetailsViewState,
    onCheckedChanged: (methods: List<ContactMethod>, changed: ContactMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .background(AppColors.backgroundSecondary)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(AppTheme.dimensions.standardSpacing),
            horizontalAlignment = Alignment.Start
        ) {
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = state.description,
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
        }

        Spacer(
            modifier = Modifier.padding(
                start = dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing),
                end = dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing),
                bottom = dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing)
            )
        )

        when (state) {
            is NotificationPreferenceDetailsViewState.Loading -> PreferenceLoadingProgress()
            is NotificationPreferenceDetailsViewState.Error -> PreferenceLoadingError({}, {})
            is NotificationPreferenceDetailsViewState.Data -> PreferencesList(state.methods, onCheckedChanged)
        }
    }
}

@Composable
fun PreferencesList(
    methods: List<ContactMethod>,
    onCheckedChanged: (methods: List<ContactMethod>, contactMethod: ContactMethod) -> Unit
) {
    Column {
        methods.forEach { method ->
            val text = if (method.required) {
                stringResource(
                    id = com.blockchain.stringResources.R.string.settings_notification_required,
                    method.title
                )
            } else {
                method.title
            }
            PreferenceToggleRow(
                primaryText = text,
                isChecked = method.isMethodEnabled,
                enabled = !method.required,
                onCheckedChange = { onCheckedChanged(methods, method) }
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    NotificationPreferenceDetailsScreen(
        NotificationPreferenceDetailsViewState.Loading
    ) { _, _ -> }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLoadingDark() {
    PreviewLoading()
}

@Preview
@Composable
private fun ErrorLoadingPreview() {
    NotificationPreferenceDetailsScreen(
        NotificationPreferenceDetailsViewState.Error(
            "Price alerts",
            "Sent when a particular asset increases or decreases in price"
        )
    ) { _, _ -> }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorLoadingPreviewDark() {
    ErrorLoadingPreview()
}

@Preview
@Composable
private fun PreferencesListPreview() {
    NotificationPreferenceDetailsScreen(
        NotificationPreferenceDetailsViewState.Data(
            "Price alerts",
            "Sent when a particular asset increases or decreases in price",
            //
            listOf(
                ContactMethod("Emails", "EMAIL", true, true),
                ContactMethod("Push notifications", "PUSH", true, false),
                ContactMethod("In-app messages", "IN_APP", false, false),
                ContactMethod("In-app messages", "IN_APP", false, true)
            )
        )
    ) { _, _ -> }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreferencesListPreviewDark() {
    PreferencesListPreview()
}
