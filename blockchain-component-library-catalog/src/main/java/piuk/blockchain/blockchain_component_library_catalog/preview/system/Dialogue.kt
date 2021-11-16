package piuk.blockchain.blockchain_component_library_catalog.preview.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.system.Dialogue
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Dialogue Short Text", group = "System")
@Composable
fun DialoguePreview() {
    AppTheme {
        AppSurface {
            Dialogue(
                body = "Discard draft?",
                firstButton = DialogueButton("Button 1") {},
                secondButton = DialogueButton("Button 2") {}
            )
        }
    }
}

@Preview(name = "Dialogue Long Text", group = "System")
@Composable
fun DialogueLongTextPreview() {
    AppTheme {
        AppSurface {
            Dialogue(
                body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor inc" +
                    "ididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,",
                firstButton = DialogueButton("Button 1") {},
                secondButton = DialogueButton("Button 2") {}
            )
        }
    }
}
