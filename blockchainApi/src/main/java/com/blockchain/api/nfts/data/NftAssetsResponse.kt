package com.blockchain.api.nfts.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//@Serializable
//data class NftAssetResponse(
//    val nftBalances: NftBalances
//)
//
//@Serializable
//data class NftBalances(
//    val address: String,
//    val balances: List<NftAssetInfo>,
//    val totalCount: Int
//)
//
//@Serializable
//data class NftAssetInfo(
//    @SerialName("contract")
//    val contractData: NftContract,
//    val tokenId: String,
//    val metadata: NftMetadata
//)
//
//@Serializable
//data class NftContract(
//    val name: String,
//    val identifier: String,
//    val baseUri: String,
//    val network: String,
//    @SerialName("interface")
//    val tokenType: String
//)
//
//@Serializable
//data class NftMetadata(
//    val name: String,
//    val image: String,
//    val description: String,
//    val attributes: List<NftAttribute>,
//    val media: List<NftMediaInfo>
//)
//
//@Serializable
//data class NftAttribute(
//    val value: String,
//    @SerialName("trait_type")
//    val traitType: String
//)
//
//@Serializable
//data class NftMediaInfo(
//    val raw: String,
//    val gateway: String?,
//    val thumbnail: String?
//)

@Serializable
data class NftAssetsResponse(
    @SerialName("next")
    val next: String? = null,
    @SerialName("previous")
    val previous: String? = null,
    @SerialName("assets")
    val assets: ArrayList<NftAssetResponse>,
)

@Serializable
data class AssetContract(
    @SerialName("address")
    val address: String?,
    @SerialName("asset_contract_type")
    val assetContractType: String?,
    @SerialName("created_date")
    val createdDate: String?,
    @SerialName("date_ingested")
    val dateIngested: String?,
    @SerialName("nft_version")
    val nftVersion: String?,
    @SerialName("opensea_version")
    val openseaVersion: String?,
    @SerialName("owner")
    val owner: Int?,
    @SerialName("schema_name")
    val schemaName: String?,
    @SerialName("total_supply")
    val totalSupply: String?
)

@Serializable
data class DisplayData(
    @SerialName("card_display_style")
    val cardDisplayStyle: String?
)

@Serializable
data class PaymentTokens(
    @SerialName("address") val address: String?,
    @SerialName("date_ingested") val dateIngested: String?,
    @SerialName("date_updated") val dateUpdated: String?,
    @SerialName("decimals") val decimals: Int?,
    @SerialName("eth_price") val ethPrice: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("name") val name: String?,
    @SerialName("symbol") val symbol: String?,
    @SerialName("usd_price") val usdPrice: String?
)

@Serializable
data class NftAssetResponse(
    @SerialName("animation_url") val animationUrl: String?,
    @SerialName("animation_original_url") val animationOriginalUrl: String?,
    @SerialName("asset_contract") val assetContract: AssetContract?,
    @SerialName("background_color") val backgroundColor: String?,
    @SerialName("collection") val collection: Collection?,
    @SerialName("creator") val creator: String?,
    @SerialName("date_ingested") val dateIngested: String?,
    @SerialName("date_updated") val dateUpdated: String?,
    @SerialName("decimals") val decimals: Int?,
    @SerialName("description") val description: String?,
    @SerialName("external_link") val externalLink: String?,
    @SerialName("id") val id: Int?,
    @SerialName("image_original_url") val imageOriginalUrl: String?,
    @SerialName("image_preview_url") val imagePreviewUrl: String?,
    @SerialName("image_thumbnail_url") val imageThumbnailUrl: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("is_nsfw") val isNsfw: Boolean?,
    @SerialName("is_presale") val isPresale: Boolean?,
    @SerialName("large_image_url") val largeImageUrl: String?,
    @SerialName("name") val name: String?,
    @SerialName("network") val network: String?,
    @SerialName("num_sales") val numSales: Int?,
    @SerialName("offers") val offers: ArrayList<Offers>,
    @SerialName("owners") val owners: ArrayList<String>,
    @SerialName("permalink") val permalink: String?,
    @SerialName("supports_wyvern") val supportsWyvern: Boolean?,
    @SerialName("token_id") val tokenId: String?,
    @SerialName("token_metadata") val tokenMetadata: String?,
    @SerialName("transfer_fee") val transferFee: String?,
    @SerialName("traits") val traits: ArrayList<String>
)

@Serializable
data class Offers(
    @SerialName("bid_amount") val bidAmount: String?,
    @SerialName("collection_slug") val collectionSlug: String?,
    @SerialName("contract_address") val contractAddress: String?,
    @SerialName("created_date") val createdDate: String?,
    @SerialName("dev_seller_fee_basis_points") val devSellerFeeBasisPoints: Int?,
    @SerialName("event_type") val eventType: String?,
    @SerialName("id") val id: Int?,
    @SerialName("quantity") val quantity: String?
)

