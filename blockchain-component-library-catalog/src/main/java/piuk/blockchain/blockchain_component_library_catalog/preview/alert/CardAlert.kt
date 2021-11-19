package piuk.blockchain.blockchain_component_library_catalog.preview.alert

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default Card", group = "CardAlert")
@Composable
fun DefaultCardAlert() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Default title", subtitle = "Default subtitle")
        }
    }
}

@Preview(name = "Default Card Bordered", group = "CardAlert")
@Composable
fun DefaultCardAlert_Bordered() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Default title", subtitle = "Default subtitle", isBordered = true)
        }
    }
}

@Preview(name = "Success Card", group = "CardAlert")
@Composable
fun SuccessCardAlert() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Success title", subtitle = "Success subtitle", alertType = AlertType.Success)
        }
    }
}

@Preview(name = "Success Card Bordered", group = "CardAlert")
@Composable
fun SuccessCardAlert_Bordered() {
    AppTheme {
        AppSurface {
            CardAlert(
                title = "Success title", subtitle = "Success subtitle", isBordered = true, alertType = AlertType.Success
            )
        }
    }
}

@Preview(name = "Warning Card", group = "CardAlert")
@Composable
fun WarningCardAlert() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Warning title", subtitle = "Warning subtitle", alertType = AlertType.Warning)
        }
    }
}

@Preview(name = "Warning Card Bordered", group = "CardAlert")
@Composable
fun WarningCardAlert_Bordered() {
    AppTheme {
        AppSurface {
            CardAlert(
                title = "Warning title", subtitle = "Warning subtitle", isBordered = true, alertType = AlertType.Warning
            )
        }
    }
}

@Preview(name = "Error Card", group = "CardAlert")
@Composable
fun ErrorCardAlert() {
    AppTheme {
        AppSurface {
            CardAlert(title = "Error title", subtitle = "Error subtitle", alertType = AlertType.Error)
        }
    }
}

@Preview(name = "Warning Card Bordered", group = "CardAlert")
@Composable
fun ErrorCardAlert_Bordered() {
    AppTheme {
        AppSurface {
            CardAlert(
                title = "Error title", subtitle = "Error subtitle", isBordered = true, alertType = AlertType.Error
            )
        }
    }
}