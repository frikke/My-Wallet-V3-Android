package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.koin.androidx.compose.get
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.CustodialBalanceClicked
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.isInterestAccount
import piuk.blockchain.android.ui.coinview.domain.model.isPrivateKeyAccount
import piuk.blockchain.android.ui.coinview.domain.model.isStakingAccount
import piuk.blockchain.android.ui.coinview.domain.model.isTradingAccount
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState
import piuk.blockchain.android.ui.coinview.presentation.LogoSource
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewAnalytics

@Composable
fun AssetAccounts(
    analytics: Analytics = get(),
    data: CoinviewAccountsState,
    assetTicker: String,
    onAccountClick: (CoinviewAccount) -> Unit,
    onLockedAccountClick: () -> Unit
) {
    when (data) {
        CoinviewAccountsState.NotSupported -> {
            Empty()
        }

        CoinviewAccountsState.Loading -> {
            AssetAccountsLoading()
        }

        CoinviewAccountsState.Error -> {
            AssetAccountsError()
        }

        is CoinviewAccountsState.Data -> {
            AssetAccountsData(
                analytics = analytics,
                data = data,
                assetTicker = assetTicker,
                onAccountClick = onAccountClick,
                onLockedAccountClick = onLockedAccountClick
            )
        }
    }
}

@Composable
fun AssetAccountsLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerLoadingTableRow()
    }
}

@Composable
fun AssetAccountsError() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.dimensions.standardSpacing
            )
    ) {
        CardAlert(
            title = stringResource(R.string.coinview_account_load_error_title),
            subtitle = stringResource(R.string.coinview_account_load_error_subtitle),
            alertType = AlertType.Warning,
            isBordered = true,
            isDismissable = false
        )
    }
}

@Composable
fun AssetAccountsData(
    analytics: Analytics = get(),
    assetTicker: String,
    data: CoinviewAccountsState.Data,
    onAccountClick: (CoinviewAccount) -> Unit,
    onLockedAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing)
            .background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
    ) {
        // accounts
        data.accounts.forEachIndexed { index, account ->
            when (account) {
                is CoinviewAccountState.Available -> {
                    BalanceTableRow(
                        titleStart = buildAnnotatedString { append(account.title) },
                        bodyStart = buildAnnotatedString { append(account.subtitle.value()) },
                        titleEnd = buildAnnotatedString { append(account.fiatBalance) },
                        bodyEnd = buildAnnotatedString { append(account.cryptoBalance) },
                        startImageResource = when (account.logo) {
                            is LogoSource.Remote -> {
                                ImageResource.Remote(url = account.logo.value, shape = CircleShape)
                            }
                            is LogoSource.Resource -> {
                                ImageResource.Local(
                                    id = account.logo.value,
                                    colorFilter = ColorFilter.tint(
                                        Color(android.graphics.Color.parseColor(account.assetColor))
                                    ),
                                    shape = CircleShape
                                )
                            }
                        },
                        tags = emptyList(),
                        backgroundColor = Color.Transparent,
                        onClick = {
                            account.cvAccount.account.let { account ->
                                if (account is CryptoAccount && account is TradingAccount) {
                                    analytics.logEvent(CustodialBalanceClicked(account.currency))
                                }
                            }

                            analytics.logEvent(
                                CoinViewAnalytics.WalletsAccountsClicked(
                                    origin = LaunchOrigin.COIN_VIEW,
                                    currency = assetTicker,
                                    accountType = account.cvAccount.toAccountType()
                                )
                            )

                            onAccountClick(account.cvAccount)
                        }
                    )
                }
                is CoinviewAccountState.Unavailable -> {
                    DefaultTableRow(
                        primaryText = account.title,
                        secondaryText = account.subtitle.value(),
                        startImageResource = when (account.logo) {
                            is LogoSource.Remote -> {
                                ImageResource.Remote(url = account.logo.value, shape = CircleShape)
                            }
                            is LogoSource.Resource -> {
                                ImageResource.Local(
                                    id = account.logo.value,
                                    colorFilter = ColorFilter.tint(Grey400),
                                    shape = CircleShape
                                )
                            }
                        },
                        endImageResource = ImageResource.Local(
                            R.drawable.ic_lock, colorFilter = ColorFilter.tint(Grey400)
                        ),
                        backgroundColor = Color.Transparent,
                        onClick = { onLockedAccountClick() }
                    )
                }
            }

            if (index < data.accounts.lastIndex) {
                Divider(color = Color(0XFFF1F2F7))
            }
        }
    }
}

private fun CoinviewAccount.toAccountType() = when {
    isTradingAccount() -> CoinViewAnalytics.Companion.AccountType.CUSTODIAL
    isInterestAccount() -> CoinViewAnalytics.Companion.AccountType.REWARDS_ACCOUNT
    isStakingAccount() -> CoinViewAnalytics.Companion.AccountType.STAKING_ACCOUNT
    isPrivateKeyAccount() -> CoinViewAnalytics.Companion.AccountType.USERKEY
    else -> CoinViewAnalytics.Companion.AccountType.EXCHANGE_ACCOUNT
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Loading() {
    AssetAccounts(
        previewAnalytics,
        CoinviewAccountsState.Loading,
        assetTicker = "ETH",
        {}, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Error() {
    AssetAccounts(
        previewAnalytics,
        CoinviewAccountsState.Error,
        assetTicker = "ETH",
        {}, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Data() {
    AssetAccounts(
        previewAnalytics,
        CoinviewAccountsState.Data(
            accounts = listOf(
                CoinviewAccountState.Available(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 1",
                    subtitle = TextValue.StringValue("ETH"),
                    cryptoBalance = "0.90349281 ETH",
                    fiatBalance = "$2,000.00",
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
                    assetColor = "#324921"
                ),
                CoinviewAccountState.Available(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 2",
                    subtitle = TextValue.StringValue("ETH"),
                    cryptoBalance = "0.90349281 ETH",
                    fiatBalance = "$2,000.00",
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
                    assetColor = "#324921"
                ),
                CoinviewAccountState.Unavailable(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 2",
                    subtitle = TextValue.StringValue("ETH"),
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator)
                )
            )
        ),
        assetTicker = "ETH",
        {}, {}
    )
}

private val previewBlockchainAccount = object : BlockchainAccount {
    override val label: String
        get() = error("preview")
    override val balanceRx: Observable<AccountBalance>
        get() = error("preview")
    override val activity: Single<ActivitySummaryList>
        get() = error("preview")
    override val isFunded: Boolean
        get() = error("preview")
    override val hasTransactions: Boolean
        get() = error("preview")
    override val receiveAddress: Single<ReceiveAddress>
        get() = error("preview")
    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = error("preview")
}

val previewCvAccount: CoinviewAccount = CoinviewAccount.PrivateKey(
    account = previewBlockchainAccount,
    cryptoBalance = DataResource.Data(Money.zero(CryptoCurrency.BTC)),
    fiatBalance = DataResource.Data(Money.zero(CryptoCurrency.BTC)),
    isEnabled = false
)
