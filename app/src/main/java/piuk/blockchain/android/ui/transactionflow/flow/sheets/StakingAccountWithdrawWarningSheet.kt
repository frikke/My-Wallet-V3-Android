package piuk.blockchain.android.ui.transactionflow.flow.sheets

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.PagerIndicatorDots
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import piuk.blockchain.android.R

class StakingAccountWithdrawWarning : ComposeModalBottomDialog() {

    override val host: Host by lazy {
        activity as? Host ?: parentFragment as? Host ?: throw IllegalStateException(
            "Host is not a StakingAccountWithdrawWarning.Host"
        )
    }

    override val makeSheetNonCollapsible: Boolean
        get() = true

    private val assetIconUrl: String? by lazy {
        arguments?.getString(ASSET_ICON_URL)
    }

    private val unbondingDays: Int? by lazy {
        arguments?.getInt(UNBONDING_DAYS)
    }

    interface Host : HostedBottomSheet.Host {
        fun learnMoreClicked()
        fun onNextClicked()
        fun onClose()
    }

    @Composable
    override fun Sheet() {
        StakingAccountInfo(
            dismiss = ::dismiss,
            onClose = { host.onClose() },
            onLearnMoreClicked = host::learnMoreClicked,
            onNext = host::onNextClicked,
            assetIcon = assetIconUrl?.let {
                ImageResource.Remote(it)
            } ?: ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_blockchain),
            accountTypeIcon = ImageResource.Local(R.drawable.ic_staking_explainer),
            unbondingDays = unbondingDays ?: 42
        )
    }

    companion object {
        private const val ASSET_ICON_URL = "ASSET_ICON_URL"
        private const val UNBONDING_DAYS = "UNBONDING_DAYS"
        fun newInstance(assetIconUrl: String?, unbondingDays: Int) =
            StakingAccountWithdrawWarning().apply {
                arguments = Bundle().apply {
                    putString(ASSET_ICON_URL, assetIconUrl)
                    putInt(UNBONDING_DAYS, unbondingDays)
                }
            }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun StakingAccountInfo(
    dismiss: () -> Unit,
    onClose: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onNext: () -> Unit,
    assetIcon: ImageResource,
    accountTypeIcon: ImageResource,
    unbondingDays: Int
) {
    val items =
        listOf(
            InfoItem(
                stringResource(
                    id = com.blockchain.stringResources.R.string.staking_cannot_withdraw_paragraph,
                    unbondingDays
                )
            )
        )
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val scroll = rememberScrollState(0)

    LaunchedEffect(key1 = "autoscroll", block = {
        while (pagerState.currentPage < pagerState.pageCount - 1) {
            delay(10000)
            pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
        }
        cancel()
    })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(AppColors.background)
    ) {
        SheetHeader(
            onClosePress = {
                onClose()
                dismiss()
            },
            title = stringResource(id = com.blockchain.stringResources.R.string.default_label_staking_wallet),
        )

        Spacer(modifier = Modifier.size(dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing)))

        Box(
            modifier = Modifier.size(102.dp)
        ) {
            Image(
                imageResource = assetIcon,
                modifier = Modifier
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.epic_spacing))
                    .clip(CircleShape)
                    .background(AppTheme.colors.backgroundSecondary)
                    .align(Alignment.Center)
            )

            Image(
                imageResource = accountTypeIcon,
                modifier = Modifier
                    .size(dimensionResource(com.blockchain.componentlib.R.dimen.huge_spacing))
                    .clip(CircleShape)
                    .background(AppTheme.colors.backgroundSecondary)
                    .border(
                        dimensionResource(com.blockchain.componentlib.R.dimen.borderRadiiSmallest),
                        color = AppTheme.colors.backgroundSecondary,
                        shape = CircleShape
                    )
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.size(dimensionResource(id = com.blockchain.componentlib.R.dimen.standard_spacing)))

        SimpleText(
            text = stringResource(id = com.blockchain.stringResources.R.string.staking_cannot_withdraw_title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre,
            modifier = Modifier.padding(
                start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                bottom = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
            )
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            HorizontalPager(
                count = items.size,
                state = pagerState
            ) { currentPage ->
                SimpleText(
                    text = items[currentPage].paragraph,
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(
                            start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                            end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                            bottom = dimensionResource(com.blockchain.componentlib.R.dimen.xlarge_spacing)
                        )
                )
            }

            PagerIndicatorDots(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)),
                selectedIndex = pagerState.currentPage,
                count = items.size
            )
        }

        MinimalPrimarySmallButton(
            text = stringResource(com.blockchain.stringResources.R.string.common_learn_more),
            onClick = onLearnMoreClicked
        )

        Spacer(modifier = Modifier.size(dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing)))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)),
            text = if (pagerState.currentPage == pagerState.pageCount - 1) {
                stringResource(id = com.blockchain.stringResources.R.string.common_i_understand)
            } else {
                stringResource(id = com.blockchain.stringResources.R.string.common_next)
            },
            onClick = {
                if (pagerState.currentPage == pagerState.pageCount - 1) {
                    onNext()
                    dismiss()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
                    }
                }
            }
        )
    }
}

private data class InfoItem(
    val paragraph: String
)

@Preview
@Composable
fun StakingInfo() {
    StakingAccountInfo(
        {},
        {},
        {},
        {},
        ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_blockchain),
        ImageResource.Local(R.drawable.ic_staking_explainer),
        42
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingInfoDark() {
    StakingInfo()
}
