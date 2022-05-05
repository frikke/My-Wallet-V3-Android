package piuk.blockchain.android.ui.settings.v2.notificationpreferences.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.system.CircularProgressBar

@Preview
@Composable
fun PreferenceLoadingProgressPreview() {
    PreferenceLoadingProgress()
}

@Composable
fun PreferenceLoadingProgress() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressBar()
    }
}
