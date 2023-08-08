package piuk.blockchain.android.ui.resources

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.fiat.FiatAccountGroup
import com.blockchain.coincore.impl.AllCustodialWalletsAccount
import com.blockchain.coincore.impl.AllNonCustodialWalletsAccount
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CryptoAccountCustodialSingleGroup
import com.blockchain.coincore.impl.CryptoAccountNonCustodialGroup
import com.blockchain.coincore.impl.CryptoExchangeAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import info.blockchain.balance.Currency
import piuk.blockchain.android.R

class AccountIcon(
    private val account: BlockchainAccount,
    private val assetResources: AssetResources
) {
    fun loadAssetIcon(imageView: ImageView) {
        val currency = currencyForIcon
        if (currency != null) {
            assetResources.loadAssetIcon(imageView, currency)
        } else {
            val icon = standardIcon ?: throw IllegalStateException("$account is not supported")
            imageView.setImageResource(icon)
        }
    }

    private val standardIcon: Int?
        @DrawableRes get() = when (account) {
            is CryptoAccount -> null
            is FiatAccount -> null
            is AccountGroup -> accountGroupIcon(account)
            else -> throw IllegalStateException("$account is not supported")
        }

    private val currencyForIcon: Currency?
        get() = when (account) {
            is SingleAccount -> account.currency
            is AccountGroup -> accountGroupTicker(account)
            else -> throw IllegalStateException("$account is not supported")
        }

    val indicator: Int?
        @DrawableRes get() = when (account) {
            is CryptoNonCustodialAccount -> R.drawable.ic_non_custodial_account_indicator
            is FiatAccount -> null
            is TradingAccount -> R.drawable.ic_custodial_account_indicator
            is EarnRewardsAccount.Interest -> R.drawable.ic_interest_account_indicator
            is EarnRewardsAccount.Staking -> R.drawable.ic_staking_account_indicator
            is EarnRewardsAccount.Active -> R.drawable.ic_staking_account_indicator // TODO(EARN): icon
            is CryptoExchangeAccount -> R.drawable.ic_exchange_indicator
            else -> null
        }

    private fun accountGroupIcon(account: AccountGroup): Int? {
        return when (account) {
            is AllWalletsAccount -> R.drawable.ic_all_wallets_white
            is AllCustodialWalletsAccount -> com.blockchain.componentlib.R.drawable.ic_portfolio
            is AllNonCustodialWalletsAccount -> com.blockchain.componentlib.R.drawable.ic_defi_wallet
            is CryptoAccountCustodialSingleGroup -> null
            is CryptoAccountNonCustodialGroup -> null
            is FiatAccountGroup -> null
            else -> throw IllegalArgumentException("$account is not a valid group")
        }
    }

    companion object {
        private fun accountGroupTicker(account: AccountGroup): Currency? {
            return when (account) {
                is AllWalletsAccount,
                is AllNonCustodialWalletsAccount,
                is AllCustodialWalletsAccount -> null
                is FiatAccount,
                is CryptoAccountCustodialSingleGroup -> account.accounts[0].currency
                is CryptoAccountNonCustodialGroup -> account.asset
                else -> throw IllegalArgumentException("$account is not a valid group")
            }
        }
    }
}
