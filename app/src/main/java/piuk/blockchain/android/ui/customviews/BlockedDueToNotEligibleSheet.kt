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
import com.blockchain.componentlib.alert.CustomEmptyState
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.nabu.BlockedReason
import piuk.blockchain.android.R
import piuk.blockchain.android.support.SupportCentreActivity

class BlockedDueToNotEligibleSheet : ComposeModalBottomDialog() {

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
                        title = com.blockchain.stringResources.R.string.account_restricted,
                        description = descriptionResId ?: com.blockchain.stringResources.R.string.feature_not_available,
                        descriptionText = descriptionText,
                        icon = Icons.Filled.User,
                        ctaText = com.blockchain.stringResources.R.string.contact_support,
                        ctaAction = { startActivity(SupportCentreActivity.newIntent(requireContext())) }
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
            subtitle = descriptionText ?: getString(descriptionResId!!),
            onCloseClick = { dismiss() },
            headerImageResource = null,
            button1 = BottomSheetButton(
                type = ButtonType.MINIMAL,
                text = stringResource(com.blockchain.stringResources.R.string.contact_support),
                onClick = { startActivity(SupportCentreActivity.newIntent(requireContext())) }
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
        private const val ARG_DESCRIPTION_TEXT = "ARG_DESCRIPTION_TEXT"

        fun newInstance(reason: BlockedReason.NotEligible): BlockedDueToNotEligibleSheet =
            BlockedDueToNotEligibleSheet().apply {
                arguments = Bundle().apply {
                    if (reason.message != null) {
                        putString(ARG_DESCRIPTION_TEXT, reason.message)
                    } else {
                        putInt(ARG_DESCRIPTION, com.blockchain.stringResources.R.string.feature_not_available)
                    }
                }
            }
    }
}
