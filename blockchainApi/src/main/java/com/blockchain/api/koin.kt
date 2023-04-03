package com.blockchain.api

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.api.addressmapping.AddressMappingApiInterface
import com.blockchain.api.addressverification.AddressVerificationApi
import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.announcements.AnnouncementsApi
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
import com.blockchain.api.brokerage.BrokerageApi
import com.blockchain.api.coinnetworks.CoinNetworkApiInterface
import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.dataremediation.DataRemediationApi
import com.blockchain.api.dex.AllowanceApi
import com.blockchain.api.dex.AllowanceApiService
import com.blockchain.api.dex.DexApi
import com.blockchain.api.dex.DexApiService
import com.blockchain.api.dex.DexQuotesApi
import com.blockchain.api.dex.DexQuotesApiService
import com.blockchain.api.earn.active.ActiveRewardsApi
import com.blockchain.api.earn.active.ActiveRewardsApiService
import com.blockchain.api.earn.passive.InterestApiInterface
import com.blockchain.api.earn.passive.InterestApiService
import com.blockchain.api.earn.staking.StakingApi
import com.blockchain.api.earn.staking.StakingApiService
import com.blockchain.api.eligibility.EligibilityApi
import com.blockchain.api.ethereum.EthereumApiInterface
import com.blockchain.api.ethereum.evm.EvmApi
import com.blockchain.api.experiments.ExperimentsApi
import com.blockchain.api.fees.FeesApi
import com.blockchain.api.fees.WithdrawFeesService
import com.blockchain.api.fiatcurrencies.FiatCurrenciesApi
import com.blockchain.api.fraud.FraudApi
import com.blockchain.api.kyc.KycApiInterface
import com.blockchain.api.kyc.KycApiService
import com.blockchain.api.kyc.ProveApi
import com.blockchain.api.kyc.ProveApiService
import com.blockchain.api.mercuryexperiments.MercuryExperimentsApi
import com.blockchain.api.nabu.NabuUserApi
import com.blockchain.api.nfts.api.NftApi
import com.blockchain.api.nftwaitlist.data.api.NftWaitlistApi
import com.blockchain.api.paymentmethods.PaymentMethodsApi
import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.recurringbuy.RecurringBuyApi
import com.blockchain.api.referral.ReferralApi
import com.blockchain.api.selfcustody.SelfCustodyApi
import com.blockchain.api.selfcustody.activity.ActivityRequest
import com.blockchain.api.selfcustody.activity.ActivityResponse
import com.blockchain.api.selfcustody.activity.activityDetailSerializer
import com.blockchain.api.selfcustody.activity.activityIconSerializer
import com.blockchain.api.selfcustody.activity.activityViewItemSerializer
import com.blockchain.api.selfcustody.activity.stackComponentSerializer
import com.blockchain.api.services.ActivityWebSocketService
import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.AddressVerificationApiService
import com.blockchain.api.services.AnalyticsService
import com.blockchain.api.services.AnnouncementsApiService
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.AuthApiService
import com.blockchain.api.services.BrokerageService
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.DataRemediationApiService
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.api.services.EligibilityApiService
import com.blockchain.api.services.ExperimentsApiService
import com.blockchain.api.services.FiatCurrenciesApiService
import com.blockchain.api.services.FraudRemoteService
import com.blockchain.api.services.MercuryExperimentsApiService
import com.blockchain.api.services.NabuUserService
import com.blockchain.api.services.NftApiService
import com.blockchain.api.services.NftWaitlistApiService
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.api.services.PaymentsService
import com.blockchain.api.services.RecurringBuyApiService
import com.blockchain.api.services.ReferralApiService
import com.blockchain.api.services.TradeService
import com.blockchain.api.services.TxLimitsService
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.api.services.WatchlistApiService
import com.blockchain.api.trade.TradeApi
import com.blockchain.api.txlimits.TxLimitsApi
import com.blockchain.api.wallet.WalletApi
import com.blockchain.api.watchlist.WatchlistApi
import com.blockchain.koin.applicationScope
import com.blockchain.koin.authOkHttpClient
import com.blockchain.koin.iterableRetrofit
import com.blockchain.koin.kotlinJsonConverterFactory
import com.blockchain.koin.kotlinXApiRetrofit
import com.blockchain.koin.kotlinXCoinApiRetrofit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.network.modules.OkHttpLoggingInterceptors
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.network.websocket.toJsonSocket
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
import okhttp3.OkHttpClient
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