@Serializable
data class Collection(
    @SerialName("banner_image_url") val bannerImageUrl: String?,
    @SerialName("buyer_fee_basis_points") val buyerFeeBasisPoints: Int?,
    @SerialName("chat_url") val chatUrl: String?,
    @SerialName("created_date") val createdDate: String?,
    @SerialName("date_ingested") val dateIngested: String?,
    @SerialName("date_updated") val dateUpdated: String?,
    @SerialName("default_to_fiat") val defaultToFiat: Boolean?,
    @SerialName("description") val description: String?,
    @SerialName("dev_buyer_fee_basis_points") val devBuyerFeeBasisPoints: String?,
    @SerialName("dev_seller_fee_basis_points") val devSellerFeeBasisPoints: String?,
    @SerialName("discord_url") val discordUrl: String?,
    @SerialName("display_data") val displayData: DisplayData?,
    @SerialName("external_url") val externalUrl: String?,
    @SerialName("featured") val featured: Boolean?,
    @SerialName("featured_image_url") val featuredImageUrl: String?,
    @SerialName("hidden") val hidden: Boolean?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("instagram_username") val instagramUsername: String?,
    @SerialName("is_subject_to_whitelist") val isSubjectToWhitelist: Boolean?,
    @SerialName("large_image_url") val largeImageUrl: String?,
    @SerialName("medium_username") val mediumUsername: String?,
    @SerialName("name") val name: String?,
    @SerialName("network") val network: String?,
    @SerialName("num_owners") val numOwners: Int?,
    @SerialName("only_proxied_transfers") val onlyProxiedTransfers: Boolean?,
    @SerialName("opensea_buyer_fee_basis_points") val openseaBuyerFeeBasisPoints: String?,
    @SerialName("opensea_seller_fee_basis_points") val openseaSellerFeeBasisPoints: String?,
    @SerialName("payment_tokens") val paymentTokens: ArrayList<PaymentTokens>,
    @SerialName("payout_address") val payoutAddress: String?,
    @SerialName("primary_asset_contracts") val primaryAssetContracts: ArrayList<PrimaryAssetContracts>,
    @SerialName("require_email") val requireEmail: Boolean?,
    @SerialName("safelist_request_status") val safelistRequestStatus: String?,
    @SerialName("seller_fee_basis_points") val sellerFeeBasisPoints: Int?,
    @SerialName("short_description") val shortDescription: String?,
    @SerialName("slug") val slug: String?,
    @SerialName("stats") val stats: Stats?,
    @SerialName("telegram_url") val telegramUrl: String?,
    @SerialName("total_supply") val totalSupply: Int?,
    @SerialName("traits") val traits: String?,
    @SerialName("twitter_username") val twitterUsername: String?,
    @SerialName("wiki_url") val wikiUrl: String?
)

@Serializable
data class Stats(
    @SerialName("floor_price") val floorPrice: Int?,
    @SerialName("total_sales") val totalSales: Int?,
    @SerialName("total_volume") val totalVolume: Int?,
    @SerialName("average_price") val averagePrice: Int?,
    @SerialName("one_day_sales") val oneDaySales: Int?,
    @SerialName("one_day_change") val oneDayChange: Int?,
    @SerialName("one_day_volume") val oneDayVolume: Int?,
    @SerialName("seven_day_sales") val sevenDaySales: Int?,
    @SerialName("seven_day_change") val sevenDayChange: Int?,
    @SerialName("seven_day_volume") val sevenDayVolume: Int?,
    @SerialName("thirty_day_sales") val thirtyDaySales: Int?,
    @SerialName("thirty_day_change") val thirtyDayChange: Int?,
    @SerialName("thirty_day_volume") val thirtyDayVolume: Int?,
    @SerialName("one_day_average_price") val oneDayAveragePrice: Int?,
    @SerialName("seven_day_average_price") val sevenDayAveragePrice: Int?,
    @SerialName("thirty_day_average_price") val thirtyDayAveragePrice: Int?
)

@Serializable
data class PrimaryAssetContracts(
    @SerialName("address") val address: String?,
    @SerialName("asset_contract_type") val assetContractType: String?,
    @SerialName("created_date") val createdDate: String?,
    @SerialName("date_ingested") val dateIngested: String?,
    @SerialName("nft_version") val nftVersion: String?,
    @SerialName("opensea_version") val openseaVersion: String?,
    @SerialName("owner") val owner: Int?,
    @SerialName("schema_name") val schemaName: String?,
    @SerialName("total_supply") val totalSupply: String?
)