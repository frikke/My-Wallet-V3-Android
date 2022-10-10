package piuk.blockchain.android.ui.coinview.presentation.composable

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import piuk.blockchain.android.R

class StakingAccountSheet : ComposeModalBottomDialog() {

    override val host: Host by lazy {
        activity as? Host ?: parentFragment as? Host ?: throw IllegalStateException(
            "Host is not a StakingAccountSheet.Host"
        )
    }

    private val assetIconUrl: String? by lazy {
        arguments?.getString(ASSET_ICON_URL)
    }

    interface Host : HostedBottomSheet.Host {
        fun learnMoreClicked()
        fun goToWebAppClicked()
    }

    @Composable
    override fun Sheet() {
        StakingAccountInfo(
            onBackPressed = ::dismiss,
            onLearnMoreClicked = host::learnMoreClicked,
            onGoToWebClicked = host::goToWebAppClicked,
            assetIcon = assetIconUrl?.let { ImageResource.Remote(it) } ?: ImageResource.Local(R.drawable.ic_blockchain),
            accountTypeIcon = ImageResource.Local(R.drawable.ic_staking_explainer)
        )
    }

    companion object {
        private const val ASSET_ICON_URL = "ASSET_ICON_URL"
        fun newInstance(assetIconUrl: String?) =
            StakingAccountSheet().apply {
                arguments = Bundle().apply {
                    putString(ASSET_ICON_URL, assetIconUrl)
                }
            }
    }
}

@Composable
fun StakingAccountInfo(
    onBackPressed: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onGoToWebClicked: () -> Unit,
    assetIcon: ImageResource,
    accountTypeIcon: ImageResource
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(White)
    ) {
        SheetHeader(
            onClosePress = onBackPressed,
            title = stringResource(id = R.string.default_label_staking_wallet),
            shouldShowDivider = false
        )

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.xhuge_spacing)))

        Box(
            modifier = Modifier.size(102.dp)
        ) {
            Image(
                imageResource = assetIcon,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.epic_spacing))
                    .clip(CircleShape)
                    .background(AppTheme.colors.background)
                    .align(Alignment.Center),
            )

            Image(
                imageResource = accountTypeIcon,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.huge_spacing))
                    .clip(CircleShape)
                    .background(AppTheme.colors.background)
                    .border(2.dp, AppTheme.colors.background, shape = CircleShape)
                    .align(Alignment.TopEnd),
            )
        }

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.huge_spacing)))

        SimpleText(
            text = stringResource(id = R.string.staking_coming_soon_title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            text = stringResource(id = R.string.staking_coming_soon_subtitle),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.standard_spacing),
                end = dimensionResource(R.dimen.standard_spacing),
                bottom = dimensionResource(R.dimen.standard_spacing)
            )
        )

        SmallMinimalButton(text = stringResource(R.string.common_learn_more), onClick = onLearnMoreClicked)

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.xhuge_spacing)))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(R.dimen.standard_spacing)),
            text = stringResource(R.string.staking_coming_soon_cta),
            onClick = onGoToWebClicked
        )
    }
}

@Preview
@Composable
fun StakingInfo() {
    AppTheme {
        AppSurface {
            StakingAccountInfo(
                {},
                {},
                {},
                ImageResource.Local(R.drawable.ic_blockchain),
                ImageResource.Local(R.drawable.ic_staking_explainer)
            )
        }
    }
}
