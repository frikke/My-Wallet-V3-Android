package com.blockchain.componentlib.legacy

import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.ProgressDialogCompatBinding

/**
 * Creates an [AlertDialog] with a custom view for emulating a Material Design progress
 * dialog on pre-Lollipop devices.
 *
 * @param context The Activity Context
 */
class MaterialProgressDialog(context: Context) {

    private val dialog: AlertDialog
    private val binding: ProgressDialogCompatBinding =
        ProgressDialogCompatBinding.inflate(LayoutInflater.from(context))

    val isShowing: Boolean
        get() = dialog.isShowing

    init {
        dialog = AlertDialog.Builder(context, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(context.getString(com.blockchain.stringResources.R.string.app_name))
            .setView(binding.root)
            .create()

        val a: TypedArray =
            context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorAccent))

        binding.progressBar.indeterminateDrawable.setColorFilter(
            a.getColor(0, 0),
            PorterDuff.Mode.SRC_IN
        )
    }

    fun setMessage(message: String) {
        binding.txtMessage.text = message
    }

    fun setMessage(@StringRes message: Int) = binding.txtMessage.setText(message)

    fun setTitle(title: String) = dialog.setTitle(title)

    fun setTitle(@StringRes title: Int) = dialog.setTitle(title)

    fun setCancelable(cancelable: Boolean) = dialog.setCancelable(cancelable)

    fun show() = dialog.show()

    fun dismiss() = dialog.dismiss()

    fun setOnCancelListener(listener: DialogInterface.OnCancelListener) =
        dialog.setOnCancelListener(listener)

    fun setOnCancelListener(listener: () -> Unit) =
        dialog.setOnCancelListener { listener.invoke() }
}
