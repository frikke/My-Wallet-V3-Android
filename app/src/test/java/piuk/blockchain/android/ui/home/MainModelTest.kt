package piuk.blockchain.android.ui.home

import android.content.Intent
import com.blockchain.android.testutils.rxInit
import com.blockchain.api.IconData
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.api.NabuUxErrorResponse
import com.blockchain.api.StatusData
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.coincore.AssetAction
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.enumValueOfOrNull
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.network.PollResult
import com.blockchain.testutils.EUR
import com.blockchain.utils.capitalizeFirstChar
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.google.gson.JsonSyntaxException
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.deeplink.BlockchainLinkState
import piuk.blockchain.android.deeplink.LinkState
import piuk.blockchain.android.deeplink.OpenBankingLinkType
import piuk.blockchain.android.kyc.KycLinkState
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.home.models.MainIntent
import piuk.blockchain.android.ui.home.models.MainInteractor
import piuk.blockchain.android.ui.home.models.MainModel
import piuk.blockchain.android.ui.home.models.MainState
import piuk.blockchain.android.ui.home.models.ReferralState
import piuk.blockchain.android.ui.home.models.ViewToLaunch
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.sell.BuySellFragment
import retrofit2.HttpException
import retrofit2.Response

class MainModelTest {
    private lateinit var model: MainModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }
    private val interactor: MainInteractor = mock {
        on { checkReferral() }.thenReturn(Single.just(ReferralState(ReferralInfo.NotAvailable)))
    }
    private val walletConnectServiceAPI: WalletConnectServiceAPI = mock {
        on { sessionEvents }.thenReturn(Observable.empty())
    }

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = MainModel(
            initialState = MainState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletConnectServiceAPI = walletConnectServiceAPI,
            interactor = interactor,
        )
    }

    @Test
    fun performInitialCheckNoErrors() {
        whenever(interactor.checkForUserWalletErrors()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(MainIntent.PerformInitialChecks)

        testState.assertValue(MainState())
    }

    @Test
    fun performInitialCheckThrowsErrors() {
        val walletId = "12345"

        whenever(interactor.checkForUserWalletErrors()).thenReturn(
            Completable.error(
                NabuApiExceptionFactory.fromResponseBody(
                    HttpException(
                        Response.error<Unit>(
                            500,
                            "{ \"description\": \"${NabuApiException.USER_WALLET_LINK_ERROR_PREFIX}$walletId\" }"
                                .toResponseBody()
                        )
                    )
                )
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.PerformInitialChecks)

        testState
            .assertValueAt(0) {
                it == MainState()
            }.assertValueAt(1) {
                it.viewToLaunch is ViewToLaunch.CheckForAccountWalletLinkErrors &&
                    (it.viewToLaunch as ViewToLaunch.CheckForAccountWalletLinkErrors).walletIdHint == walletId
            }
    }

    @Test
    fun checkForPendingLinksKyc_Resubmit() {
        val mockIntent: Intent = mock()
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.KycDeepLink(
                    link = KycLinkState.Resubmit
                )
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchKyc &&
                (it.viewToLaunch as ViewToLaunch.LaunchKyc).campaignType == CampaignType.Resubmission
        }
    }

    @Test
    fun checkForPendingLinksKyc_Verified() {
        val mockIntent: Intent = mock()
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.KycDeepLink(
                    link = KycLinkState.EmailVerified
                )
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchKyc &&
                (it.viewToLaunch as ViewToLaunch.LaunchKyc).campaignType == CampaignType.None
        }
    }

    @Test
    fun checkForPendingLinksKyc_General_nullData() {
        val mockIntent: Intent = mock()
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.KycDeepLink(
                    link = KycLinkState.General(
                        null
                    )
                )
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchKyc &&
                (it.viewToLaunch as ViewToLaunch.LaunchKyc).campaignType == CampaignType.None
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Linking_Complete() {
        val mockIntent: Intent = mock()
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.LINK_BANK, "")
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_LINK_COMPLETE)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }

        verify(interactor).checkForDeepLinks(mockIntent)
        verify(interactor).getBankLinkingState()
        verify(interactor).resetLocalBankAuthState()
        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun checkForPendingLinksOpenBanking_Linking_InProgress_Success() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.LINK_BANK, consentToken)
            )
        )

        val bankLinkingInfoMock: BankLinkingInfo = mock()
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_LINK_PENDING)
            on { bankLinkingInfo }.thenReturn(bankLinkingInfoMock)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())

        val expectedUpdatedState = bankState.copy(
            bankAuthFlow = BankAuthFlowState.BANK_LINK_COMPLETE
        )
        doNothing().whenever(interactor).updateBankLinkingState(expectedUpdatedState)

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingLinking &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingLinking).bankLinkingInfo == bankLinkingInfoMock
        }

        verify(interactor).checkForDeepLinks(mockIntent)
        verify(interactor).getBankLinkingState()
        verify(interactor).updateOpenBankingConsent(consentToken)
        verify(interactor).updateBankLinkingState(expectedUpdatedState)
        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun checkForPendingLinksOpenBanking_Linking_InProgress_Success_Exception() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.LINK_BANK, consentToken)
            )
        )

        val bankLinkingInfoMock: BankLinkingInfo = mock()
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_LINK_PENDING)
            on { bankLinkingInfo }.thenReturn(bankLinkingInfoMock)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())

        val expectedUpdatedState = bankState.copy(
            bankAuthFlow = BankAuthFlowState.BANK_LINK_COMPLETE
        )
        whenever(interactor.updateBankLinkingState(expectedUpdatedState)).thenThrow(JsonSyntaxException("test error"))

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.ShowOpenBankingError
        }

        verify(interactor).checkForDeepLinks(mockIntent)
        verify(interactor).getBankLinkingState()
        verify(interactor).updateOpenBankingConsent(consentToken)
        verify(interactor).updateBankLinkingState(expectedUpdatedState)
        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun checkForPendingLinksOpenBanking_Linking_InProgress_Error_LocalData() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.LINK_BANK, consentToken)
            )
        )

        val bankLinkingInfoMock: BankLinkingInfo = mock()
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_LINK_PENDING)
            on { bankLinkingInfo }.thenReturn(bankLinkingInfoMock)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingLinking &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingLinking).bankLinkingInfo == bankLinkingInfoMock
        }

        verify(interactor).checkForDeepLinks(mockIntent)
        verify(interactor).getBankLinkingState()
        verify(interactor).updateOpenBankingConsent(consentToken)
        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun checkForPendingLinksOpenBanking_Linking_InProgress_Error_No_LocalData() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.LINK_BANK, consentToken)
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_LINK_PENDING)
            on { bankLinkingInfo }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.ShowOpenBankingError
        }

        verify(interactor).checkForDeepLinks(mockIntent)
        verify(interactor).getBankLinkingState()
        verify(interactor).updateOpenBankingConsent(consentToken)
        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_Complete() {
        val mockIntent: Intent = mock()
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, "")
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_COMPLETE)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }

        verify(interactor).checkForDeepLinks(mockIntent)
        verify(interactor).getBankLinkingState()
        verify(interactor).resetLocalBankAuthState()
        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Consent_Success_Poll_Success_Transfer_Complete() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val transferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Complete)
            on { amount }.thenReturn(FiatValue.zero(EUR))
        }
        whenever(interactor.pollForBankTransferCharge(paymentData)).thenReturn(
            Single.just(PollResult.FinalResult(transferDetails))
        )
        doNothing().whenever(interactor).updateBankLinkingState(any())
        val estimatedCompletionTime = "121212"
        whenever(interactor.getEstimatedDepositCompletionTime()).thenReturn(estimatedCompletionTime)
        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress)
                .value == paymentData.orderValue
        }.assertValueAt(2) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositComplete &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositComplete)
                .amount == transferDetails.amount &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositComplete)
                .estimatedDepositCompletionTime == estimatedCompletionTime
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Consent_Success_Poll_Success_Transfer_Pending() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val transferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Pending)
            on { amount }.thenReturn(FiatValue.zero(EUR))
        }
        whenever(interactor.pollForBankTransferCharge(paymentData)).thenReturn(
            Single.just(PollResult.FinalResult(transferDetails))
        )
        doNothing().whenever(interactor).updateBankLinkingState(any())

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress)
                .value == paymentData.orderValue
        }.assertValueAt(2) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalTimeout &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalTimeout)
                .currencyCode == transferDetails.amount.currencyCode
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Consent_Success_Poll_Success_Transfer_Error() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val transferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Error())
            on { amount }.thenReturn(FiatValue.zero(EUR))
        }
        whenever(interactor.pollForBankTransferCharge(paymentData)).thenReturn(
            Single.just(PollResult.FinalResult(transferDetails))
        )
        doNothing().whenever(interactor).updateBankLinkingState(any())

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress)
                .value == paymentData.orderValue
        }.assertValueAt(2) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingError &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingError)
                .currencyCode == transferDetails.amount.currencyCode
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Success_Poll_Timeout() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val transferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Error())
            on { amount }.thenReturn(FiatValue.zero(EUR))
        }
        whenever(interactor.pollForBankTransferCharge(paymentData)).thenReturn(
            Single.just(PollResult.TimeOut(transferDetails))
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress)
                .value == paymentData.orderValue
        }.assertValueAt(2) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalTimeout &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalTimeout)
                .currencyCode == paymentData.orderValue.currencyCode
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Success_Poll_Error() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        whenever(interactor.pollForBankTransferCharge(paymentData)).thenReturn(
            Single.error(Exception("test"))
        )
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress)
                .value == paymentData.orderValue
        }.assertValueAt(2) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingError &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingError)
                .currencyCode == paymentData.orderValue.currencyCode
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Success_Poll_NabuException_Error() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        val nabuUxErrorResponse = NabuUxErrorResponse(
            "title",
            "message",
            IconData(
                "iconUrl",
                StatusData(
                    "statusUrl"
                )
            ),
            actions = emptyList(),
            categories = emptyList()

        )

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        whenever(interactor.pollForBankTransferCharge(paymentData)).thenReturn(
            Single.error(NabuApiExceptionFactory.fromServerSideError(nabuUxErrorResponse))
        )
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress)
                .value == paymentData.orderValue
        }.assertValueAt(2) {
            it.viewToLaunch is ViewToLaunch.LaunchServerDrivenOpenBankingError &&
                (it.viewToLaunch as ViewToLaunch.LaunchServerDrivenOpenBankingError)
                .currencyCode == paymentData.orderValue.currencyCode &&
                (it.viewToLaunch as ViewToLaunch.LaunchServerDrivenOpenBankingError)
                .title == nabuUxErrorResponse.title &&
                (it.viewToLaunch as ViewToLaunch.LaunchServerDrivenOpenBankingError)
                .description == nabuUxErrorResponse.message
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_NoLocalData_SimpleBuy_State_Exists_Await_Funds() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val mockSBState: SimpleBuyState = mock {
            on { orderState }.thenReturn(OrderState.AWAITING_FUNDS)
        }
        whenever(interactor.getSimpleBuySyncLocalState()).thenReturn(mockSBState)
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_NoLocalData_SimpleBuy_State_Exists_Funds_Exist() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val mockSBState: SimpleBuyState = mock {
            on { orderState }.thenReturn(OrderState.FINISHED)
        }
        whenever(interactor.getSimpleBuySyncLocalState()).thenReturn(mockSBState)
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchPaymentForCancelledOrder &&
                (it.viewToLaunch as ViewToLaunch.LaunchPaymentForCancelledOrder).state == mockSBState
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_NoLocalData_SimpleBuy_Sync_Success_Await_Funds() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())
        val mockSBState: SimpleBuyState = mock {
            on { orderState }.thenReturn(OrderState.AWAITING_FUNDS)
        }
        whenever(interactor.getSimpleBuySyncLocalState())
            .thenReturn(null)
            .thenReturn(mockSBState)
        whenever(interactor.performSimpleBuySync()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_NoLocalData_SimpleBuy_Sync_Error() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())

        whenever(interactor.getSimpleBuySyncLocalState())
            .thenReturn(null)
            .thenReturn(null)

        whenever(interactor.performSimpleBuySync()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingBuyApprovalError
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_NoLocalData_SimpleBuy_Sync_Success_Funds_Exist() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.complete())

        val mockSBState: SimpleBuyState = mock {
            on { orderState }.thenReturn(OrderState.FINISHED)
        }
        whenever(interactor.getSimpleBuySyncLocalState())
            .thenReturn(null)
            .thenReturn(mockSBState)

        whenever(interactor.performSimpleBuySync()).thenReturn(Completable.complete())
        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchPaymentForCancelledOrder &&
                (it.viewToLaunch as ViewToLaunch.LaunchPaymentForCancelledOrder).state == mockSBState
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Error_LocalData() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val paymentData: BankPaymentApproval = mock {
            on { orderValue }.thenReturn(FiatValue.zero(EUR))
        }
        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(paymentData)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.error(Exception("test")))

        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingError &&
                (it.viewToLaunch as ViewToLaunch.LaunchOpenBankingError)
                .currencyCode == paymentData.orderValue.currencyCode
        }
    }

    @Test
    fun checkForPendingLinksOpenBanking_Approval_InProgress_Error_No_LocalData() {
        val mockIntent: Intent = mock()
        val consentToken = "1234"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, consentToken)
            )
        )

        val bankState: BankAuthDeepLinkState = mock {
            on { bankAuthFlow }.thenReturn(BankAuthFlowState.BANK_APPROVAL_PENDING)
            on { bankPaymentData }.thenReturn(null)
        }

        whenever(interactor.getBankLinkingState()).thenReturn(bankState)
        whenever(interactor.updateOpenBankingConsent(consentToken)).thenReturn(Completable.error(Exception("test")))

        doNothing().whenever(interactor).resetLocalBankAuthState()

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchOpenBankingBuyApprovalError
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Swap() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Swap)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchSwap
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_TwoFa() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.TwoFa)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchTwoFaSetup
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_VerifyEmail() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.VerifyEmail)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchVerifyEmail
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Biometrics() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.SetupFingerprint)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchSetupBiometricLogin
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Interest() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Interest)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchInterestDashboard
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Receive() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Receive)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchReceive
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Send() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Send)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchSend
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Sell() {
        val mockIntent: Intent = mock()
        val ticker = "BTC"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Sell(ticker))
            )
        )

        val mockAssetInfo: AssetInfo = mock()
        whenever(interactor.getAssetFromTicker(ticker)).thenReturn(mockAssetInfo)

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchBuySell &&
                (it.viewToLaunch as ViewToLaunch.LaunchBuySell).type == BuySellFragment.BuySellViewType.TYPE_SELL &&
                (it.viewToLaunch as ViewToLaunch.LaunchBuySell).asset == mockAssetInfo
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Activity() {
        val mockIntent: Intent = mock()

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Activities)
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchAssetAction &&
                (it.viewToLaunch as ViewToLaunch.LaunchAssetAction).action == AssetAction.ViewActivity
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Buy() {
        val mockIntent: Intent = mock()
        val ticker = "BTC"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.Buy(ticker))
            )
        )

        val mockAssetInfo: AssetInfo = mock()
        whenever(interactor.getAssetFromTicker(ticker)).thenReturn(mockAssetInfo)

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchBuySell &&
                (it.viewToLaunch as ViewToLaunch.LaunchBuySell).type == BuySellFragment.BuySellViewType.TYPE_BUY &&
                (it.viewToLaunch as ViewToLaunch.LaunchBuySell).asset == mockAssetInfo
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_SimpleBuy() {
        val mockIntent: Intent = mock()
        val ticker = "BTC"
        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.SimpleBuy(ticker))
            )
        )

        val mockAssetInfo: AssetInfo = mock()
        whenever(interactor.getAssetFromTicker(ticker)).thenReturn(mockAssetInfo)

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchSimpleBuy &&
                (it.viewToLaunch as ViewToLaunch.LaunchSimpleBuy).asset == mockAssetInfo
        }
    }

    @Test
    fun checkForPendingLinksBlockchain_Kyc() {
        val mockIntent: Intent = mock()
        val campaignType = "interest"

        whenever(interactor.checkForDeepLinks(mockIntent)).thenReturn(
            Single.just(
                LinkState.BlockchainLink(BlockchainLinkState.KycCampaign(campaignType))
            )
        )

        val testState = model.state.test()
        model.process(MainIntent.CheckForPendingLinks(mockIntent))

        val expectedType = enumValueOfOrNull<CampaignType>(campaignType.capitalizeFirstChar())

        testState.assertValueAt(0) {
            it == MainState()
        }.assertValueAt(1) {
            it.viewToLaunch is ViewToLaunch.LaunchKyc &&
                (it.viewToLaunch as ViewToLaunch.LaunchKyc).campaignType == expectedType
        }
    }

    @Test
    fun unpairWallet() {
        whenever(interactor.unpairWallet()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(MainIntent.UnpairWallet)

        testState
            .assertValues(
                MainState()
            )
    }

    @Test
    fun checkForReferralCode() {
        model.process(MainIntent.CheckReferralCode)

        val testState = model.state.test()
        testState.assertValue(
            MainState(referral = ReferralState(ReferralInfo.NotAvailable))
        )
    }

    @Test
    fun updateReferralClickedState() {
        val mockReferralInfo: ReferralInfo = mock()
        whenever(interactor.checkReferral()).doReturn(Single.just(ReferralState(mockReferralInfo, false)))

        model.process(MainIntent.CheckReferralCode)
        model.process(MainIntent.ReferralIconClicked)

        val testState = model.state.test()
        testState.assertValue(
            MainState(referral = ReferralState(mockReferralInfo, true))
        )
    }

    @Test
    fun markReferralStateWhenLaunchedFromReferralDeeplink() {
        model.process(MainIntent.ShowReferralWhenAvailable)

        val testState = model.state.test()
        testState.assertValue(
            MainState(referral = ReferralState(ReferralInfo.NotAvailable, referralDeeplink = true))
        )
    }

    @Test
    fun showReferralStateWhenLaunchedFromReferralDeeplink() {
        val mockReferralInfo: ReferralInfo = mock()
        whenever(interactor.checkReferral()).doReturn(Single.just(ReferralState(mockReferralInfo, false)))

        model.process(MainIntent.ShowReferralWhenAvailable)
        model.process(MainIntent.CheckReferralCode)

        val testState = model.state.test()
        testState.assertValue(
            MainState(
                referral = ReferralState(mockReferralInfo, referralDeeplink = false),
                viewToLaunch = ViewToLaunch.ShowReferralSheet
            )
        )
    }
}
