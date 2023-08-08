package com.blockchain.koin

import android.app.Application
import com.blockchain.addressverification.koin.addressVerificationModule
import com.blockchain.analytics.data.koin.analyticsModule
import com.blockchain.api.blockchainApiModule
import com.blockchain.bitpay.bitpayModule
import com.blockchain.chrome.multiAppModule
import com.blockchain.coincore.coincoreModule
import com.blockchain.common.util.commonMpUtilsModule
import com.blockchain.core.experiments.experimentsTraitsModule
import com.blockchain.core.price.pricesModule
import com.blockchain.coreandroid.coreAndroidModule
import com.blockchain.deeplinking.koin.deeplinkModule
import com.blockchain.defiwalletbackup.data.koin.backupPhraseDataModule
import com.blockchain.earn.data.koin.earnDataModule
import com.blockchain.earn.koin.earnPresentationModule
import com.blockchain.fiatActions.koin.fiatActionsModule
import com.blockchain.home.data.koin.homeDataModule
import com.blockchain.home.presentation.koin.homePresentationModule
import com.blockchain.koin.modules.apiInterceptorsModule
import com.blockchain.koin.modules.appProperties
import com.blockchain.koin.modules.applicationModule
import com.blockchain.koin.modules.coroutinesModule
import com.blockchain.koin.modules.environmentModule
import com.blockchain.koin.modules.featureFlagsModule
import com.blockchain.koin.modules.keys
import com.blockchain.koin.modules.nabuUrlModule
import com.blockchain.koin.modules.serviceModule
import com.blockchain.koin.modules.urls
import com.blockchain.koin.modules.versionsModule
import com.blockchain.koin.modules.xlmModule
import com.blockchain.kyc.kycPresentationModule
import com.blockchain.kycproviders.prove.proveModule
import com.blockchain.logging.data.koin.loggingModule
import com.blockchain.metadata.metadataModule
import com.blockchain.network.modules.apiModule
import com.blockchain.network.modules.okHttpModule
import com.blockchain.news.koin.newsDataModule
import com.blockchain.news.koin.newsPresentationModule
import com.blockchain.nfts.data.koin.nftDataModule
import com.blockchain.nfts.koin.nftPresentationModule
import com.blockchain.notifications.koin.notificationModule
import com.blockchain.payments.googlepay.googlePayPresentationModule
import com.blockchain.payments.vgs.tokenizerModule
import com.blockchain.presentation.commonPresentationModule
import com.blockchain.presentation.koin.backupPhrasePresentationModule
import com.blockchain.prices.pricesDataModule
import com.blockchain.serializers.jsonSerializers
import com.blockchain.store_caches_inmemory.storeCachesInMemoryModule
import com.blockchain.store_persisters_persistedjsonsqldelight.storePersistersJsonSqlDelightModule
import com.blockchain.transactions.koin.commonTransactionsPresentationModule
import com.blockchain.transactions.koin.receivePresentationModule
import com.blockchain.transactions.koin.sellTransactionsPresentationModule
import com.blockchain.transactions.koin.swapTransactionsPresentationModule
import com.blockchain.transactions.koin.transactionsDataModule
import com.blockchain.unifiedcryptowallet.data.koin.unifiedCryptoWalletModule
import com.blockchain.walletconnect.koin.walletConnectModule
import com.dex.data.koin.dexDataModule
import com.dex.presentation.koin.dexPresentation
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.fraud.data.fraudDataModule
import piuk.blockchain.android.maintenance.data.appMaintenanceDataModule
import piuk.blockchain.android.maintenance.domain.appMaintenanceDomainModule
import piuk.blockchain.android.maintenance.presentation.appMaintenancePresentationModule
import piuk.blockchain.android.rating.data.appRatingDataModule
import piuk.blockchain.android.rating.presentaion.appRatingPresentationModule
import piuk.blockchain.android.ui.auth.newlogin.data.secureChannelDataModule
import piuk.blockchain.android.ui.auth.newlogin.presentation.secureChannelPresentationModule
import piuk.blockchain.android.ui.brokerage.brokeragePresentationModule
import piuk.blockchain.android.ui.coinview.domain.coinviewDomainModule
import piuk.blockchain.android.ui.coinview.presentation.coinviewPresentationModule
import piuk.blockchain.android.ui.customersupport.customerSupportModule
import piuk.blockchain.android.ui.dashboard.dashboardModule
import piuk.blockchain.android.ui.debug.remoteFeatureFlagsModule
import piuk.blockchain.android.ui.home.mainModule
import piuk.blockchain.android.ui.kyc.koin.kycUiModule
import piuk.blockchain.android.ui.kyc.koin.kycUiNabuModule
import piuk.blockchain.android.ui.launcher.loader.loaderModule
import piuk.blockchain.android.ui.linkbank.alias.bankAliasLinkPresentationModule
import piuk.blockchain.android.ui.linkbank.data.bankAuthDataModule
import piuk.blockchain.android.ui.linkbank.domain.bankAuthDomainModule
import piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission.bankAuthPresentationModule
import piuk.blockchain.android.ui.login.loginUiModule
import piuk.blockchain.android.ui.referral.presentation.referralPresentationModule
import piuk.blockchain.android.ui.reset.resetAccountModule
import piuk.blockchain.android.ui.settings.redesignSettingsModule
import piuk.blockchain.android.ui.start.startupUiModule
import piuk.blockchain.android.ui.transactionflow.transactionModule
import piuk.blockchain.android.util.accessibilityModule
import piuk.blockchain.com.internalFeatureFlagsModule
import timber.log.Timber

