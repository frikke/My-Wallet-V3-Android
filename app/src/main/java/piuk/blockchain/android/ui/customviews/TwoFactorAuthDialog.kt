package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.InputType
import android.text.method.DigitsKeyListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.blockchain.componentlib.viewextensions.getAlertDialogPaddedView
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.wallet.api.data.Settings
import piuk.blockchain.android.R

fun getTwoFactorDialog(
    context: Context,
    authType: Int,
    walletPrefs: WalletStatusPrefs,
    positiveAction: (String) -> Unit,
    resendAction: (Boolean) -> Unit
): AlertDialog {
    val editText = AppCompatEditText(context)
    editText.setHint(com.blockchain.stringResources.R.string.two_factor_dialog_hint)

    val message = when (authType) {
        Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR -> {
            editText.inputType = InputType.TYPE_NUMBER_VARIATION_NORMAL
            editText.keyListener = DigitsKeyListener.getInstance("1234567890")
            com.blockchain.stringResources.R.string.two_factor_dialog_message_authenticator
        }
        Settings.AUTH_TYPE_SMS -> {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            com.blockchain.stringResources.R.string.two_factor_dialog_message_sms
        }
        else -> throw IllegalArgumentException("Auth Type $authType should not be passed to this function")
    }

    val builder = AlertDialog.Builder(context, com.blockchain.componentlib.R.style.AlertDialogStyle)
        .setTitle(com.blockchain.stringResources.R.string.two_factor_dialog_title)
        .setMessage(message)
        .setView(context.getAlertDialogPaddedView(editText))
        .setPositiveButton(android.R.string.ok) { _, _ ->
            positiveAction(editText.text.toString())
        }
        .setNegativeButton(android.R.string.cancel, null)

    if (authType == Settings.AUTH_TYPE_SMS) {
        builder.setNeutralButton(
            context.getString(
                com.blockchain.stringResources.R.string.two_factor_resend_sms,
                walletPrefs.resendSmsRetries
            )
        ) { _, _ ->
            if (walletPrefs.resendSmsRetries > 0) {
                walletPrefs.setResendSmsRetries(walletPrefs.resendSmsRetries - 1)
            }
            resendAction(walletPrefs.resendSmsRetries == 0)
        }
    }

    return builder.create()
}
