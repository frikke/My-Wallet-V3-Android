package piuk.blockchain.android.ui.transfer.receive.detail

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import piuk.blockchain.android.R
import piuk.blockchain.android.util.copyToClipboard

fun Activity.copyAddress(receiveAddress: ReceiveAddress, confirmationAnchorView: View) {
    AlertDialog.Builder(this, R.style.AlertDialogStyle)
        .setTitle(R.string.app_name)
        .setMessage(R.string.receive_address_to_clipboard)
        .setCancelable(false)
        .setPositiveButton(R.string.common_yes) { _, _ ->
            copyToClipboard("Send address", receiveAddress.address)
            BlockchainSnackbar.make(
                confirmationAnchorView,
                getString(R.string.copied_to_clipboard), type = SnackbarType.Success
            ).show()
        }
        .setNegativeButton(R.string.common_no, null)
        .show()
}
