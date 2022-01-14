package piuk.blockchain.android.ui.home

import android.content.Intent
import com.blockchain.core.Database
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.ThePitLinkingPrefs
import com.blockchain.sunriver.XlmAccountReference
import com.blockchain.sunriver.XlmDataManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import exchangerate.HistoricRateQueries
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import piuk.blockchain.android.campaign.SunriverCampaignRegistration
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.scan.ScanResult
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.android.ui.home.models.MainInteractor
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import thepit.PitLinking

class MainInteractorTest {

    private lateinit var interactor: MainInteractor
    private val deepLinkProcessor: DeepLinkProcessor = mock()
    private val exchangeLinking: PitLinking = mock()
    private val exchangePrefs: ThePitLinkingPrefs = mock()
    private val assetCatalogue: AssetCatalogue = mock()
    private val xlmDataManager: XlmDataManager = mock()
    private val sunriverCampaignRegistration: SunriverCampaignRegistration = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val simpleBuySync: SimpleBuySyncFactory = mock()
    private val userIdentity: UserIdentity = mock()
    private val upsellManager: KycUpgradePromptManager = mock()
    private val database: Database = mock()
    private val credentialsWiper: CredentialsWiper = mock()
    private val qrScanResultProcessor: QrScanResultProcessor = mock()
    private val secureChannelManager: SecureChannelManager = mock()
    private val cancelOrderUseCase: CancelOrderUseCase = mock()
    private val paymentsDataManager: PaymentsDataManager = mock()

    @Before
    fun setup() {
        interactor = MainInteractor(
            deepLinkProcessor = deepLinkProcessor,
            exchangeLinking = exchangeLinking,
            exchangePrefs = exchangePrefs,
            assetCatalogue = assetCatalogue,
            xlmDataManager = xlmDataManager,
            sunriverCampaignRegistration = sunriverCampaignRegistration,
            kycStatusHelper = kycStatusHelper,
            bankLinkingPrefs = bankLinkingPrefs,
            custodialWalletManager = custodialWalletManager,
            simpleBuySync = simpleBuySync,
            userIdentity = userIdentity,
            upsellManager = upsellManager,
            database = database,
            credentialsWiper = credentialsWiper,
            qrScanResultProcessor = qrScanResultProcessor,
            secureChannelManager = secureChannelManager,
            cancelOrderUseCase = cancelOrderUseCase,
            paymentsDataManager = paymentsDataManager
        )
    }

    @Test
    fun checkForDeepLink_Intent() {
        val intent: Intent = mock()

        interactor.checkForDeepLinks(intent)
        whenever(deepLinkProcessor.getLink(intent)).thenReturn(Single.just(mock()))
        verify(deepLinkProcessor).getLink(intent)
    }

    @Test
    @Ignore("Underlying Uri.parse is an android call")
    fun checkForDeepLink_ScanResult() {
        val url = "https://test.com?link=1234"
        val result = ScanResult.HttpUri(url, false)

        interactor.checkForDeepLinks(result)

        verify(deepLinkProcessor).getLink("1234")
    }

    @Test
    fun checkForWalletErrors() {
        interactor.checkForUserWalletErrors()
        whenever(userIdentity.checkForUserWalletLinkErrors()).thenReturn(Completable.complete())
        verify(userIdentity).checkForUserWalletLinkErrors()
    }

    @Test
    fun getExchangeLinkingState() {
        interactor.getExchangeLinkingState()
        verify(exchangeLinking).isPitLinked()
    }

    @Test
    fun getExchangeWalletLinkId() {
        val id = "1245"
        whenever(exchangePrefs.pitToWalletLinkId).thenReturn(id)
        val resultId = interactor.getExchangeToWalletLinkId()
        verify(exchangePrefs).pitToWalletLinkId
        Assert.assertEquals(id, resultId)
    }

    @Test
    fun getAssetFromTicker_Known_Ticker() {
        val assetInfo: AssetInfo = mock()
        val ticker = "BTC"
        whenever(assetCatalogue.assetInfoFromNetworkTicker(ticker)).thenReturn(assetInfo)

        val resultAsset = interactor.getAssetFromTicker(ticker)
        verify(assetCatalogue).assetInfoFromNetworkTicker(ticker)
        Assert.assertEquals(assetInfo, resultAsset)
    }

    @Test
    fun getAssetFromTicker_Unknown_Ticker() {
        val assetInfo: AssetInfo? = null
        val ticker = "BTC"
        whenever(assetCatalogue.assetInfoFromNetworkTicker(ticker)).thenReturn(null)

        val resultAsset = interactor.getAssetFromTicker(ticker)

        verify(assetCatalogue).assetInfoFromNetworkTicker(ticker)
        Assert.assertEquals(assetInfo, resultAsset)
    }

    @Test
    fun registerForCampaign() {
        val data: CampaignData = mock()
        val defaultAccount: XlmAccountReference = mock()
        val kycMock: KycState = mock()

        whenever(xlmDataManager.defaultAccount()).thenReturn(Single.just(defaultAccount))
        whenever(sunriverCampaignRegistration.registerCampaign(data)).thenReturn(Completable.complete())
        whenever(kycStatusHelper.getKycStatus()).thenReturn(Single.just(kycMock))

        val observer = interactor.registerForCampaign(data).test()
        observer.assertValue(kycMock)

        verify(xlmDataManager).defaultAccount()
        verify(sunriverCampaignRegistration).registerCampaign(data)
        verify(kycStatusHelper).getKycStatus()
    }

