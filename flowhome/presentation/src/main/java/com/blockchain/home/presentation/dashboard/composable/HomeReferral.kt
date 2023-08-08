package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.CustomBackgroundCard
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.home.presentation.R

fun LazyListScope.homeReferral(
    referralData: ReferralInfo.Data,
    openReferral: () -> Unit
) {
    paddedItem(
        paddingValues = {
            PaddingValues(AppTheme.dimensions.smallSpacing)
        }
    ) {
        Referral(
            referralData = referralData,
            openReferral = openReferral
        )
    }
}

@Composable
private fun Referral(
    referralData: ReferralInfo.Data,
    openReferral: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            title = stringResource(id = com.blockchain.stringResources.R.string.referral_program),
            subtitle = referralData.rewardSubtitle,
            onClick = openReferral
        )
    }
}
