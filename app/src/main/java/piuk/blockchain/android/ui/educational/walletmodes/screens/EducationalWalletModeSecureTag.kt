package piuk.blockchain.android.ui.educational.walletmodes.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Purple0000

@Composable
fun EducationalWalletModeSecureTag(
    text: String,
    color: Color
) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiSmallest),
        elevation = AppTheme.dimensions.smallElevation
    ) {
        Text(
            modifier = Modifier.padding(
                vertical = AppTheme.dimensions.smallestSpacing,
                horizontal = AppTheme.dimensions.tinySpacing
            ),
            text = text,
            style = AppTheme.typography.caption2,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewEducationalWalletModeSecureTag() {
    EducationalWalletModeSecureTag(
        text = "Secured by You",
        color = Purple0000
    )
}
