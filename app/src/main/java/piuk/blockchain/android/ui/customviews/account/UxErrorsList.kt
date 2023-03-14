package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.common.model.ServerSideUxErrorInfo

class UxErrorsList @JvmOverloads constructor(
    ctx: Context,
    val attr: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(ctx, attr, defStyle) {

    private val errorsAdapter = UxErrorsAdapter()

    init {
        layoutManager = LinearLayoutManager(
            context,
            VERTICAL,
            false
        )
        itemAnimator = null
        adapter = errorsAdapter
    }

    fun submitList(errors: List<ServerSideUxErrorInfo>) {
        visibleIf { errors.isNotEmpty() }
        errorsAdapter.submitList(errors)
    }
}

private class UxErrorsAdapter : ListAdapter<ServerSideUxErrorInfo, ExErrorViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExErrorViewHolder =
        ExErrorViewHolder(ComposeView(parent.context))

    override fun onBindViewHolder(holder: ExErrorViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ServerSideUxErrorInfo>() {
            override fun areItemsTheSame(
                oldItem: ServerSideUxErrorInfo,
                newItem: ServerSideUxErrorInfo
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: ServerSideUxErrorInfo,
                newItem: ServerSideUxErrorInfo
            ): Boolean = oldItem == newItem
        }
    }
}

private class ExErrorViewHolder(
    private val composeView: ComposeView,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(error: ServerSideUxErrorInfo) {
        composeView.setContent {
            Box(
                modifier = Modifier.padding(bottom = AppTheme.dimensions.smallSpacing)
            ) {
                CardAlert(
                    title = error.title,
                    subtitle = error.description,
                    alertType = AlertType.Warning,
                    isBordered = true,
                    isDismissable = false,
                )
            }
        }
    }
}
