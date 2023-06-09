package com.blockchain.presentation

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType

fun Context.getResolvedColor(@ColorRes color: Int): Int = ContextCompat.getColor(this, color)

/**
 * Returns a color associated with a particular resource ID.
 *
 * @param color The Res ID of the color.
 */
fun Fragment.getResolvedColor(@ColorRes color: Int): Int =
    ContextCompat.getColor(requireContext(), color)

/**
 * Returns a nullable Drawable associated with a particular resource ID.
 *
 * @param drawable The Res ID of the Drawable.
 */
fun Context.getResolvedDrawable(@DrawableRes drawable: Int): Drawable? =
    ContextCompat.getDrawable(this, drawable)

fun Context.copyToClipboard(label: String, text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
        ClipData.newPlainText(label, text).also { clipData ->
            setPrimaryClip(clipData)
        }
    }
}

fun Context.copyToClipboardWithConfirmationDialog(
    confirmationAnchorView: View,
    @StringRes confirmationTitle: Int = com.blockchain.stringResources.R.string.app_name,
    @StringRes confirmationMessage: Int,
    label: String,
    text: String
) {
    AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
        .setTitle(confirmationTitle)
        .setMessage(confirmationMessage)
        .setCancelable(false)
        .setPositiveButton(com.blockchain.stringResources.R.string.common_yes) { _, _ ->
            copyToClipboard(label, text)
            BlockchainSnackbar.make(
                confirmationAnchorView,
                getString(com.blockchain.stringResources.R.string.copied_to_clipboard),
                type = SnackbarType.Success
            ).show()
        }
        .setNegativeButton(com.blockchain.stringResources.R.string.common_no, null)
        .show()
}

fun ComponentActivity.disableBackPress(
    owner: LifecycleOwner? = null,
    callbackEnabled: Boolean = true
): OnBackPressedCallback {
    return onBackPressedDispatcher.addCallback(owner = owner, enabled = callbackEnabled) {
        // this will catch back press but would do nothing
    }
}
