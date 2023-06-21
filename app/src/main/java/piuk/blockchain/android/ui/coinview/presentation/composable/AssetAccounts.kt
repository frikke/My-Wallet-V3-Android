package piuk.blockchain.android.ui.coinview.presentation.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TxSourceState
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SmallInfoWithIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.ActionTableRow
import com.blockchain.componentlib.tablerow.BalanceFiatAndCryptoTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.LocalLogo
import com.blockchain.componentlib.utils.LogoValue
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.componentlib.utils.toImageResource
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.koin.androidx.compose.get
import piuk.blockchain.android.simplebuy.CustodialBalanceClicked
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.isInterestAccount
import piuk.blockchain.android.ui.coinview.domain.model.isPrivateKeyAccount
import piuk.blockchain.android.ui.coinview.domain.model.isStakingAccount
import piuk.blockchain.android.ui.coinview.domain.model.isTradingAccount
import piuk.blockchain.android.ui.coinview.presentation.CoinViewAnalytics
import piuk.blockchain.android.ui.coinview.presentation.CoinViewNetwork
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState

@Composable
fun AssetAccounts(
    analytics: Analytics = get(),
    data: DataResource<CoinviewAccountsState?>,
    l1Network: CoinViewNetwork?,
    assetTicker: String,
    enabled: Boolean,
    onAccountClick: (CoinviewAccount) -> Unit,
    onLockedAccountClick: () -> Unit
) {
    when (data) {
        DataResource.Loading -> {}

        is DataResource.Error -> {
            AssetAccountsError()
        }

        is DataResource.Data -> {
            AssetAccountsData(
                analytics = analytics,
                data = data,
                l1Network = l1Network,
                assetTicker = assetTicker,
                enabled= enabled,
                onAccountClick = onAccountClick,
                onLockedAccountClick = onLockedAccountClick
            )
        }
    }
}

@Composable
fun AssetAccountsError() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.backgroundSecondary, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
    ) {
        CardAlert(
            title = stringResource(com.blockchain.stringResources.R.string.coinview_account_load_error_title),
            subtitle = stringResource(com.blockchain.stringResources.R.string.coinview_account_load_error_subtitle),
            alertType = AlertType.Warning,
            backgroundColor = AppTheme.colors.backgroundSecondary,
            isBordered = false,
            isDismissable = false
        )
    }
}

