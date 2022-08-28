package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountsHeaderState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsStyle
import piuk.blockchain.android.ui.coinview.presentation.LogoSource
import piuk.blockchain.android.ui.coinview.presentation.SimpleValue

@Composable
fun AssetAccounts(
    data: CoinviewAccountsState,
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
                data = data,
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
                horizontal = AppTheme.dimensions.paddingLarge,
                vertical = AppTheme.dimensions.paddingMedium
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
    data: CoinviewAccountsState.Data,
    onAccountClick: (CoinviewAccount) -> Unit,
    onLockedAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .applyStyle(data.style)
    ) {
        // header
        AssetAccountHeader(header = data.header)

        // accounts
        data.accounts.forEach { account ->
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
                        onClick = { onAccountClick(account.cvAccount) }
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
                        onClick = { onLockedAccountClick() }
                    )
                }
            }

            Separator()
        }
    }
}

@Composable
fun AssetAccountHeader(header: CoinviewAccountsHeaderState) {
    when (header) {
        is CoinviewAccountsHeaderState.ShowHeader -> {
            SmallSectionHeader(
                modifier = Modifier.fillMaxWidth(),
                text = header.text.value()
            )
        }

        CoinviewAccountsHeaderState.NoHeader -> {
            Empty()
        }
    }
}

@Composable
fun Separator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.xxxPaddingSmall)
            .background(AppTheme.colors.medium)
    )
}

@Composable
private fun Modifier.applyStyle(style: CoinviewAccountsStyle): Modifier {
    return when (style) {
        CoinviewAccountsStyle.Boxed -> {
            run {
                padding(AppTheme.dimensions.paddingMedium)
            }.run {
                border(
                    width = AppTheme.dimensions.xxxPaddingSmall,
                    color = AppTheme.colors.medium,
                    shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
                )
            }.run {
                background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
            }
        }
        CoinviewAccountsStyle.Simple -> this
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Loading() {
    AssetAccounts(
        CoinviewAccountsState.Loading,
        {}, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Error() {
    AssetAccounts(
        CoinviewAccountsState.Error,
        {}, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Data_Simple() {
    AssetAccounts(
        CoinviewAccountsState.Data(
            style = CoinviewAccountsStyle.Simple,
            header = CoinviewAccountsHeaderState.ShowHeader(SimpleValue.StringValue("wallet & accounts")),
            accounts = listOf(
                CoinviewAccountState.Available(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 1",
                    subtitle = SimpleValue.StringValue("ETH"),
                    cryptoBalance = "0.90349281 ETH",
                    fiatBalance = "$2,000.00",
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
                    assetColor = "#324921"
                ),
                CoinviewAccountState.Available(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 2",
                    subtitle = SimpleValue.StringValue("ETH"),
                    cryptoBalance = "0.90349281 ETH",
                    fiatBalance = "$2,000.00",
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
                    assetColor = "#324921"
                ),
                CoinviewAccountState.Unavailable(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 2",
                    subtitle = SimpleValue.StringValue("ETH"),
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator)
                )
            )
        ),
        {}, {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAssetAccounts_Data_Boxed() {
    AssetAccounts(
        CoinviewAccountsState.Data(
            style = CoinviewAccountsStyle.Boxed,
            header = CoinviewAccountsHeaderState.NoHeader,
            accounts = listOf(
                CoinviewAccountState.Available(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 1",
                    subtitle = SimpleValue.StringValue("ETH"),
                    cryptoBalance = "0.90349281 ETH",
                    fiatBalance = "$2,000.00",
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
                    assetColor = "#324921"
                ),
                CoinviewAccountState.Available(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 2",
                    subtitle = SimpleValue.StringValue("ETH"),
                    cryptoBalance = "0.90349281 ETH",
                    fiatBalance = "$2,000.00",
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator),
                    assetColor = "#324921"
                ),
                CoinviewAccountState.Unavailable(
                    cvAccount = previewCvAccount,
                    title = "Ethereum 2",
                    subtitle = SimpleValue.StringValue("ETH"),
                    logo = LogoSource.Resource(R.drawable.ic_interest_account_indicator)
                )
            )
        ),
        {}, {}
    )
}

private val previewBlockchainAccount = object : BlockchainAccount {
    override val label: String
        get() = TODO("Not yet implemented")
    override val balance: Observable<AccountBalance>
        get() = TODO("Not yet implemented")
    override val activity: Single<ActivitySummaryList>
        get() = TODO("Not yet implemented")
    override val isFunded: Boolean
        get() = TODO("Not yet implemented")
    override val hasTransactions: Boolean
        get() = TODO("Not yet implemented")
    override val receiveAddress: Single<ReceiveAddress>
        get() = TODO("Not yet implemented")
    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = TODO("Not yet implemented")
}

val previewCvAccount: CoinviewAccount = CoinviewAccount.Defi(
    account = previewBlockchainAccount,
    cryptoBalance = Money.zero(CryptoCurrency.BTC),
    fiatBalance = Money.zero(CryptoCurrency.BTC),
    isEnabled = false
)