val blockchainApi = StringQualifier("blockchain-api")
val walletPubkeyApi = StringQualifier("wallet-pubkey-api")
val explorerApi = StringQualifier("explorer-api")
val nabuApi = StringQualifier("nabu-api")
val dexApi = StringQualifier("dex-api")
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

    single(dexApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("dex-api"))
            .client(get(authOkHttpClient))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .build()
    }

    single(kotlinXCoinApiRetrofit) {
        val builder: OkHttpClient.Builder = get()
        get<OkHttpLoggingInterceptors>().forEach {
            builder.addInterceptor(it)
        }
        val client = builder.build()

        val json = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            encodeDefaults = true
        }
        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(client)
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
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
            allowSpecialFloatingPointValues = true
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
                stackComponentSerializer()
                activityViewItemSerializer()
                activityIconSerializer()
                activityDetailSerializer()
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
        val api = get<Retrofit>(assetsApi).create(AssetDiscoveryApiInterface::class.java)
        val coinNetworkApi = get<Retrofit>(kotlinXCoinApiRetrofit).create(CoinNetworkApiInterface::class.java)
        AssetDiscoveryApiService(
            api,
            coinNetworkApi
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
            api = api,
            apiCode = getProperty("api-code"),
            captchaSiteKey = getProperty("site-key"),
            sessionIdService = get()
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
        val api = get<Retrofit>(dexApi).create(DexApi::class.java)
        DexApiService(
            api = api
        )
    }
    factory {
        val api = get<Retrofit>(blockchainApi).create(AllowanceApi::class.java)
        AllowanceApiService(
            api = api
        )
    }
    factory {
        val api = get<Retrofit>(nabuApi).create(DexQuotesApi::class.java)
        DexQuotesApiService(
            dexQuotesApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(ProveApi::class.java)
        ProveApiService(
            api = api
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
        val api = get<Retrofit>(nabuApi).create(RecurringBuyApi::class.java)
        RecurringBuyApiService(
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
        val api = get<Retrofit>(nabuApi).create(FeesApi::class.java)
        WithdrawFeesService(
            feesApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(WatchlistApi::class.java)
        WatchlistApiService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(FraudApi::class.java)
        FraudRemoteService(api)
    }

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
        val api = get<Retrofit>(blockchainApi).create(NftApi::class.java)
        NftApiService(
            nftApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(ExperimentsApi::class.java)
        ExperimentsApiService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(StakingApi::class.java)
        StakingApiService(
            stakingApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(ActiveRewardsApi::class.java)
        ActiveRewardsApiService(
            activeRewardsApi = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(MercuryExperimentsApi::class.java)
        MercuryExperimentsApiService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(iterableRetrofit).create(AnnouncementsApi::class.java)
        AnnouncementsApiService(
            api = api
        )
    }

    scope(payloadScopeQualifier) {

        scoped {
            val api = get<Retrofit>(walletPubkeyApi).create(SelfCustodyApi::class.java)
            DynamicSelfCustodyService(
                selfCustodyApi = api,
                credentials = get()
            )
        }

        scoped {
            val webSocket = get<OkHttpClient>()
                .newBlockchainWebSocket(
                    options = Options(url = getBaseUrl("unified-activity-ws"))
                )
                .autoRetry()
                .debugLog("ACTIVITY_LOG")
                .toJsonSocket(
                    json = get(),
                    outgoingAdapter = ActivityRequest.serializer(),
                    incomingAdapter = ActivityResponse.serializer()
                )
            ActivityWebSocketService(
                webSocket = webSocket,
                activityCacheService = get(),
                credentials = get(),
                lifecycleObservable = get(),
                wsScope = get(applicationScope)
            )
        }
    }
}

fun Scope.getBaseUrl(propName: String): String = getProperty(propName)
