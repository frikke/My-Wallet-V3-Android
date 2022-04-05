package piuk.blockchain.android.ui.interest.tbm.presentation.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.EmptyStateView

@Composable
fun InterestDashboardError() {
    AndroidView(
        factory = { context ->
            EmptyStateView(context).apply {
                setDetails(
                    title = R.string.rewards_error_title,
                    description = R.string.rewards_error_desc,
                    contactSupportEnabled = true
                ) {
                }
            }
        }
    )
}

@Preview
@Composable
private fun PreviewInterestDashboardError() {
    InterestDashboardError()
}