package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

class LoadAssetInfoUseCase(
    private val assetService: AssetService
) {
    operator fun invoke(asset: AssetInfo): Flow<DataResource<DetailedAssetInformation>> {
        return assetService.getAssetInformation(asset)
    }
}
