package com.blockchain.home.presentation.dashboard.composable

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.AlertOn
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.stringResources.R

internal fun LazyListScope.kycRejected(
    onClick: () -> Unit
) {
    paddedItem(
        paddingValues = {
            PaddingValues(AppTheme.dimensions.smallSpacing)
        }
    ) {
        KycRejectedCard(
            onClick = onClick
        )
    }
}

@Composable
private fun KycRejectedCard(
    onClick: () -> Unit
) {
    Surface(
        color = AppColors.backgroundSecondary,
        shape = AppTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = AppTheme.dimensions.standardSpacing,
                    horizontal = AppTheme.dimensions.smallSpacing
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallTagIcon(
                icon = StackedIcon.SmallTag(
                    main = Icons.Filled.User
                        .withTint(AppColors.title)
                        .withBackground(backgroundColor = AppColors.light, iconSize = 58.dp, backgroundSize = 88.dp),
                    tag = Icons.AlertOn
                        .withTint(AppColors.warning),
                ),
                iconBackground = AppColors.backgroundSecondary,
                mainIconSize = 88.dp,
                tagIconSize = 44.dp,
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Text(
                text = stringResource(R.string.dashboard_kyc_blocked_title),
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Text(
                text = stringResource(R.string.dashboard_kyc_blocked_description),
                style = AppTheme.typography.body1,
                color = AppTheme.colors.body,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(min = 56.dp),
                text = stringResource(R.string.go_to_defi),
                onClick = onClick
            )
        }
    }
}

@Preview
@Composable
private fun PreviewKycRejectedCard() {
    KycRejectedCard(onClick = {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewKycRejectedCardDark() {
    PreviewKycRejectedCard()
}
