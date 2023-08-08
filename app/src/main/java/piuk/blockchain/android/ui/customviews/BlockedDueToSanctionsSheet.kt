package piuk.blockchain.android.ui.customviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.alert.CustomEmptyState
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.nabu.BlockedReason
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU8

class BlockedDueToSanctionsSheet : ComposeModalBottomDialog() {

    private val descriptionText: String by lazy {
        arguments?.getString(ARG_DESCRIPTION)!!
    }

    private val actionUrl: String? by lazy {
        arguments?.getString(ARG_ACTION_URL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // This is because we need to show this as a regular fragment as well as a BottomSheet
        if (!showsDialog) {
            return ComposeView(requireContext()).apply {
                setContent {
                    CustomEmptyState(
                        title = com.blockchain.stringResources.R.string.account_restricted,
                        descriptionText = descriptionText,
                        icon = Icons.Filled.User,
                        ctaText = com.blockchain.stringResources.R.string.common_learn_more,
                        ctaAction = {
                            actionUrl?.let {
                                context.openUrl(it)
                            }
                        }
                    )
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @Composable
    override fun Sheet() {
        BottomSheetTwoButtons(
            title = getString(com.blockchain.stringResources.R.string.account_restricted),
            showTitleInHeader = true,
            subtitle = descriptionText,
            onCloseClick = { dismiss() },
            headerImageResource = null,
            button1 = BottomSheetButton(
                type = ButtonType.MINIMAL,
                text = stringResource(com.blockchain.stringResources.R.string.common_learn_more),
                onClick = {
                    actionUrl?.let {
                        context?.openUrl(it)
                    }
                }
            ),
            button2 = BottomSheetButton(
                type = ButtonType.PRIMARY,
                text = stringResource(com.blockchain.stringResources.R.string.common_i_understand),
                onClick = { dismiss() }
            )
        )
    }

    companion object {
        private const val ARG_DESCRIPTION = "ARG_DESCRIPTION"
        private const val ARG_ACTION_URL = "ARG_ACTION_URL"

        fun newInstance(reason: BlockedReason.Sanctions): BlockedDueToSanctionsSheet =
            BlockedDueToSanctionsSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DESCRIPTION, reason.message)
                    val actionUrl = when (reason) {
                        is BlockedReason.Sanctions.RussiaEU5 -> URL_RUSSIA_SANCTIONS_EU5
                        is BlockedReason.Sanctions.RussiaEU8 -> URL_RUSSIA_SANCTIONS_EU8
                        is BlockedReason.Sanctions.Unknown -> null
                    }
                    putString(ARG_ACTION_URL, actionUrl)
                }
            }
    }
}
