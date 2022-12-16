package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewWalletsDefiBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem

class DefiAccountDetailsDelegate(
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onLockedAccountSelected: () -> Unit,
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.DefiDetailsInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        DefiWalletViewHolder(
            ViewCoinviewWalletsDefiBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAccountSelected = onAccountSelected,
            onLockedAccountSelected = onLockedAccountSelected
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as DefiWalletViewHolder).bind(
        item = items[position] as AssetDetailsItem.DefiDetailsInfo,
    )
}

private class DefiWalletViewHolder(
    private val binding: ViewCoinviewWalletsDefiBinding,
    private val onAccountSelected: (AssetDetailsItem.CryptoDetailsInfo) -> Unit,
    private val onLockedAccountSelected: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AssetDetailsItem.DefiDetailsInfo) {
        with(binding) {
            assetDetailsAvailable.onClick = {
                onAccountSelected(item)
            }
            assetDetailsNotAvailable.onClick = {
                onLockedAccountSelected()
            }
            val account = when (item.account) {
                is CryptoAccount -> item.account
                is AccountGroup -> item.account.selectFirstAccount()
                else -> throw IllegalStateException(
                    "Unsupported account type for asset details ${item.account}"
                )
            }

            if (item.actions.any { it.state == ActionState.Available }) {
                assetDetailsNotAvailable.gone()
                assetDetailsAvailable.apply {
                    visible()
                    titleStart = buildAnnotatedString { append(item.account.label) }
                    titleEnd = buildAnnotatedString { append(item.fiatBalance.toStringWithSymbol()) }
                    bodyEnd = buildAnnotatedString { append(item.balance.toStringWithSymbol()) }
                    bodyStart = buildAnnotatedString { append(account.currency.displayTicker) }
                    startImageResource = ImageResource.Remote(url = account.currency.logo, shape = CircleShape)
                }
            } else {
                assetDetailsAvailable.gone()
                assetDetailsNotAvailable.apply {
                    visible()
                    primaryText = account.currency.name
                    secondaryText = context.getString(R.string.coinview_nc_desc)
                    startImageResource = ImageResource.Remote(url = account.currency.logo, shape = CircleShape)
                    endImageResource = ImageResource.LocalWithBackground(
                        R.drawable.ic_lock, Grey400, White, 1F
                    )
                }
            }
        }
    }
}
