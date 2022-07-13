package piuk.blockchain.android.ui.customviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.nabu.BlockedReason
import piuk.blockchain.android.R
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.util.openUrl

class BlockedDueToSanctionsSheet : ComposeModalBottomDialog() {

    @get:StringRes
    private val descriptionResId: Int? by lazy {
        arguments?.getInt(ARG_DESCRIPTION, 0)?.takeIf { it != 0 }
    }

    private val descriptionText: String? by lazy {
        arguments?.getString(ARG_DESCRIPTION_TEXT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // This is because we need to show this as a regular fragment as well as a BottomSheet
        if (!showsDialog) {
            return ComposeView(requireContext()).apply {
                setContent {
                    CustomEmptyState(
                        title = R.string.account_restricted,
                        description = descriptionResId ?: R.string.russia_sanctions_eu5_sheet_subtitle,
                        descriptionText = descriptionText,
                        icon = R.drawable.ic_wallet_intro_image,
                        ctaText = R.string.common_learn_more,
                        ctaAction = { requireContext().openUrl(URL_RUSSIA_SANCTIONS_EU5) }
                    )
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @Composable
    override fun Sheet() {
        BottomSheetTwoButtons(
            title = getString(R.string.account_restricted),
            showTitleInHeader = true,
            subtitle = descriptionText ?: getString(descriptionResId!!),
            shouldShowHeaderDivider = false,
            onCloseClick = { dismiss() },
            headerImageResource = null,
            button1 = BottomSheetButton(
                type = ButtonType.MINIMAL,
                text = stringResource(R.string.common_learn_more),
                onClick = { context.openUrl(URL_RUSSIA_SANCTIONS_EU5) }
            ),
            button2 = BottomSheetButton(
                type = ButtonType.PRIMARY,
                text = stringResource(R.string.common_i_understand),
                onClick = { dismiss() }
            )
        )
    }

    companion object {
        private const val ARG_DESCRIPTION = "ARG_DESCRIPTION"
        private const val ARG_DESCRIPTION_TEXT = "ARG_DESCRIPTION_TEXT"

        fun newInstance(reason: BlockedReason.Sanctions): BlockedDueToSanctionsSheet =
            BlockedDueToSanctionsSheet().apply {
                arguments = Bundle().apply {
                    when (reason) {
                        BlockedReason.Sanctions.RussiaEU5 ->
                            putInt(ARG_DESCRIPTION, R.string.russia_sanctions_eu5_sheet_subtitle)
                        is BlockedReason.Sanctions.Unknown -> putString(ARG_DESCRIPTION_TEXT, reason.message)
                    }
                }
            }
    }
}
