package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.coincore.AddressFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionProcessor
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import com.blockchain.coincore.AddressParseError
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxConfirmationValue
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.androidcore.utils.extensions.mapList
import timber.log.Timber

class TransactionInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory,
    private val custodialRepository: CustodialRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val identity: UserIdentity,
    private val accountsSorting: AccountsSorting,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val bankLinkingPrefs: BankLinkingPrefs
) {
    private var transactionProcessor: TransactionProcessor? = null
    private val invalidate = PublishSubject.create<Unit>()

    fun invalidateTransaction(): Completable =
        Completable.fromAction {
            reset()
            transactionProcessor = null
        }

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))

    fun validateTargetAddress(address: String, asset: AssetInfo): Single<ReceiveAddress> =
        addressFactory.parse(address, asset)
            .switchIfEmpty(
                Single.error(
                    TxValidationFailure(ValidationState.INVALID_ADDRESS)
                )
            )
            .onErrorResumeNext { e ->
                if (e.isUnexpectedContractError) {
                    Single.error(TxValidationFailure(ValidationState.ADDRESS_IS_CONTRACT))
                } else {
                    Single.error(e)
                }
            }

    fun initialiseTransaction(
        sourceAccount: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Observable<PendingTx> =
        coincore.createTransactionProcessor(sourceAccount, target, action)
            .doOnSubscribe { Timber.d("!TRANSACTION!> SUBSCRIBE") }
            .doOnSuccess {
                if (transactionProcessor != null)
                    throw IllegalStateException("TxProcessor double init")
            }
            .doOnSuccess { transactionProcessor = it }
            .doOnError {
                Timber.e("!TRANSACTION!> error initialising $it")
            }.flatMapObservable {
                it.initialiseTx()
            }.takeUntil(invalidate)

    val canTransactFiat: Boolean
        get() = transactionProcessor?.canTransactFiat ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionAmount(amount: Money): Completable =
        transactionProcessor?.updateAmount(amount) ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionFees(feeLevel: FeeLevel, customFeeAmount: Long?): Completable =
        transactionProcessor?.updateFeeLevel(
            level = feeLevel,
            customFeeAmount = customFeeAmount
        ) ?: throw IllegalStateException("TxProcessor not initialised")

    fun getTargetAccounts(sourceAccount: BlockchainAccount, action: AssetAction): Single<SingleAccountList> =
        when (action) {
            AssetAction.Swap -> swapTargets(sourceAccount as CryptoAccount)
            AssetAction.Sell -> sellTargets(sourceAccount as CryptoAccount)
            AssetAction.FiatDeposit -> linkedBanksFactory.getNonWireTransferBanks().mapList { it }
            AssetAction.Withdraw -> linkedBanksFactory.getAllLinkedBanks().mapList { it }
            else -> coincore.getTransactionTargets(sourceAccount as CryptoAccount, action)
        }

    private fun sellTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> {
        val availableFiats =
            custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency)
        val apiPairs = Single.zip(
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(),
            availableFiats
        ) { supportedPairs, fiats ->
                supportedPairs.pairs.filter { fiats.contains(it.fiatCurrency) }
                    .map {
                        CurrencyPair.CryptoToFiatCurrencyPair(
                            it.cryptoCurrency,
                            it.fiatCurrency
                        )
                    }
            }

        return Singles.zip(
            coincore.getTransactionTargets(sourceAccount, AssetAction.Sell),
            apiPairs
        ).map { (accountList, pairs) ->
            accountList.filterIsInstance(FiatAccount::class.java)
                .filter { account ->
                    pairs.any { it.source == sourceAccount.asset && account.fiatCurrency == it.destination }
                }
        }
    }

    private fun swapTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> =
        Singles.zip(
            coincore.getTransactionTargets(sourceAccount, AssetAction.Swap),
            custodialRepository.getSwapAvailablePairs(),
            identity.isEligibleFor(Feature.SimpleBuy)
        ).map { (accountList, pairs, eligible) ->
            accountList.filterIsInstance(CryptoAccount::class.java)
                .filter { account ->
                    pairs.any { it.source == sourceAccount.asset && account.asset == it.destination }
                }.filter { account ->
                    eligible or (account is NonCustodialAccount)
                }
        }

    fun getAvailableSourceAccounts(
        action: AssetAction,
        targetAccount: TransactionTarget
    ): Single<SingleAccountList> =
        when (action) {
            AssetAction.Swap -> {
                coincore.allWalletsWithActions(setOf(action), accountsSorting.sorter())
                    .zipWith(
                        custodialRepository.getSwapAvailablePairs()
                    ).map { (accounts, pairs) ->
                        accounts.filter { account ->
                            (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false
                        }
                    }.map {
                        it.map { account -> account as CryptoAccount }
                    }
            }
            AssetAction.InterestDeposit -> {
                require(targetAccount is InterestAccount)
                require(targetAccount is CryptoAccount)
                coincore.allWalletsWithActions(setOf(action), accountsSorting.sorter()).map {
                    it.filter { acc ->
                        acc is CryptoAccount && acc.asset == targetAccount.asset && acc != targetAccount
                    }
                }
            }
            AssetAction.FiatDeposit -> {
                linkedBanksFactory.getNonWireTransferBanks().map { it }
            }
            else -> throw IllegalStateException("Source account should be preselected for action $action")
        }

    fun verifyAndExecute(secondPassword: String): Completable =
        transactionProcessor?.execute(secondPassword) ?: throw IllegalStateException("TxProcessor not initialised")

    fun modifyOptionValue(newConfirmation: TxConfirmationValue): Completable =
        transactionProcessor?.setOption(newConfirmation) ?: throw IllegalStateException("TxProcessor not initialised")

    fun startFiatRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.userExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.targetExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun validateTransaction(): Completable =
        transactionProcessor?.validateAll() ?: throw IllegalStateException("TxProcessor not initialised")

    fun reset() {
        invalidate.onNext(Unit)
        transactionProcessor?.reset() ?: Timber.i("TxProcessor is not initialised yet")
    }

    fun linkABank(selectedFiat: String): Single<LinkBankTransfer> = custodialWalletManager.linkToABank(selectedFiat)

    fun updateFiatDepositState(bankPaymentData: BankPaymentApproval) {
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(
                bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_PENDING,
                bankPaymentData = bankPaymentData
            ).toPreferencesValue()
        )

        val sanitisedUrl = bankPaymentData.linkedBank.callbackPath.removePrefix("nabu-gateway/")
        bankLinkingPrefs.setDynamicOneTimeTokenUrl(sanitisedUrl)
    }
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair.CryptoCurrencyPair>): Boolean =
    pairs.any { it.source == this.asset }

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
