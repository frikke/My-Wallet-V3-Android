package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Viewfinder
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.WalletBalance
import com.blockchain.koin.payloadScope
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import org.koin.androidx.compose.getViewModel

@Composable
fun MenuOptions(
    modifier: Modifier = Modifier,
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope),
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    showBackground: Boolean,
    showBalance: Boolean
) {
    val viewState: AssetsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    MenuOptionsScreen(
        modifier = modifier,
        walletBalance = viewState.balance,
        openSettings = openSettings,
        launchQrScanner = launchQrScanner,
        balanceVisible = showBackground,
        showBalance = showBalance
    )
}

@Composable
fun MenuOptionsScreen(
    modifier: Modifier = Modifier,
    walletBalance: WalletBalance,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    balanceVisible: Boolean,
    showBalance: Boolean
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // to hide what is scrolled passed this view
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.xHugeSpacing)
                .background(
                    AppTheme.colors.backgroundMuted.copy(alpha = 0.9F),
                    AppTheme.shapes.veryLarge
                )
        )

        Box(
            modifier = modifier
                .padding(AppTheme.dimensions.smallestSpacing)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                modifier = Modifier.matchParentSize(),
                visible = balanceVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    modifier = Modifier
                        .padding(AppTheme.dimensions.smallestSpacing)
                        .matchParentSize()
                        .background(AppTheme.colors.background, AppTheme.shapes.large)
                        .clickable { },
                    shape = AppTheme.shapes.large,
                    elevation = 3.dp
                ) { }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.tinySpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    imageResource = ImageResource.Local(R.drawable.ic_user_settings),
                    modifier = Modifier
                        .padding(AppTheme.dimensions.tinySpacing)
                        .clickable {
                            openSettings()
                        }
                )

                Spacer(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = showBalance,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TotalBalanceSmall(balance = walletBalance.balance)
                }

                Spacer(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Image(
                    imageResource = Icons.Viewfinder,
                    modifier = Modifier
                        .padding(AppTheme.dimensions.tinySpacing)
                        .clickable {
                            launchQrScanner()
                        }
                )
            }
        }
    }
}

@Composable
fun TotalBalanceSmall(
    balance: DataResource<Money>
) {
    Text(
        text = (balance as? DataResource.Data)?.data?.toStringWithSymbol() ?: "",
        style = AppTheme.typography.title3,
        color = AppTheme.colors.title
    )
}

@Preview(showBackground = true, backgroundColor = 0XFF1234F2)
@Composable
fun PreviewMenuOptionsScreen() {
    MenuOptionsScreen(
        walletBalance = WalletBalance(
            balance = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
            cryptoBalanceDifference24h = DataResource.Data(
                Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())
            ),
            cryptoBalanceNow = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
        ),
        openSettings = {}, launchQrScanner = {},
        balanceVisible = true,
        showBalance = true
    )
}
