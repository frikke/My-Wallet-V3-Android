package piuk.blockchain.android.ui.interest.presentation.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.EmptyStateView

@Composable
fun InterestDashboardError(action: () -> Unit) {
    AndroidView(
        factory = { context ->
            EmptyStateView(context).apply {
                setDetails(
                    title = R.string.rewards_error_title,
                    description = R.string.rewards_error_desc,
                    contactSupportEnabled = true,
                    action = action
                )
            }
        }
    )
}

@Preview
@Composable
private fun PreviewInterestDashboardError() {
    InterestDashboardError {}
}