object KoinStarter {

    @Suppress("ConstantConditionIf")
    @JvmStatic
    fun start(application: Application) {
        stopKoin()
        startKoin {
            if (BuildConfig.LOG_KOIN_STARTUP) TimberLogger() else NullLogger()
            properties(appProperties + keys + urls)
            androidContext(application)
            modules(
                listOf(
                    versionsModule,
                    featureFlagsModule,
                    apiInterceptorsModule,
                    apiModule,
                    blockchainApiModule,
                    homePresentationModule,
                    fiatActionsModule,
                    dexPresentation,
                    dexDataModule,
                    com.blockchain.prices.koin.pricesPresentationModule,
                    unifiedCryptoWalletModule,
                    homeDataModule,
                    applicationModule,
                    jsonSerializers,
                    coroutinesModule,
                    dashboardModule,
                    walletConnectModule,
                    bitpayModule,
                    coincoreModule,
                    transactionModule,
                    commonMpUtilsModule,
                    commonPresentationModule,
                    okHttpModule,
                    coreModule,
                    coreAndroidModule,
                    pricesModule,
                    environmentModule,
                    coinviewPresentationModule, coinviewDomainModule,
                    brokeragePresentationModule,
                    internalFeatureFlagsModule,
                    kycUiModule,
                    kycUiNabuModule,
                    loginUiModule,
                    loaderModule,
                    nabuModule,
                    nabuUrlModule,
                    metadataModule,
                    notificationModule,
                    resetAccountModule,
                    secureChannelPresentationModule,
                    secureChannelDataModule,
                    serviceModule,
                    startupUiModule,
                    sunriverModule,
                    walletModule,
                    xlmModule,
                    mainModule,
                    redesignSettingsModule,
                    remoteFeatureFlagsModule,
                    deeplinkModule,
                    loggingModule,
                    analyticsModule,
                    accessibilityModule,
                    experimentsTraitsModule,
                    customerSupportModule,
                    storeCachesInMemoryModule,
                    storePersistersJsonSqlDelightModule,
                    googlePayPresentationModule,
                    addressVerificationModule,
                    appMaintenanceDataModule, appMaintenanceDomainModule, appMaintenancePresentationModule,
                    bankAuthDataModule, bankAuthDomainModule, bankAuthPresentationModule,
                    referralPresentationModule,
                    appRatingDataModule, appRatingPresentationModule,
                    backupPhraseDataModule, backupPhrasePresentationModule,
                    bankAliasLinkPresentationModule,
                    fraudDataModule,
                    tokenizerModule,
                    nftDataModule, nftPresentationModule,
                    multiAppModule,
                    earnDataModule, earnPresentationModule,
                    pricesDataModule,
                    proveModule,
                    commonTransactionsPresentationModule, swapTransactionsPresentationModule,
                    sellTransactionsPresentationModule, transactionsDataModule,
                    newsDataModule, newsPresentationModule,
                    kycPresentationModule,
                    receivePresentationModule
                )
            )
        }
    }
}

private class TimberLogger : Logger() {
    override fun display(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> Timber.d(msg)
            Level.INFO -> Timber.i(msg)
            Level.ERROR -> Timber.e(msg)
            else -> {
            }
        }
    }
}

private class NullLogger : Logger() {
    override fun display(level: Level, msg: MESSAGE) {}
}
