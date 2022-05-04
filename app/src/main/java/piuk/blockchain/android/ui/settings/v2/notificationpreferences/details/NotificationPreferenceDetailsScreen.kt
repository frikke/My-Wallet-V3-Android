package piuk.blockchain.android.ui.settings.v2.notificationpreferences.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.tablerow.ToggleTableRow
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.settings.v2.notificationpreferences.component.PreferenceLoadingError
import piuk.blockchain.android.ui.settings.v2.notificationpreferences.component.PreferenceLoadingProgress

@Composable
fun NotificationPreferenceDetailsScreen(
    state: NotificationPreferenceDetailsViewState,
    onCheckedChanged: (methods: List<ContactMethod>, changed: ContactMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(AppTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.Start
        ) {

            SimpleText(
                text = state.title,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            SimpleText(
                text = state.description,
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
        }

        Spacer(
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.standard_margin),
                end = dimensionResource(id = R.dimen.standard_margin),
                bottom = dimensionResource(id = R.dimen.standard_margin)
            )
        )

        when (state) {
            is NotificationPreferenceDetailsViewState.Loading -> PreferenceLoadingProgress()
            is NotificationPreferenceDetailsViewState.Error -> PreferenceLoadingError({}, {})
            is NotificationPreferenceDetailsViewState.Data -> PreferencesList(state.methods, onCheckedChanged)
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

@Preview
@Composable
private fun PreferencesListPreview() {
    NotificationPreferenceDetailsScreen(
        NotificationPreferenceDetailsViewState.Data(
            "Price alerts",
            "Sent when a particular asset increases or decreases in price",
            //
            listOf(
                ContactMethod("EMAIL", "Emails", true, true),
                ContactMethod("PUSH", "Push notifications", true, false),
                ContactMethod("IN_APP", "In-app messages", false, false),
                ContactMethod("IN_APP", "In-app messages", false, true),
            )
        )
    ) { _, _ -> }
}

@Composable
fun PreferencesList(
    methods: List<ContactMethod>,
    onCheckedChanged: (methods: List<ContactMethod>, contactMethod: ContactMethod) -> Unit
) {
    Column {
        methods.forEach { method ->
            ToggleTableRow(
                primaryText = method.title,
                isChecked = method.isMethodEnabled,
                enabled = method.isButtonEnabled,
                onCheckedChange = { onCheckedChanged(methods, method) }
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}
