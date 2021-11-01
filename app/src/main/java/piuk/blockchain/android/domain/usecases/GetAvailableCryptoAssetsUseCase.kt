package piuk.blockchain.android.domain.usecases

import com.blockchain.coincore.Coincore
import com.blockchain.usecases.UseCase
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class GetAvailableCryptoAssetsUseCase(
    private val coincore: Coincore
) : UseCase<Unit, Single<List<AssetInfo>>>() {

    override fun execute(parameter: Unit): Single<List<AssetInfo>> =
        Single.fromCallable {
            coincore.activeCryptoAssets().map { cryptoAsset -> cryptoAsset.asset }.toSet().plus(
                coincore.availableCryptoAssets().sortedBy { assetInfo: AssetInfo -> assetInfo.displayTicker }
            ).toList()
        }
}