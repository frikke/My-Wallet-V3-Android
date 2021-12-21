package piuk.blockchain.android.ui.resources

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.fiat.FiatAccountGroup
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CryptoAccountCustodialGroup
import com.blockchain.coincore.impl.CryptoAccountNonCustodialGroup
import com.blockchain.coincore.impl.CryptoExchangeAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R

class AccountIcon(
    private val account: BlockchainAccount,
    private val assetResources: AssetResources
) {
    fun loadAssetIcon(imageView: ImageView) {
        val assetTicker = assetForIcon
        if (assetTicker != null) {
            assetResources.loadAssetIcon(imageView, assetTicker)
        } else {
            val icon = standardIcon ?: throw IllegalStateException("$account is not supported")
            imageView.setImageResource(icon)
        }
    }

    private val standardIcon: Int?
        @DrawableRes get() = when (account) {
            is CryptoAccount -> null
            is FiatAccount -> assetResources.fiatCurrencyIcon(account.currency)
            is AccountGroup -> accountGroupIcon(account)
            else -> throw IllegalStateException("$account is not supported")
        }

    private val assetForIcon: AssetInfo?
        get() = when (account) {
            is CryptoAccount -> account.currency
            is FiatAccount -> null
            is AccountGroup -> accountGroupTicker(account)
            else -> throw IllegalStateException("$account is not supported")
        }

    val indicator: Int?
        @DrawableRes get() = when (account) {
            is CryptoNonCustodialAccount -> R.drawable.ic_non_custodial_account_indicator
            is FiatAccount -> null
            is InterestAccount -> R.drawable.ic_interest_account_indicator
            is TradingAccount -> R.drawable.ic_custodial_account_indicator
            is CryptoExchangeAccount -> R.drawable.ic_exchange_indicator
            else -> null
        }

    private fun accountGroupIcon(account: AccountGroup): Int? {
        return when (account) {
            is AllWalletsAccount -> R.drawable.ic_all_wallets_white
            is CryptoAccountCustodialGroup -> null
            is CryptoAccountNonCustodialGroup -> null
            is FiatAccountGroup -> (account.accounts.getOrNull(0) as? FiatAccount)?.let {
                assetResources.fiatCurrencyIcon(it.currency)
            } ?: DEFAULT_FIAT_ICON
            else -> throw IllegalArgumentException("$account is not a valid group")
        }
    }

    companion object {
        private const val DEFAULT_FIAT_ICON = R.drawable.ic_funds_usd

        private fun accountGroupTicker(account: AccountGroup): AssetInfo? {
            return when (account) {
                is AllWalletsAccount -> null
                is CryptoAccountCustodialGroup -> (account.accounts[0] as CryptoAccount).currency
                is CryptoAccountNonCustodialGroup -> account.asset
                is FiatAccountGroup -> null
                else -> throw IllegalArgumentException("$account is not a valid group")
            }
        }
    }
}