    @Test
    fun resetLocalBankState() {
        doNothing().whenever(bankLinkingPrefs).setBankLinkingState(any())
        interactor.resetLocalBankAuthState()
        verify(bankLinkingPrefs).setBankLinkingState(any())
    }

    @Test
    fun setLocalBankState() {
        doNothing().whenever(bankLinkingPrefs).setBankLinkingState(any())
        interactor.updateBankLinkingState(mock())
        verify(bankLinkingPrefs).setBankLinkingState(any())
    }

    @Test
    fun updateConsent_Success() {
        val consentToken = "1234"
        val tokenUrl = "token url"
        whenever(bankLinkingPrefs.getDynamicOneTimeTokenUrl()).thenReturn(tokenUrl)
        whenever(paymentsDataManager.updateOpenBankingConsent(tokenUrl, consentToken)).thenReturn(
            Completable.complete()
        )

        interactor.updateOpenBankingConsent(consentToken)

        verify(bankLinkingPrefs).getDynamicOneTimeTokenUrl()
        verify(paymentsDataManager).updateOpenBankingConsent(tokenUrl, consentToken)
        verifyNoMoreInteractions(bankLinkingPrefs)
        verifyNoMoreInteractions(custodialWalletManager)
    }

    @Test
    fun updateConsent_Error() {
        val consentToken = "1234"
        val tokenUrl = "token url"
        val exception = Exception("test")

        whenever(bankLinkingPrefs.getDynamicOneTimeTokenUrl()).thenReturn(tokenUrl)
        whenever(paymentsDataManager.updateOpenBankingConsent(tokenUrl, consentToken)).thenReturn(
            Completable.error(exception)
        )
        doNothing().whenever(bankLinkingPrefs).setBankLinkingState(any())

        val observer = interactor.updateOpenBankingConsent(consentToken).test()
        observer
            .assertNotComplete()
            .assertError(exception)

        verify(bankLinkingPrefs).getDynamicOneTimeTokenUrl()
        verify(bankLinkingPrefs).setBankLinkingState(any())
        verify(paymentsDataManager).updateOpenBankingConsent(tokenUrl, consentToken)
        verifyNoMoreInteractions(bankLinkingPrefs)
        verifyNoMoreInteractions(custodialWalletManager)
    }

    @Test
    fun unpairWallet() {
        val mockQueries: HistoricRateQueries = mock()

        doNothing().whenever(credentialsWiper).wipe()
        whenever(database.historicRateQueries).thenReturn(mockQueries)
        doNothing().whenever(mockQueries).clear()

        val observer = interactor.unpairWallet().test()
        observer.assertComplete()

        verify(credentialsWiper).wipe()
        verify(database.historicRateQueries).clear()
    }

    @Test
    fun simpleBuy_LocalState() {
        val sbState: SimpleBuyState = mock()
        whenever(simpleBuySync.currentState()).thenReturn(sbState)
        val result = interactor.getSimpleBuySyncLocalState()

        verify(simpleBuySync).currentState()
        Assert.assertEquals(sbState, result)
    }

    @Test
    fun simpleBuy_Sync() {
        whenever(simpleBuySync.performSync()).thenReturn(Completable.complete())
        val observer = interactor.performSimpleBuySync().test()
        observer.assertComplete()

        verify(simpleBuySync).performSync()
    }

    @Test
    fun cancelPendingOrder_No_Local_State() {
        whenever(simpleBuySync.currentState()).thenReturn(null)

        val observer = interactor.cancelAnyPendingConfirmationBuy().test()
        observer.assertComplete()

        verify(simpleBuySync).currentState()
        verifyNoMoreInteractions(custodialWalletManager)
        verifyNoMoreInteractions(simpleBuySync)
    }

    @Test
    fun cancelPendingOrder_Order_Not_Pending() {
        val sbState: SimpleBuyState = mock {
            on { it.orderState }.thenReturn(OrderState.FINISHED)
        }
        whenever(simpleBuySync.currentState()).thenReturn(sbState)

        interactor.cancelAnyPendingConfirmationBuy()

        verify(simpleBuySync).currentState()
        verifyNoMoreInteractions(custodialWalletManager)
        verifyNoMoreInteractions(simpleBuySync)
    }

    @Test
    fun cancelPendingOrder_Order_Delete_Success() {
        val orderId = "1235"
        val sbState: SimpleBuyState = mock {
            on { orderState }.thenReturn(OrderState.PENDING_CONFIRMATION)
            on { id }.thenReturn(orderId)
        }

        whenever(simpleBuySync.currentState()).thenReturn(sbState)
        whenever(custodialWalletManager.deleteBuyOrder(orderId)).thenReturn(Completable.complete())

        val observer = interactor.cancelAnyPendingConfirmationBuy().test()
        observer.assertComplete()

        verify(custodialWalletManager).deleteBuyOrder(orderId)
        verify(simpleBuySync).currentState()
        verify(simpleBuySync).clear()

        verifyNoMoreInteractions(custodialWalletManager)
        verifyNoMoreInteractions(simpleBuySync)
    }

    @Test
    fun cancelOrder() {
        // Arrange
        val orderId = "orderId"

        // Act
        interactor.cancelOrder(orderId)

        // Assert
        verify(cancelOrderUseCase).invoke(orderId)
    }
}