@Composable
fun AssetAccountsData(
    analytics: Analytics = get(),
    assetTicker: String,
    l1Network: CoinViewNetwork?,
    data: DataResource.Data<CoinviewAccountsState?>,
    enabled: Boolean,
    onAccountClick: (CoinviewAccount) -> Unit,
    onLockedAccountClick: () -> Unit
) {
    data.data?.let {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // balance
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = stringResource(com.blockchain.stringResources.R.string.common_balance),
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.body
                )

                Text(
                    text = it.totalBalance,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Surface(
                color = AppColors.backgroundSecondary,
                shape = AppTheme.shapes.large
            ) {
                Column {
                    // accounts
                    it.accounts.forEachIndexed { index, account ->
                        when (account) {
                            is CoinviewAccountsState.CoinviewAccountState.Available -> {
                                BalanceFiatAndCryptoTableRow(
                                    title = account.title,
                                    subtitle = account.subtitle?.value(),
                                    valueFiat = account.fiatBalance,
                                    valueCrypto = account.cryptoBalance,
                                    icon = when (account.logo) {
                                        is LogoValue.Remote -> {
                                            StackedIcon.SingleIcon(
                                                ImageResource.Remote(url = account.logo.value, shape = CircleShape)
                                            )
                                        }

                                        is LogoValue.Local -> {
                                            StackedIcon.SingleIcon(
                                                account.logo.value.toImageResource()
                                                    .withTint(Color.White)
                                                    .withBackground(
                                                        backgroundSize = AppTheme.dimensions.standardSpacing,
                                                        backgroundColor = Color(
                                                            android.graphics.Color.parseColor(account.assetColor)
                                                        )
                                                    )
                                            )
                                        }
                                    },
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
                                    }.takeIf { enabled }
                                )
                            }

                            is CoinviewAccountsState.CoinviewAccountState.Unavailable -> {
                                ActionTableRow(
                                    title = account.title,
                                    subtitle = account.subtitle.value(),
                                    icon = when (account.logo) {
                                        is LogoValue.Remote -> {
                                            StackedIcon.SingleIcon(
                                                ImageResource.Remote(url = account.logo.value, shape = CircleShape)
                                            )
                                        }

                                        is LogoValue.Local -> {
                                            StackedIcon.SingleIcon(
                                                account.logo.value.toImageResource()
                                            )
                                        }
                                    },
                                    actionIcon = Icons.Filled.Lock,
                                    onClick = { onLockedAccountClick() }
                                )
                            }
                        }

                        if (index < it.accounts.lastIndex) {
                            AppDivider()
                        }
                    }
                }
            }

            // l1 netwrok
            l1Network?.let { l1Network ->
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                SmallInfoWithIcon(
                    iconUrl = l1Network.logo,
                    text = stringResource(
                        com.blockchain.stringResources.R.string.coinview_asset_l1,
                        it.assetName,
                        l1Network.name
                    )
                )
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

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewAssetAccounts_Error() {
    AssetAccounts(
        analytics = previewAnalytics,
        data = DataResource.Error(Exception()),
        l1Network = null,
        assetTicker = "ETH",
        enabled = true,
        onAccountClick = {},
        onLockedAccountClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF07080D)
@Composable
private fun PreviewAssetAccounts_ErrorDark() {
    PreviewAssetAccounts_Error()
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
fun PreviewAssetAccounts_Data() {
    AssetAccounts(
        analytics = previewAnalytics,
        data = DataResource.Data(
            CoinviewAccountsState(
                totalBalance = "$2,000.00",
                accounts = listOf(
                    CoinviewAccountsState.CoinviewAccountState.Available(
                        cvAccount = previewCvAccount,
                        title = "Ethereum 1",
                        subtitle = TextValue.StringValue("ETH"),
                        cryptoBalance = "0.90349281 ETH",
                        fiatBalance = "$2,000.00",
                        logo = LogoValue.Local(LocalLogo.Rewards),
                        assetColor = "#324921"
                    ),
                    CoinviewAccountsState.CoinviewAccountState.Available(
                        cvAccount = previewCvAccount,
                        title = "Ethereum 2",
                        subtitle = TextValue.StringValue("ETH"),
                        cryptoBalance = "0.90349281 ETH",
                        fiatBalance = "$2,000.00",
                        logo = LogoValue.Local(LocalLogo.Rewards),
                        assetColor = "#324921"
                    ),
                    CoinviewAccountsState.CoinviewAccountState.Unavailable(
                        cvAccount = previewCvAccount,
                        title = "Ethereum 2",
                        subtitle = TextValue.StringValue("ETH"),
                        logo = LogoValue.Local(LocalLogo.Rewards),
                    )
                ),
                assetName = "Ethereum"
            )
        ),
        l1Network = CoinViewNetwork("", "MATIC"),
        assetTicker = "ETH",
        enabled = true,
        onAccountClick = {},
        onLockedAccountClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0XFF07080D)
@Composable
private fun PreviewAssetAccounts_DataDark() {
    PreviewAssetAccounts_Data()
}

private val previewBlockchainAccount = object : SingleAccount {
    override val isDefault: Boolean
        get() = false
    override val currency: Currency
        get() = error("preview")
    override val sourceState: Single<TxSourceState>
        get() = error("preview")
    override val label: String
        get() = error("preview")

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> {
        error("preview")
    }

    override fun activity(freshnessStrategy: FreshnessStrategy): Observable<ActivitySummaryList> {
        error("preview")
    }

    override val receiveAddress: Single<ReceiveAddress>
        get() = error("preview")
    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = error("preview")

    override fun stateOfAction(assetAction: AssetAction): Single<ActionState> {
        error("preview")
    }
}

val previewCvAccount: CoinviewAccount = CoinviewAccount.PrivateKey(
    account = previewBlockchainAccount,
    cryptoBalance = DataResource.Data(Money.zero(CryptoCurrency.BTC)),
    fiatBalance = DataResource.Data(Money.zero(CryptoCurrency.BTC)),
    isEnabled = false
)
