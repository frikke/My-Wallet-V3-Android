package com.blockchain.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.common.R
import com.blockchain.common.databinding.ViewEmptyStateBinding
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.getResolvedDrawable

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyStateBinding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun setDetails(
        @StringRes title: Int = com.blockchain.stringResources.R.string.common_empty_title,
        @StringRes description: Int = com.blockchain.stringResources.R.string.common_empty_details,
        @DrawableRes icon: Int = Icons.Filled.User.id,
        @StringRes ctaText: Int = com.blockchain.stringResources.R.string.common_empty_cta,
        contactSupportEnabled: Boolean = false,
        action: () -> Unit,
        onContactSupport: () -> Unit = {}
    ) {
        with(binding) {
            viewEmptyTitle.text = context.getString(title)
            viewEmptyDesc.text = context.getString(description)
            viewEmptyIcon.setImageDrawable(context.getResolvedDrawable(icon))
            viewEmptyCta.text = context.getString(ctaText)
            viewEmptyCta.setOnClickListener {
                action()
            }
            viewEmptySupportCta.visibleIf {
                contactSupportEnabled
            }
            viewEmptySupportCta.setOnClickListener {
                onContactSupport()
            }
        }
    }
}
