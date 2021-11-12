package piuk.blockchain.android.ui.home.v2

import android.content.Intent
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.data.BankTransferStatus
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.network.PollService
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.ThePitLinkingPrefs
import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import piuk.blockchain.android.campaign.SunriverCampaignRegistration
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.LinkState
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.fromPreferencesValue
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import thepit.PitLinking

class RedesignInteractor(
    private val deepLinkProcessor: DeepLinkProcessor,
    private val exchangeLinking: PitLinking,
    private val exchangePrefs: ThePitLinkingPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val xlmDataManager: XlmDataManager,
    private val sunriverCampaignRegistration: SunriverCampaignRegistration,
    private val kycStatusHelper: KycStatusHelper,
    private val bankLinkingPrefs: BankLinkingPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val simpleBuySync: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity
) {

    fun checkForDeepLinks(intent: Intent): Single<LinkState> =
        deepLinkProcessor.getLink(intent)

    fun checkForUserWalletErrors(): Completable =
        userIdentity.checkForUserWalletLinkErrors()

    fun getExchangeLinkingState(): Single<Boolean> =
        exchangeLinking.isPitLinked()

    fun getExchangeToWalletLinkId(): String = exchangePrefs.pitToWalletLinkId

    fun getAssetFromTicker(ticker: String?): AssetInfo? =
        ticker?.let {
            assetCatalogue.fromNetworkTicker(ticker)
        }

    fun registerForCampaign(data: CampaignData): Single<KycState> =
        xlmDataManager.defaultAccount()
            .flatMapCompletable {
                sunriverCampaignRegistration
                    .registerCampaign(data)
            }
            .andThen(kycStatusHelper.getKycStatus())

    fun resetLocalBankAuthState() =
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(bankAuthFlow = BankAuthFlowState.NONE, bankPaymentData = null, bankLinkingInfo = null)
                .toPreferencesValue()
        )

    fun getBankLinkingState(): BankAuthDeepLinkState =
        bankLinkingPrefs.getBankLinkingState().fromPreferencesValue()

    fun updateBankLinkingState(bankLinkingState: BankAuthDeepLinkState) =
        bankLinkingPrefs.setBankLinkingState(bankLinkingState.toPreferencesValue())

    fun updateOpenBankingConsent(consentToken: String): Completable =
        custodialWalletManager.updateOpenBankingConsent(
            bankLinkingPrefs.getDynamicOneTimeTokenUrl(), consentToken
        ).doOnError {
            resetLocalBankAuthState()
        }

    fun pollForBankTransferCharge(paymentData: BankPaymentApproval) =
        PollService(
            custodialWalletManager.getBankTransferCharge(paymentData.paymentId)
        ) { transferDetails ->
            transferDetails.status != BankTransferStatus.PENDING
        }.start()

    fun getEstimatedDepositCompletionTime(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getSimpleBuySyncLocalState(): SimpleBuyState? = simpleBuySync.currentState()

    fun performSimpleBuySync(): Completable = simpleBuySync.performSync()
}
