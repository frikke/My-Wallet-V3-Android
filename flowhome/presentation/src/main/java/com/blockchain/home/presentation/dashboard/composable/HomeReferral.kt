package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.CustomBackgroundCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.referral.ReferralIntent
import com.blockchain.home.presentation.referral.ReferralViewModel
import com.blockchain.home.presentation.referral.ReferralViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun Referral(
    viewModel: ReferralViewModel = getViewModel(scope = payloadScope),
    forceRefresh: Boolean,
    openReferral: () -> Unit
) {
    val viewState: ReferralViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ReferralIntent.LoadData())
        onDispose { }
    }

    DisposableEffect(forceRefresh) {
        if (forceRefresh) {
            viewModel.onIntent(ReferralIntent.Refresh)
        }
        onDispose { }
    }

    ReferralScreen(
        referralPrompt = viewState.referralInfo,
        openReferral = openReferral
    )
}

@Composable
fun ReferralScreen(
    referralPrompt: DataResource<ReferralInfo>,
    openReferral: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ((referralPrompt as? DataResource.Data)?.data as? ReferralInfo.Data)?.let { referralData ->
            referralData.announcementInfo?.let { announcementInfo ->
                CustomBackgroundCard(
                    isCloseable = false,
                    title = announcementInfo.title,
                    subtitle = announcementInfo.message,
                    backgroundResource = if (announcementInfo.backgroundUrl.isNotEmpty()) {
                        ImageResource.Remote(announcementInfo.backgroundUrl)
                    } else {
                        ImageResource.None
                    },
                    iconResource = if (announcementInfo.iconUrl.isNotEmpty()) {
                        ImageResource.Remote(announcementInfo.iconUrl)
                    } else {
                        ImageResource.None
                    },
                    onClick = openReferral
                )
            } ?: CustomBackgroundCard(
                title = stringResource(id = R.string.referral_program),
                subtitle = referralData.rewardSubtitle,
                backgroundResource = ImageResource.Local(R.drawable.bkgd_button_blue),
                onClick = openReferral
            )
        }
    }
}

@Preview
@Composable
fun PreviewReferralScreen() {
    ReferralScreen(
        DataResource.Loading,
        {}
    )
}
