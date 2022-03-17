package piuk.blockchain.android.ui.customviews

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.blockchain.componentlib.alert.SnackbarAlertView
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.findSuitableParent
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class BlockchainSnackbar(
    parent: ViewGroup,
    content: SnackbarAlertView
) : BaseTransientBottomBar<BlockchainSnackbar>(parent, content, content) {

    init {
        getView().setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.transparent))
        getView().setPadding(0, 0, 0, 0)
    }

    companion object {
        fun make(
            view: View,
            message: String,
            @Duration duration: Int = Snackbar.LENGTH_LONG,
            type: SnackbarType = SnackbarType.Info,
            actionLabel: String? = null,
            onClick: () -> Unit = {}
        ): BlockchainSnackbar {

            // First we find a suitable parent for our custom view
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            val customView = SnackbarAlertView(view.context).apply {
                this.message = message
                this.actionLabel = actionLabel.orEmpty()
                this.onClick = onClick
                this.type = type
            }

            return BlockchainSnackbar(
                parent,
                customView
            ).setDuration(duration).apply {
                // set the elevation here to max possible to make sure snackbars always show above
                // any other UI element (fixes snackbars showing behind bottom sheets)
                this.view.elevation = Float.MAX_VALUE
            }
        }

        fun make(
            view: View,
            @StringRes message: Int,
            @Duration duration: Int = Snackbar.LENGTH_LONG,
            type: SnackbarType = SnackbarType.Info,
            actionLabel: String? = null,
            onClick: () -> Unit = {}
        ): BlockchainSnackbar {

            // First we find a suitable parent for our custom view
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            val customView = SnackbarAlertView(view.context).apply {
                this.message = view.context.getString(message)
                this.actionLabel = actionLabel.orEmpty()
                this.onClick = onClick
                this.type = type
            }

            return BlockchainSnackbar(
                parent,
                customView
            ).setDuration(duration)
        }
    }
}
