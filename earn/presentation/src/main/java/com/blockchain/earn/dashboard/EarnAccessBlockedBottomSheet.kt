package com.blockchain.earn.dashboard

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.earn.R

class EarnAccessBlockedBottomSheet : ComposeModalBottomDialog() {

    private val title by lazy {
        arguments?.getString(TITLE).orEmpty()
    }

    private val paragraph by lazy {
        arguments?.getString(PARAGRAPH).orEmpty()
    }

    @Composable
    override fun Sheet() {
        BottomSheetTwoButtons(
            onCloseClick = { dismiss() },
            title = title,
            headerImageResource = ImageResource.Local(R.drawable.ic_region),
            subtitleAlign = TextAlign.Center,
            showTitleInHeader = false,
            subtitle = paragraph,
            button1 = BottomSheetButton(
                type = ButtonType.SMALL_MINIMAL,
                onClick = {
                    context?.openUrl(LEARN_MORE_URL)
                },
                text = getString(com.blockchain.stringResources.R.string.common_learn_more)
            ),
            button2 = BottomSheetButton(
                type = ButtonType.PRIMARY,
                onClick = {
                    dismiss()
                },
                text = getString(com.blockchain.stringResources.R.string.common_ok)
            )
        )
    }

    companion object {
        private const val TITLE = "TITLE"
        private const val PARAGRAPH = "PARAGRAPH"
        private const val LEARN_MORE_URL = "https://support.blockchain.com/hc/en-us/articles/360018751932"

        fun newInstance(title: String, paragraph: String) = EarnAccessBlockedBottomSheet().apply {
            arguments = Bundle().apply {
                putString(TITLE, title)
                putString(PARAGRAPH, paragraph)
            }
        }
    }
}
