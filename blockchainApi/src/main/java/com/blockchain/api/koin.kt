package com.blockchain.api

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.api.addressmapping.AddressMappingApiInterface
import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.assetdiscovery.AssetDiscoveryApiInterface
import com.blockchain.api.assetdiscovery.data.assetTypeSerializers
import com.blockchain.api.assetprice.AssetPriceApiInterface
import com.blockchain.api.auth.AuthApiInterface
import com.blockchain.api.bitcoin.BitcoinApi
import com.blockchain.api.brokerage.BrokerageApi
import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.ethereum.EthereumApiInterface
import com.blockchain.api.interest.InterestApiInterface
import com.blockchain.api.nabu.NabuUserApi
import com.blockchain.api.paymentmethods.PaymentMethodsApi
import com.blockchain.api.payments.PaymentsApi
import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.AnalyticsService
import com.blockchain.api.services.AssetDiscoveryService
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.AuthApiService
import com.blockchain.api.services.BrokerageService
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.InterestService
import com.blockchain.api.services.NabuUserService
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.api.services.PaymentsService
import com.blockchain.api.services.TradeService
import com.blockchain.api.services.TxLimitsService
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.api.trade.TradeApi
import com.blockchain.api.txlimits.TxLimitsApi
import com.blockchain.api.wallet.WalletApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

val blockchainApi = StringQualifier("blockchain-api")
val explorerApi = StringQualifier("explorer-api")
val nabuApi = StringQualifier("nabu-api")
val assetsApi = StringQualifier("assets-api")

private val json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
    isLenient = true
}

private val jsonConverter = json.asConverterFactory("application/json".toMediaType())

val blockchainApiModule = module {

    single { RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()) }

    single { OutcomeCallAdapterFactory() }

    single(blockchainApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addConverterFactory(jsonConverter)
            .build()
    }

    single(explorerApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("explorer-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addConverterFactory(jsonConverter)
            .build()
    }

    single(nabuApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("nabu-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(jsonConverter)
            .build()
    }

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
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(assetsApi).create(AssetDiscoveryApiInterface::class.java)
        AssetDiscoveryService(
            api,
            getProperty("api-code")
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
        val api = get<Retrofit>(nabuApi).create(NabuUserApi::class.java)
        NabuUserService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(PaymentsApi::class.java)
        PaymentsService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(PaymentMethodsApi::class.java)
        PaymentMethodsService(api)
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(CustodialBalanceApi::class.java)
        CustodialBalanceService(
            api = api
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
        InterestService(
            api = api
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
}

fun Scope.getBaseUrl(propName: String): String = getProperty(propName)
