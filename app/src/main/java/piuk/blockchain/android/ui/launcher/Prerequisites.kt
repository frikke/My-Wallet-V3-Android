package piuk.blockchain.android.ui.launcher

import com.blockchain.coincore.Coincore
import com.blockchain.core.auth.metadata.WalletCredentialsMetadataUpdater
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataInitException
import com.blockchain.metadata.MetadataService
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.utils.then
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.rxCompletable
import piuk.blockchain.android.ui.home.HomeActivityLauncher

class Prerequisites(
    private val metadataService: MetadataService,
    private val xlmDataManager: XlmDataManager,
    private val ethDataManager: EthDataManager,
    private val settingsDataManager: SettingsDataManager,
    private val coincore: Coincore,
    private val payloadDataManager: PayloadDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val remoteLogger: RemoteLogger,
    private val homeActivityLauncher: HomeActivityLauncher,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val globalEventHandler: GlobalEventHandler,
    private val walletCredentialsUpdater: WalletCredentialsMetadataUpdater
) {

    fun initMetadataAndRelatedPrerequisites(): Completable =
        metadataService.attemptMetadataSetup().then {
            if (payloadDataManager.isDoubleEncrypted) {
                checkIfCoinsMissingPubKeyDerivation()
            } else Completable.complete()
        }
            .logOnError(METADATA_ERROR_MESSAGE)
            .onErrorResumeNext {
                if (it is InvalidCredentialsException || it is HDWalletException) {
                    Completable.error(it)
                } else
                    Completable.error(MetadataInitException(it))
            }.then {
                coincore.init() // Coincore signals the remote logger internally
            }.then {
                walletCredentialsUpdater.checkAndUpdate()
                    .logAndCompleteOnError(WALLET_CREDENTIALS)
            }.then {
                Completable.fromCallable {
                    walletConnectServiceAPI.init()
                }
            }
            .then {
                rxCompletable {
                    homeActivityLauncher.updateHomeActivity()
                }
            }
            .doOnComplete {
                globalEventHandler.init()
            }
            .subscribeOn(Schedulers.io())

    /**
     * At this step we need to ensure that 2nd password wallets have already derived the pubkeys.
     * If not we need to ask for 2nd pass and derive them from the master key.
     */
    private fun checkIfCoinsMissingPubKeyDerivation(): Completable {
        val eth = ethDataManager.initEthereumWallet()
        val xlm = xlmDataManager.maybeDefaultAccount().switchIfEmpty(
            Single.error(HDWalletException("Second password is required for XLM metadata"))
        ).flatMapCompletable {
            Completable.complete()
        }

        return eth.then {
            xlm
        }
    }

    private fun Completable.logOnError(tag: String): Completable =
        this.doOnError {
            remoteLogger.logException(
                CustomLogMessagedException(tag, it)
            )
        }

    private fun Completable.logAndCompleteOnError(tag: String): Completable =
        this.logOnError(tag).onErrorComplete()

    fun initSettings(guid: String, sharedKey: String): Single<Settings> =
        settingsDataManager.initSettings(
            guid,
            sharedKey
        ).firstOrError()

    fun decryptAndSetupMetadata(secondPassword: String): Completable {
        return Completable.fromCallable {
            payloadDataManager.decryptHDWallet(secondPassword)
        }.then {
            metadataService.decryptAndSetupMetadata()
        }
    }

    fun warmCaches(): Completable =
        exchangeRates.init()

    companion object {
        private const val METADATA_ERROR_MESSAGE = "metadata_init"
        private const val SIMPLE_BUY_SYNC = "simple_buy_sync"
        private const val WALLET_CREDENTIALS = "wallet_credentials"
        private const val WALLET_CONNECT = "wallet_connect"
    }
}
