package piuk.blockchain.android.ui.settings.v2.notificationpreferences.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.PrimarySwitch
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R

@Preview
@Composable
fun PreferenceToggleRowPreviewCheckedDisabled_NotChecked() {
    PreferenceToggleRow({}, "Push notification")
}

@Preview
@Composable
fun PreferenceToggleRowPreviewDisabled_Checked() {
    PreferenceToggleRow({}, "Emails (required)", true, false)
}

@Preview
@Composable
fun PreferenceToggleRowPreviewEnabled_NotChecked() {
    PreferenceToggleRow({}, "SMS", false, true)
}

@Preview
@Composable
fun PreferenceToggleRowPreviewEnabled_Checked() {
    PreferenceToggleRow({}, "In-App", true, true)
}

@Composable
fun PreferenceToggleRow(
    onCheckedChange: (isChecked: Boolean) -> Unit,
    primaryText: String,
    isChecked: Boolean = false,
    enabled: Boolean = true
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(AppTheme.colors.background)
                .padding(
                    start = dimensionResource(R.dimen.standard_spacing),
                    end = dimensionResource(R.dimen.standard_spacing)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween

        ) {
            Text(
                text = primaryText,
                style = AppTheme.typography.body2,
            )

            PrimarySwitch(
                isChecked = isChecked,
                onCheckChanged = onCheckedChange
            )
        }
        if (!enabled) {
            Box(
                modifier = Modifier
                    .background(White.copy(alpha = .75f))
                    .matchParentSize()
                    .clickable(enabled = true, onClick = { })
            )
        }
    }
}
