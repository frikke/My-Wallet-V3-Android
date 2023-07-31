package piuk.blockchain.android.ui.settings.security.biometrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.DialogFragment
import com.blockchain.componentlib.icons.Fingerprint
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.system.DialogueCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.stringResources.R

class RequestBiometricsDialog : DialogFragment() {

    interface Host {
        fun enrollBiometrics()
        fun cancel()
    }

    val host: Host by lazy {
        parentFragment as? Host
            ?: activity as? Host
            ?: throw IllegalStateException("Host is not a RequestBiometricsDialog.Host")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    RequestBiometricsDialogContent(
                        enroll = {
                            host.enrollBiometrics()
                        },
                        cancel = {
                            host.cancel()
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance() = RequestBiometricsDialog()
        val TAG = RequestBiometricsDialog::class.simpleName
    }
}

@Composable
fun RequestBiometricsDialogContent(enroll: () -> Unit, cancel: () -> Unit) {
    DialogueCard(
        icon = Icons.Filled.Fingerprint.withTint(AppTheme.colors.primary),
        title = stringResource(R.string.biometric_request_dialog_title),
        body = stringResource(R.string.biometric_request_dialog_body),
        firstButton = DialogueButton(
            text = stringResource(R.string.common_dont_allow),
            onClick = cancel
        ),
        secondButton = DialogueButton(
            text = stringResource(R.string.common_ok),
            showIndication = true,
            onClick = enroll
        ),
        onDismissRequest = cancel
    )
}

@Preview(showBackground = true)
@Composable
fun RequestBiometricsDialogContentPreview() {
    AppTheme {
        RequestBiometricsDialogContent(
            enroll = {},
            cancel = {}
        )
    }
}
