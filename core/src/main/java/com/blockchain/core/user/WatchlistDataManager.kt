package com.blockchain.core.user

import com.blockchain.api.services.AssetTag
import com.blockchain.api.services.WatchlistService
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.outcome.fold
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxSingle

interface WatchlistDataManager {
    fun getWatchlist(): Single<Watchlist>
    fun addToWatchlist(asset: Currency, tags: List<AssetTag>): Single<WatchlistInfo>
    fun removeFromWatchList(asset: Currency, tags: List<AssetTag>): Completable
    fun isAssetInWatchlist(asset: Currency): Single<Boolean>
}

class WatchlistDataManagerImpl(
    private val authenticator: AuthHeaderProvider,
    private val watchlistService: WatchlistService,
    private val assetCatalogue: AssetCatalogue
) : WatchlistDataManager {

    override fun isAssetInWatchlist(asset: Currency): Single<Boolean> =
        getWatchlist().map {
            val existingAsset = it.assetMap[asset]
            existingAsset?.let { tags ->
                tags.firstOrNull { assetTag ->
                    assetTag == AssetTag.Favourite
                } != null
            } ?: return@map false
        }

    override fun getWatchlist(): Single<Watchlist> =
        authenticator.getAuthHeader().flatMap { header ->
            rxSingle {
                watchlistService.getWatchlist(header).fold(
                    onFailure = {
                        throw it.throwable
                    },
                    onSuccess = { response ->
                        val assetMap = mutableMapOf<Currency, List<AssetTag>>()
                        response.assets.forEach { item ->
                            assetCatalogue.fromNetworkTicker(item.asset)?.let { currency ->
                                assetMap[currency] = item.tags.map { tagItem ->
                                    AssetTag.fromString(tagItem.tag)
                                }
                            }
                        }
                        return@fold Watchlist(assetMap)
                    }
                )
            }
        }

    override fun addToWatchlist(asset: Currency, tags: List<AssetTag>): Single<WatchlistInfo> =
        authenticator.getAuthHeader().flatMap { header ->
            rxSingle {
                watchlistService.addToWatchlist(header, asset.networkTicker, tags).fold(
                    onFailure = {
                        throw it.throwable
                    },
                    onSuccess = { response ->
                        WatchlistInfo(
                            asset,
                            response.tags.map { tagItem ->
                                AssetTag.fromString(tagItem.tag)
                            }
                        )
                    }
                )
            }
        }

    override fun removeFromWatchList(asset: Currency, tags: List<AssetTag>): Completable =
        authenticator.getAuthHeader().flatMapCompletable { header ->
            rxCompletable {
                watchlistService.removeFromWatchlist(
                    header, asset.networkTicker, tags
                ).fold(
                    onFailure = {
                        Completable.error(it.throwable)
                    },
                    onSuccess = {
                        Completable.complete()
                    }
                )
            }
        }
}

data class Watchlist(
    val assetMap: Map<Currency, List<AssetTag>>
)

data class WatchlistInfo(
    val asset: Currency,
    val currentTags: List<AssetTag>
)
