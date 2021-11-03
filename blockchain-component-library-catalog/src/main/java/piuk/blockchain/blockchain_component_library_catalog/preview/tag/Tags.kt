package piuk.blockchain.blockchain_component_library_catalog.preview.tag

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.tag.ErrorTag
import com.blockchain.componentlib.tag.InfoAltTag
import com.blockchain.componentlib.tag.SuccessTag
import com.blockchain.componentlib.tag.WarningTag
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default tag", group = "Tags")
@Composable
fun DefaultTag() {
    AppTheme {
        AppSurface {
            DefaultTag(text = "Default")
        }
    }
}

@Preview(name = "Error tag", group = "Tags")
@Composable
fun ErrorTag() {
    AppTheme {
        AppSurface {
            ErrorTag(text = "Error")
        }
    }
}

@Preview(name = "Info Alt tag", group = "Tags")
@Composable
fun InfoAltTag() {
    AppTheme {
        AppSurface {
            InfoAltTag(text = "Info Alt")
        }
    }
}

@Preview(name = "Success tag", group = "Tags")
@Composable
fun SuccessTag() {
    AppTheme {
        AppSurface {
            SuccessTag(text = "Success")
        }
    }
}

@Preview(name = "Warning tag", group = "Tags")
@Composable
fun WarningTag() {
    AppTheme {
        AppSurface {
            WarningTag(text = "Warning")
        }
    }
}