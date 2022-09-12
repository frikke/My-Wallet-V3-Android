package com.blockchain.api

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.api.addressmapping.AddressMappingApiInterface
import com.blockchain.api.addressverification.AddressVerificationApi
import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.assetdiscovery.AssetDiscoveryApiInterface
import com.blockchain.api.assetdiscovery.data.AssetInformationDto
import com.blockchain.api.assetdiscovery.data.AssetType
import com.blockchain.api.assetdiscovery.data.CeloTokenAsset
import com.blockchain.api.assetdiscovery.data.CoinAsset
import com.blockchain.api.assetdiscovery.data.Erc20Asset
import com.blockchain.api.assetdiscovery.data.FiatAsset
import com.blockchain.api.assetdiscovery.data.UnsupportedAsset
import com.blockchain.api.assetdiscovery.data.assetTypeSerializers
import com.blockchain.api.assetprice.AssetPriceApiInterface
import com.blockchain.api.auth.AuthApiInterface
import com.blockchain.api.bitcoin.BitcoinApi
import com.blockchain.api.blockchainCard.BlockchainCardApi
import com.blockchain.api.blockchainCard.WalletHelperUrl
import com.blockchain.api.brokerage.BrokerageApi
import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.dataremediation.DataRemediationApi
import com.blockchain.api.eligibility.EligibilityApi
import com.blockchain.api.ethereum.EthereumApiInterface
import com.blockchain.api.ethereum.evm.EvmApi
import com.blockchain.api.fiatcurrencies.FiatCurrenciesApi
import com.blockchain.api.interest.InterestApiInterface
import com.blockchain.api.interest.InterestApiService
import com.blockchain.api.kyc.KycApiInterface
import com.blockchain.api.kyc.KycApiService
import com.blockchain.api.nabu.NabuUserApi
import com.blockchain.api.nfts.api.NftApi
import com.blockchain.api.nftwaitlist.data.api.NftWaitlistApi
import com.blockchain.api.paymentmethods.PaymentMethodsApi
import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.referral.ReferralApi
import com.blockchain.api.selfcustody.SelfCustodyApi
import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.AddressVerificationApiService
import com.blockchain.api.services.AnalyticsService
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.AuthApiService
import com.blockchain.api.services.BlockchainCardService
import com.blockchain.api.services.BrokerageService
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.DataRemediationApiService
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.api.services.FiatCurrenciesApiService
import com.blockchain.api.services.NabuUserService
import com.blockchain.api.services.NftService
import com.blockchain.api.services.NftWaitlistApiService
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.api.services.PaymentsService
import com.blockchain.api.services.ReferralApiService
import com.blockchain.api.services.TradeService
import com.blockchain.api.services.TxLimitsService
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.api.services.WatchlistService
import com.blockchain.api.trade.TradeApi
import com.blockchain.api.txlimits.TxLimitsApi
import com.blockchain.api.wallet.WalletApi
import com.blockchain.api.watchlist.WatchlistApi
import com.blockchain.koin.authOkHttpClient
import com.blockchain.koin.kotlinJsonConverterFactory
import com.blockchain.koin.kotlinXApiRetrofit
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.IsoDateSerializer
import com.blockchain.serializers.KZonedDateTimeSerializer
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

val blockchainApi = StringQualifier("blockchain-api")
val walletPubkeyApi = StringQualifier("wallet-pubkey-api")
val explorerApi = StringQualifier("explorer-api")
val nabuApi = StringQualifier("nabu-api")
val assetsApi = StringQualifier("assets-api")

val blockchainApiModule = module {
    single { get<Json>().asConverterFactory("application/json".toMediaType()) }

    single { RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()) }

    single { OutcomeCallAdapterFactory() }

    single(blockchainApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .build()
    }

    single(walletPubkeyApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("wallet-pubkey-api"))
            .client(get())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .build()
    }

    single(explorerApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("explorer-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .build()
    }

    single(nabuApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("nabu-api"))
            .client(get(authOkHttpClient))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .build()
    }

    // *****
    // BaseJson
    @OptIn(ExperimentalSerializationApi::class)
    single {
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            serializersModule = SerializersModule {
                contextual(BigDecimalSerializer)
                contextual(BigIntSerializer)
                contextual(IsoDateSerializer)
                contextual(KZonedDateTimeSerializer)
                polymorphic(AssetType::class) {
                    subclass(CoinAsset::class)
                    subclass(Erc20Asset::class)
                    subclass(CeloTokenAsset::class)
                    subclass(FiatAsset::class)
                    subclass(AssetInformationDto::class)
                    default { UnsupportedAsset.serializer() }
                }
            }
        }
    }
    // *****

    single(assetsApi) {
        // Can't use the standard convertor here, because we need to set a discriminator
        // for some polymorphic objects
        val json = Json {
            explicitNulls = false
            serializersModule = assetTypeSerializers
            ignoreUnknownKeys = true
            isLenient = true
            classDiscriminator = "name"
        }

        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(BitcoinApi::class.java)
        NonCustodialBitcoinService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(EthereumApiInterface::class.java)
        NonCustodialErc20Service(
            api
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(EvmApi::class.java)
        NonCustodialEvmService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(walletPubkeyApi).create(SelfCustodyApi::class.java)
        DynamicSelfCustodyService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(assetsApi).create(AssetDiscoveryApiInterface::class.java)
        AssetDiscoveryApiService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(FiatCurrenciesApi::class.java)
        FiatCurrenciesApiService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AssetPriceApiInterface::class.java)
        AssetPriceService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(explorerApi).create(WalletApi::class.java)
        WalletSettingsService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AuthApiInterface::class.java)
        AuthApiService(
            api,
            getProperty("api-code"),
            getProperty("site-key")
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AddressMappingApiInterface::class.java)
        AddressMappingService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AnalyticsApiInterface::class.java)
        AnalyticsService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(AddressVerificationApi::class.java)
        AddressVerificationApiService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(NabuUserApi::class.java)
        NabuUserService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(KycApiInterface::class.java)
        KycApiService(
            kycApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(PaymentsApi::class.java)
        PaymentsService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(EligibilityApi::class.java)
        EligibilityApiService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(PaymentMethodsApi::class.java)
        PaymentMethodsService(
            api = api,
            remoteConfigPrefs = get(),
            environmentConfig = get()
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(DataRemediationApi::class.java)
        DataRemediationApiService(api)
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(CustodialBalanceApi::class.java)
        CustodialBalanceService(
            custodialBalanceApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(BrokerageApi::class.java)
        BrokerageService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(InterestApiInterface::class.java)
        InterestApiService(
            interestApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(TradeApi::class.java)
        TradeService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(TxLimitsApi::class.java)
        TxLimitsService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(WatchlistApi::class.java)
        WatchlistService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(BlockchainCardApi::class.java)
        BlockchainCardService(
            api,
            get()
        )
    }

    factory {
        object : WalletHelperUrl {
            override val url: String
                get() = getProperty("wallet-helper-url")
        }
    }.bind(WalletHelperUrl::class)

    factory {
        val api = get<Retrofit>(nabuApi).create(ReferralApi::class.java)
        ReferralApiService(
            api = api,
        )
    }

    factory {
        val api = get<Retrofit>(kotlinXApiRetrofit).create(NftWaitlistApi::class.java)
        NftWaitlistApiService(
            nftWaitlistApi = api
        )
    }

    factory {
        val api = get<Retrofit>(explorerApi).create(NftApi::class.java)
        NftService(
            nftApi = api
        )
    }
}

fun Scope.getBaseUrl(propName: String): String = getProperty(propName)
