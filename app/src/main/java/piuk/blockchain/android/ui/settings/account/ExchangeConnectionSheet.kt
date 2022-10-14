package piuk.blockchain.android.ui.settings.account

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import java.io.Serializable
import piuk.blockchain.android.databinding.DialogSheetExchangeConnectBinding
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog

class ExchangeConnectionSheet : ErrorBottomDialog<DialogSheetExchangeConnectBinding>() {

    private val tagsList: List<TagViewState> by lazy {
        arguments?.getSerializable(ARG_TAGS) as? List<TagViewState> ?: emptyList()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetExchangeConnectBinding =
        DialogSheetExchangeConnectBinding.inflate(inflater, container, false)

    override fun init(content: Content) {
        with(binding) {
            exchangeTitle.text = content.title

            content.icon.takeIf { it > 0 }?.let {
                dialogIcon.setImageResource(it)
                dialogIcon.visible()
            } ?: dialogIcon.gone()

            exchangeBody.apply {
                with(content) {
                    text = descriptionToFormat?.let {
                        getString(descriptionToFormat.first, descriptionToFormat.second)
                    } ?: description
                    movementMethod = LinkMovementMethod.getInstance()
                    visibleIf { descriptionToFormat != null || description.isNotEmpty() }
                }
            }

            exchangeTags.apply {
                visibleIf { tagsList.isNotEmpty() }
                tags = tagsList
            }

            exchangePrimaryCta.apply {
                if (content.ctaButtonText != 0) {
                    text = getString(content.ctaButtonText)
                } else {
                    gone()
                }
            }

            exchangeSecondaryCta.apply {
                if (content.dismissText != 0) {
                    text = getString(content.dismissText)
                } else {
                    gone()
                }
            }

            exchangePrimaryCta.onClick = {
                this@ExchangeConnectionSheet.dismiss()
                onCtaClick()
            }
            exchangeSecondaryCta.onClick = {
                this@ExchangeConnectionSheet.dismiss()
                onDismissClick()
            }
        }
    }

    companion object {
        private const val ARG_CONTENT = "arg_content"
        private const val ARG_TAGS = "arg_tags"

        private fun newInstance(
            content: Content,
            tags: List<TagViewState> = emptyList()
        ): ExchangeConnectionSheet {
            return ExchangeConnectionSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TAGS, tags as Serializable)
                    putParcelable(ARG_CONTENT, content)
                }
            }
        }

        fun newInstance(
            content: Content,
            tags: List<TagViewState>,
            primaryCtaClick: () -> Unit = {},
            secondaryCtaClick: () -> Unit = {}
        ): ExchangeConnectionSheet =
            newInstance(content, tags).apply {
                onCtaClick = { primaryCtaClick() }
                onDismissClick = { secondaryCtaClick() }
            }
    }
}
