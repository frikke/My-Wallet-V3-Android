package piuk.blockchain.android.ui.maintenance.presentation.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.button.SecondaryButton
import piuk.blockchain.android.R

@Composable
fun AppMaintenanceSkipUpdateButton(onClick: () -> Unit) {
    SecondaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = R.string.app_maintenance_cta_update_later),
        onClick = { },
    )
}