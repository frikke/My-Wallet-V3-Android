package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import java.text.DecimalFormat
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewWalletsBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem
import piuk.blockchain.android.ui.resources.AccountIcon
import piuk.blockchain.android.ui.resources.AssetResources

class BrokerageAccountDetailsDelegate(
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onLockedAccountSelected: () -> Unit,
    private val labels: DefaultLabels,
    private val assetResources: AssetResources,
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.BrokerageDetailsInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssetWalletViewHolder(
            ViewCoinviewWalletsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAccountSelected = onAccountSelected,
            onLockedAccountSelected = onLockedAccountSelected,
            labels = labels,
            assetResources = assetResources,
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AssetWalletViewHolder).bind(
        item = items[position] as AssetDetailsItem.BrokerageDetailsInfo,
        isFirstItemOfCategory = items.indexOfFirst { it is AssetDetailsItem.BrokerageDetailsInfo } == position
    )
}

private class AssetWalletViewHolder(
    private val binding: ViewCoinviewWalletsBinding,
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onLockedAccountSelected: () -> Unit,
    private val labels: DefaultLabels,
    private val assetResources: AssetResources
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: AssetDetailsItem.BrokerageDetailsInfo,
        isFirstItemOfCategory: Boolean
    ) {
        val asset = getAsset(item.account, item.balance.currencyCode)

        with(binding) {
            assetDetailsAvailable.onClick = {
                onAccountSelected(item)
            }
            assetDetailsNotAvailable.onClick = {
                onLockedAccountSelected()
            }
            val walletLabel = when (item.assetFilter) {
                // todo (othman) should be removed once universal mode is removed
                AssetFilter.NonCustodial -> item.account.label
                AssetFilter.Trading -> labels.getDefaultTradingWalletLabel()
                AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
                else -> throw IllegalArgumentException("Filer not supported for account label")
            }
            val account = when (item.account) {
                is CryptoAccount -> item.account
                is AccountGroup -> item.account.selectFirstAccount()
                else -> throw IllegalStateException(
                    "Unsupported account type for asset details ${item.account}"
                )
            }
            val accountIcon = AccountIcon(account, assetResources)

            if (item.actions.any { it.state == ActionState.Available }) {
                assetDetailsNotAvailable.gone()
                assetDetailsAvailable.apply {
                    visible()
                    titleStart = buildAnnotatedString { append(walletLabel) }
                    titleEnd = buildAnnotatedString { append(item.fiatBalance.toStringWithSymbol()) }
                    bodyEnd = buildAnnotatedString { append(item.balance.toStringWithSymbol()) }
                    bodyStart = buildAnnotatedString {
                        append(
                            when (item.assetFilter) {
                                // todo (othman) should be removed once universal mode is removed
                                AssetFilter.NonCustodial -> {
                                    if (account is MultiChainAccount) {
                                        context.getString(
                                            R.string.coinview_multi_nc_desc,
                                            account.l1Network.name
                                        )
                                    } else {
                                        context.getString(R.string.coinview_nc_desc)
                                    }
                                }

                                AssetFilter.Trading -> context.getString(R.string.coinview_c_available_desc)
                                AssetFilter.Interest -> {
                                    val interestFormat = DecimalFormat("0.#")
                                    val interestRate = interestFormat.format(item.interestRate)
                                    binding.root.context.getString(
                                        R.string.coinview_interest_with_balance, interestRate
                                    )
                                }
                                else -> throw IllegalArgumentException("Not a supported filter")
                            }
                        )
                    }

                    accountIcon.indicator?.let {
                        startImageResource =
                            ImageResource.LocalWithBackgroundAndExternalResources(
                                it, asset.colour, "#FFFFFF", 1F
                            )
                    }
                }
            } else {
                assetDetailsAvailable.gone()
                assetDetailsNotAvailable.apply {
                    visible()
                    primaryText = walletLabel
                    secondaryText = when (item.assetFilter) {
                        AssetFilter.NonCustodial -> context.getString(R.string.coinview_nc_desc)
                        AssetFilter.Trading -> context.getString(R.string.coinview_c_unavailable_desc, asset.name)
                        AssetFilter.Interest -> {
                            val interestFormat = DecimalFormat("0.#")
                            val interestRate = interestFormat.format(item.interestRate)
                            context.getString(
                                R.string.coinview_interest_no_balance, interestRate
                            )
                        }
                        else -> throw IllegalArgumentException("Not a supported filter")
                    }
                    accountIcon.indicator?.let {
                        startImageResource =
                            ImageResource.LocalWithBackground(it, Grey400, White, 1F)
                    }
                    endImageResource =
                        ImageResource.LocalWithBackground(R.drawable.ic_lock, Grey400, White, 1F)
                }
            }

            walletsLabel.apply {
                visibleIf { isFirstItemOfCategory }
                title = context.getString(R.string.coinview_accounts_label)
            }
        }
    }

    private fun getAsset(account: BlockchainAccount, currency: String): Currency =
        when (account) {
            is CryptoAccount -> account.currency
            is AccountGroup -> account.accounts.filterIsInstance<CryptoAccount>()
                .firstOrNull()?.currency ?: throw IllegalStateException(
                "No crypto accounts found in ${this::class.java} with currency $currency "
            )
            else -> null
        } ?: throw IllegalStateException("Unsupported account type ${this::class.java}")
}
