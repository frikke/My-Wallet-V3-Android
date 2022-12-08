package piuk.blockchain.android.ui.prices.presentation.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.presentation.customviews.EmptyStateView
import piuk.blockchain.android.R
import piuk.blockchain.android.support.SupportCentreActivity

@Composable
fun PricesScreenError(action: () -> Unit) {
    AndroidView(
        factory = { context ->
            EmptyStateView(context).apply {
                setDetails(
                    title = R.string.prices_error_title,
                    description = R.string.prices_error_desc,
                    contactSupportEnabled = true,
                    action = action,
                    onContactSupport = { context.startActivity(SupportCentreActivity.newIntent(context)) }
                )
            }
        }
    )
}

@Preview
@Composable
private fun PreviewPriceScreenError() {
    PricesScreenError {}
}
