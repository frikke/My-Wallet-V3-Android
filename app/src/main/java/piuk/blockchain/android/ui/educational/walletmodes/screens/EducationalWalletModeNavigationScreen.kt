package piuk.blockchain.android.ui.educational.walletmodes.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.presentation.R

@Composable
fun EducationalWalletModeNavigationScreen(
    doneOnClick: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(title = stringResource(R.string.backup_phrase_title_secure_wallet), onBackButtonClick = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.standard_margin)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1F))

            Image(
                imageResource = ImageResource.Local(R.drawable.ic_backup_successful)
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

            Text(
                text = stringResource(R.string.backup_success_title),
                style = AppTheme.typography.subheading,
                color = Grey900,
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

            Text(
                text = stringResource(R.string.backup_success_description),
                style = AppTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = Grey900,
            )

            Spacer(modifier = Modifier.weight(2F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.done),
                onClick = doneOnClick
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewBackupSuccessScreen() {
    EducationalWalletModeNavigationScreen {}
}
