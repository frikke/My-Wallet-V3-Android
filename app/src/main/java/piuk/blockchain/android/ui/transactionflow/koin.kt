package piuk.blockchain.android.ui.transactionflow

import android.content.Context
import com.blockchain.koin.defaultOrder
import com.blockchain.koin.improvedPaymentUxFeatureFlag
import com.blockchain.koin.payloadScope
import com.blockchain.koin.swapSourceOrder
import com.blockchain.koin.swapTargetOrder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionInteractor
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TxFlowErrorReporting
import piuk.blockchain.android.ui.transactionflow.flow.AmountFormatter
import piuk.blockchain.android.ui.transactionflow.flow.AvailableToTradePropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.AvailableToWithdrawPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ChainPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.CompoundNetworkFeeFormatter
import piuk.blockchain.android.ui.transactionflow.flow.DAppInfoPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.EstimatedCompletionPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ExchangePriceFormatter
import piuk.blockchain.android.ui.transactionflow.flow.FromPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.NetworkFormatter
import piuk.blockchain.android.ui.transactionflow.flow.PaymentMethodPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ProcessingFeeFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ReadMoreDisclaimerPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.SalePropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.SignEthMessagePropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.SwapExchangeRateFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ToPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ToWithNameAndAddressFormatter
import piuk.blockchain.android.ui.transactionflow.flow.TotalFormatter
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFeeFormatter
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout
import piuk.blockchain.android.ui.transactionflow.flow.TxOptionsFormatterCheckout
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.SourceSelectionCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TargetSelectionCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiser
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowInfoBottomSheetCustomiser
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowInfoBottomSheetCustomiserImpl
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionProgressCustomisations

val transactionFlowActivityScope = named("TransactionActivityScope")

val transactionModule = module {

    factory {
        TransactionFlowCustomiserImpl(
            resources = get<Context>().resources,
            assetResources = get()
        )
    }.apply {
        bind(TransactionFlowCustomiser::class)
        bind(EnterAmountCustomisations::class)
        bind(SourceSelectionCustomisations::class)
        bind(TargetSelectionCustomisations::class)
        bind(TransactionConfirmationCustomisations::class)
        bind(TransactionProgressCustomisations::class)
        bind(TransactionFlowCustomisations::class)
    }

    factory {
        TransactionFlowInfoBottomSheetCustomiserImpl(
            resources = get<Context>().resources
        )
    }.bind(TransactionFlowInfoBottomSheetCustomiser::class)

    factory {
        ExchangePriceFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        ToPropertyFormatter(
            context = get(),
            defaultLabel = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        ToWithNameAndAddressFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        FromPropertyFormatter(
            context = get(),
            defaultLabel = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        SalePropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        EstimatedCompletionPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        DAppInfoPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        ChainPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        SignEthMessagePropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        PaymentMethodPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        SwapExchangeRateFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        NetworkFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        TransactionFeeFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        CompoundNetworkFeeFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        TotalFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        ProcessingFeeFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        AmountFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        AvailableToTradePropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        AvailableToWithdrawPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        ReadMoreDisclaimerPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        TxConfirmReadOnlyMapperCheckout(
            formatters = getAll()
        )
    }

    factory {
        TxFlowAnalytics(
            analytics = get(),
            remoteLogger = get()
        )
    }

    factory {
        TxFlowErrorReporting(
            remoteLogger = get()
        )
    }

    scope(transactionFlowActivityScope) {
        scoped {
            TransactionInteractor(
                coincore = payloadScope.get(),
                addressFactory = payloadScope.get(),
                custodialRepository = payloadScope.get(),
                custodialWalletManager = payloadScope.get(),
                bankService = payloadScope.get(),
                paymentMethodService = payloadScope.get(),
                currencyPrefs = get(),
                identity = payloadScope.get(),
                defaultAccountsSorting = payloadScope.get(defaultOrder),
                swapSourceAccountsSorting = payloadScope.get(swapSourceOrder),
                swapTargetAccountsSorting = payloadScope.get(swapTargetOrder),
                linkedBanksFactory = payloadScope.get(),
                bankLinkingPrefs = payloadScope.get(),
                dismissRecorder = payloadScope.get(),
                fiatCurrenciesService = payloadScope.get(),
                tradeDataService = payloadScope.get(),
                improvedPaymentUxFF = payloadScope.get(improvedPaymentUxFeatureFlag),
                stakingService = payloadScope.get(),
                transactionPrefs = payloadScope.get(),
                activeRewardsService = payloadScope.get()
            )
        }

        scoped {
            TransactionModel(
                initialState = TransactionState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                errorLogger = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }
    }
}
