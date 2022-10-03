package piuk.blockchain.android.ui.settings.v2.notificationpreferences

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.settings.v2.notificationpreferences.component.PreferenceLoadingError
import piuk.blockchain.android.ui.settings.v2.notificationpreferences.component.PreferenceLoadingProgress

@Composable
fun NotificationPreferenceScreen(
    state: NotificationPreferencesViewState,
    onItemClicked: (preferenceId: Int) -> Unit,
    onRetryClicked: () -> Unit,
    onBackClicked: () -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.settings_notification_preferences_subtitle),
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
            is NotificationPreferencesViewState.Loading -> PreferenceLoadingProgress()
            is NotificationPreferencesViewState.Error -> PreferenceLoadingError(onRetryClicked, onBackClicked)
            is NotificationPreferencesViewState.Data -> PreferenceList(state.categories, onItemClicked)
        }
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    NotificationPreferenceScreen(NotificationPreferencesViewState.Loading, { }, { }, { })
}

@Preview
@Composable
private fun PreviewPreferenceList() {
    val categories = listOf(
        NotificationCategory("Wallet Activity", "Push, Email, SMS & In-App"),
        NotificationCategory("Security Alerts", "Push, Email, SMS & In-App"),
        NotificationCategory("Price Alerts", "Push, Email, & In-App"),
        NotificationCategory("Product News", "Email")
    )
    NotificationPreferenceScreen(NotificationPreferencesViewState.Data(categories), { }, { }, { })
}

@Composable
private fun PreferenceList(
    categories: List<NotificationCategory>,
    onItemClicked: (preferenceId: Int) -> Unit
) {
    Column(
        Modifier
            .background(Color.White)
            .fillMaxWidth()
    ) {
        categories.forEachIndexed { index, item ->
            DefaultTableRow(
                primaryText = item.title,
                secondaryText = item.notificationTypes,
                onClick = { onItemClicked(index) },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
private fun ErrorLoadingPreview() {
    NotificationPreferenceScreen(NotificationPreferencesViewState.Error, { }, { }, { })
}
