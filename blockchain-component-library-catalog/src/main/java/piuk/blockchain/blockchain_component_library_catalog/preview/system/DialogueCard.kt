package piuk.blockchain.blockchain_component_library_catalog.preview.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.system.DialogueCard
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.blockchain_component_library_catalog.R

@Preview(name = "DialogueCard Icon + Title", group = "System")
@Composable
fun DialogueCardPreview() {
    AppTheme {
        AppSurface {
            DialogueCard(
                icon = com.blockchain.componentlib.R.drawable.ic_chip_checkmark,
                title = "Title",
                body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,",
                firstButton = DialogueButton("Button 1") {},
                secondButton = DialogueButton("Button 2") {}
            )
        }
    }
}

@Preview(name = "DialogueCard Title", group = "System")
@Composable
fun DialogueCardNoIconPreview() {
    AppTheme {
        AppSurface {
            DialogueCard(
                title = "Title",
                body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,",
                firstButton = DialogueButton("Button 1") {},
                secondButton = DialogueButton("Button 2") {}
            )
        }
    }
}

@Preview(name = "DialogueCard One Button", group = "System")
@Composable
fun DialogueCardOneButtonPreview() {
    AppTheme {
        AppSurface {
            DialogueCard(
                body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,",
                firstButton = DialogueButton("Button 1") {}
            )
        }
    }
}