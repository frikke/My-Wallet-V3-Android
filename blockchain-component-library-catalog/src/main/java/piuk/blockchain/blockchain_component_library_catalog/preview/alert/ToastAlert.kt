package piuk.blockchain.blockchain_component_library_catalog.preview.alert

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.alert.DefaultToastAlert
import com.blockchain.componentlib.alert.ErrorToastAlert
import com.blockchain.componentlib.alert.SuccessToastAlert
import com.blockchain.componentlib.alert.WarningToastAlert
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default Text", group = "ToastAlert")
@Composable
fun DefaultToastAlert_Text() {
    AppTheme {
        AppSurface {
            DefaultToastAlert(text = "Default")
        }
    }
}

@Preview(name = "Default Text+Icon", group = "ToastAlert")
@Composable
fun DefaultToastAlert_TextIcon() {
    AppTheme {
        AppSurface {
            DefaultToastAlert(text = "Default", startIcon = ImageResource.Local(R.drawable.ic_refresh))
        }
    }
}

@Preview(name = "Success Text", group = "ToastAlert")
@Composable
fun SuccessToastAlert_Text() {
    AppTheme {
        AppSurface {
            SuccessToastAlert(text = "Success")
        }
    }
}

@Preview(name = "Success Text+Icon", group = "ToastAlert")
@Composable
fun SuccessToastAlert_TextIcon() {
    AppTheme {
        AppSurface {
            SuccessToastAlert(text = "Success", startIconDrawableRes = R.drawable.ic_refresh)
        }
    }
}

@Preview(name = "Warning Text", group = "ToastAlert")
@Composable
fun WarningToastAlert_Text() {
    AppTheme {
        AppSurface {
            WarningToastAlert(text = "Warning")
        }
    }
}

@Preview(name = "Warning Text+Icon", group = "ToastAlert")
@Composable
fun WarningToastAlert_TextIcon() {
    AppTheme {
        AppSurface {
            WarningToastAlert(text = "Warning", startIconDrawableRes = R.drawable.ic_refresh)
        }
    }
}

@Preview(name = "Error Text", group = "ToastAlert")
@Composable
fun ErrorToastAlert_Text() {
    AppTheme {
        AppSurface {
            ErrorToastAlert(text = "Error")
        }
    }
}

@Preview(name = "Error Text+Icon", group = "ToastAlert")
@Composable
fun ErrorToastAlert_TextIcon() {
    AppTheme {
        AppSurface {
            ErrorToastAlert(text = "Error", startIconDrawableRes = R.drawable.ic_refresh)
        }
    }
}