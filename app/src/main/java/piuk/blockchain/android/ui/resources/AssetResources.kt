package piuk.blockchain.android.ui.resources

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.circleCrop
import com.bumptech.glide.request.RequestOptions
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import piuk.blockchain.android.R

interface AssetResources {

    @ColorInt
    fun assetColor(asset: Currency): Int

    fun loadAssetIcon(imageView: ImageView, asset: Currency)

    fun makeBlockExplorerUrl(asset: AssetInfo, transactionHash: String): String
}

internal class AssetResourcesImpl(val resources: Resources) : AssetResources {

    override fun assetColor(asset: Currency): Int =
        Color.parseColor(asset.colour)

    @SuppressLint("CheckResult")
    override fun loadAssetIcon(imageView: ImageView, asset: Currency) {
        Glide.with(imageView.context)
            .load(asset.logo)
            .apply(RequestOptions().placeholder(R.drawable.ic_default_asset_logo))
            .apply {
                if (asset.type == CurrencyType.CRYPTO) {
                    circleCrop()
                }
            }
            .into(imageView)
    }

    override fun makeBlockExplorerUrl(asset: AssetInfo, transactionHash: String): String =
        asset.txExplorerUrlBase?.let {
            "$it$transactionHash"
        } ?: ""
}
