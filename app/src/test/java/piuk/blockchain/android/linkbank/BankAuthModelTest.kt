package piuk.blockchain.android.linkbank

import com.blockchain.android.testutils.rxInit
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankAuthError
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.BankLinkingProcessState
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankTransferAction
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.LinkedBankErrorState
import com.blockchain.domain.paymentmethods.model.LinkedBankState
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RefreshBankInfo
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.network.PollResult
import com.blockchain.testutils.GBP
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.ui.linkbank.BankAuthIntent
import piuk.blockchain.android.ui.linkbank.BankAuthModel
import piuk.blockchain.android.ui.linkbank.BankAuthState

class BankAuthModelTest {

    private lateinit var model: BankAuthModel
    private val interactor: SimpleBuyInteractor = mock()
    private val bankService: BankService = mock()
    private var defaultState = BankAuthState(id = "123")
    private val accountProviderId = "123"
    private val accountId = "1234"
    private val linkingBankId = "12345"
    private val linkBankTransfer =
        LinkBankTransfer(linkingBankId, BankPartner.YAPILY, YapilyAttributes(entity = "entity", listOf()))
    private val source = BankAuthSource.SIMPLE_BUY

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        model = BankAuthModel(
            interactor = interactor,
            bankService = bankService,
            initialState = defaultState,
            uiScheduler = Schedulers.io(),
            environmentConfig = mock(),
            remoteLogger = mock()
        )
    }

    @Test
    fun orderCancellation_ok() {
        whenever(interactor.cancelOrder(any())).thenReturn(Completable.complete())

        val test = model.state.test()
        model.process(BankAuthIntent.CancelOrder)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            BankAuthState(
                bankLinkingProcessState = BankLinkingProcessState.CANCELED
            )
        )
    }

    @Test
    fun orderCancellation_error() {
        whenever(interactor.cancelOrder(any())).thenReturn(Completable.error(Exception()))

        val test = model.state.test()
        model.process(BankAuthIntent.CancelOrder)

        test.assertValueAt(0, defaultState)

        test.assertValueAt(
            1,
            defaultState.copy(
                errorState = BankAuthError.GenericError
            )
        )
    }

    @Test
    fun bankLinkUpdate_ok() {
        val intent = BankAuthIntent.UpdateAccountProvider(
            accountProviderId = accountProviderId,
            accountId = accountId,
            linkingBankId = linkingBankId,
            linkBankTransfer = linkBankTransfer,
            authSource = source
        )

        whenever(
            interactor.updateSelectedBankAccountId(
                linkingId = intent.linkingBankId,
                providerAccountId = intent.accountProviderId,
                accountId = intent.accountId,
                partner = intent.linkBankTransfer.partner,
                action = BankTransferAction.LINK,
                source = source
            )
        ).thenReturn(Completable.complete())

        whenever(
            interactor.pollForLinkedBankState(
                partner = any(),
                id = any()
            )
        ).thenReturn(
            Single.just(
                PollResult.FinalResult(
                    LinkedBank(
                        id = linkingBankId,
                        currency = GBP,
                        partner = BankPartner.YAPILY,
                        accountName = "name", bankName = "bankName",
                        accountNumber = "123",
                        state = LinkedBankState.BLOCKED,
                        errorStatus = LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED,
                        accountType = "",
                        authorisationUrl = "url",
                        sortCode = "123",
                        accountIban = "123",
                        bic = "123",
                        entity = "entity",
                        iconUrl = "iconUrl",
                        callbackPath = ""
                    )
                )
            )
        )

        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                linkBankTransfer = linkBankTransfer,
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )

        verify(interactor).pollForLinkedBankState(intent.linkingBankId, BankPartner.YAPILY)
    }

    @Test
    fun bankLinkUpdate_error() {
        val intent = BankAuthIntent.UpdateAccountProvider(
            accountProviderId = accountProviderId,
            accountId = accountId,
            linkingBankId = linkingBankId,
            linkBankTransfer = linkBankTransfer,
            authSource = source
        )

        whenever(
            interactor.updateSelectedBankAccountId(
                linkingId = intent.linkingBankId,
                providerAccountId = intent.accountProviderId,
                accountId = intent.accountId,
                partner = intent.linkBankTransfer.partner,
                action = BankTransferAction.LINK,
                source = source
            )
        ).thenReturn(Completable.error(Exception()))

        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                linkBankTransfer = linkBankTransfer,
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                linkBankTransfer = linkBankTransfer,
                bankLinkingProcessState = BankLinkingProcessState.NONE,
                errorState = BankAuthError.BankLinkingUpdateFailed
            )
        )
    }

    @Test
    fun getLinkedBankState_active_from_deeplink() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.ACTIVE,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING_SUCCESS,
                linkedBank = expectedBank,
                selectedPaymentMethod = SelectedPaymentMethod(
                    id = expectedBank.id,
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    label = expectedBank.accountName,
                    isEligible = true
                )
            )
        )
    }

    @Test
    fun getLinkedBankState_active_not_from_deeplink() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, false)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.ACTIVE,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.IN_EXTERNAL_FLOW
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING_SUCCESS,
                linkedBank = expectedBank,
                selectedPaymentMethod = SelectedPaymentMethod(
                    id = expectedBank.id,
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    label = expectedBank.accountName,
                    isEligible = true
                )
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_already_linked() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.ACCOUNT_ALREADY_LINKED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankAlreadyLinked,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_unknown() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.UNKNOWN,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.BankLinkingFailed,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_account_unsupported() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name", bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankAccountUnsupported,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_name_mismatch() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.NAMES_MISMATCHED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankNamesMismatched,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_name_rejected() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.REJECTED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankRejected,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_name_expired() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.EXPIRED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankExpired,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_failure() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.FAILURE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankFailure,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_name_none_state_blocked() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.BankLinkingFailed,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_blocked_name_none_state_other() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.ACTIVE,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )
    }

    @Test
    fun getLinkedBankState_pending() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.PENDING,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )

        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.BankLinkingTimeout,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun getLinkedBankState_error() {
        val intent = BankAuthIntent.GetLinkedBankState(linkingBankId, true)

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.error(Exception()))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.ACTIVATING
            )
        )

        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.BankLinkingFailed,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun `linkPlaidAccount() - success`() {
        val linkBankAccountId = "linkBankAccountId"
        val linkBankToken = "linkBankToken"
        val linkBankTransfer: LinkBankTransfer = mock()
        val bankAuthSource: BankAuthSource = mock()

        val intent = BankAuthIntent.LinkPlaidAccount(
            linkingBankId,
            linkBankAccountId,
            linkBankToken,
            linkBankTransfer,
            bankAuthSource
        )

        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.PLAID,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.PENDING,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.just(expectedBank))
        whenever(bankService.linkPlaidBankAccount(linkingBankId, linkBankAccountId, linkBankToken)).thenReturn(
            Completable.complete()
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                id = linkingBankId,
                bankLinkingProcessState = BankLinkingProcessState.LINKING,
                linkBankTransfer = linkBankTransfer,
                linkBankAccountId = linkBankAccountId,
                linkBankToken = linkBankToken
            )
        )
    }

    @Test
    fun `linkPlaidAccount() - error`() {
        val linkBankAccountId = "linkBankAccountId"
        val linkBankToken = "linkBankToken"
        val linkBankTransfer: LinkBankTransfer = mock()
        val bankAuthSource: BankAuthSource = mock()

        val intent = BankAuthIntent.LinkPlaidAccount(
            linkingBankId,
            linkBankAccountId,
            linkBankToken,
            linkBankTransfer,
            bankAuthSource
        )

        whenever(bankService.linkPlaidBankAccount(linkingBankId, linkBankAccountId, linkBankToken)).thenReturn(
            Completable.error(Exception())
        )
        whenever(interactor.pollForBankLinkingCompleted(linkingBankId)).thenReturn(Single.error(Exception()))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                id = linkingBankId,
                bankLinkingProcessState = BankLinkingProcessState.LINKING,
                linkBankTransfer = linkBankTransfer,
                linkBankAccountId = linkBankAccountId,
                linkBankToken = linkBankToken
            )
        )

        test.assertValueAt(
            2,
            defaultState.copy(
                id = linkingBankId,
                linkBankTransfer = linkBankTransfer,
                linkBankAccountId = linkBankAccountId,
                linkBankToken = linkBankToken,
                errorState = BankAuthError.BankLinkingUpdateFailed,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun `refreshPlaidAccount() - success`() {
        // Arrange
        val bankAccountId = "refreshBankAccountId"
        val intent = BankAuthIntent.RefreshPlaidAccount(bankAccountId)
        val refreshBankAccountInfo = RefreshBankInfo(
            partner = BankPartner.PLAID,
            id = bankAccountId,
            linkToken = "linkToken",
            linkUrl = "linkUrl",
            tokenExpiresAt = "tokenExpiresAt"
        )
        whenever(bankService.refreshPlaidBankAccount(bankAccountId)).thenReturn(Single.just(refreshBankAccountInfo))

        // Act
        val test = model.state.test()
        model.process(intent)

        // Assert
        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                refreshBankAccountId = bankAccountId,
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                refreshBankAccountId = bankAccountId,
                refreshBankInfo = refreshBankAccountInfo,
                bankLinkingProcessState = BankLinkingProcessState.IN_REFRESH_FLOW
            )
        )
    }

    @Test
    fun `refreshPlaidAccount() - error`() {
        // Arrange
        val bankAccountId = "refreshBankAccountId"
        val intent = BankAuthIntent.RefreshPlaidAccount(bankAccountId)
        whenever(bankService.refreshPlaidBankAccount(bankAccountId)).thenReturn(Single.error(Throwable()))

        // Act
        val test = model.state.test()
        model.process(intent)

        // Assert
        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                refreshBankAccountId = bankAccountId,
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                refreshBankAccountId = bankAccountId,
                errorState = BankAuthError.LinkedBankFailure,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_active() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name", bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.ACTIVE,
            errorStatus = LinkedBankErrorState.NONE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING_SUCCESS,
                linkedBank = expectedBank,
                selectedPaymentMethod = SelectedPaymentMethod(
                    id = expectedBank.id,
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    label = expectedBank.accountName,
                    isEligible = true
                )
            )
        )
    }

    @Test
    fun pollLinkStatus_blocked_already_linked() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.ACCOUNT_ALREADY_LINKED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankAlreadyLinked,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_blocked_unknown() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.UNKNOWN,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.BankLinkingFailed,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_not_info_account() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name", bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.NOT_INFO_FOUND,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankInfoNotFound,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_fraud_account() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name", bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.FRAUD,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankFraud,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_internal_failure_account() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name", bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.INTERNAL_FAILURE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankInternalFailure,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_blocked_name_mismatch() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.NAMES_MISMATCHED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankNamesMismatched,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_blocked_rejected() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.REJECTED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankRejected,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_blocked_expired() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.EXPIRED,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankExpired,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_blocked_failure() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)
        val expectedBank = LinkedBank(
            id = linkingBankId,
            currency = GBP,
            partner = BankPartner.YAPILY,
            accountName = "name",
            bankName = "bankName",
            accountNumber = "123",
            state = LinkedBankState.BLOCKED,
            errorStatus = LinkedBankErrorState.FAILURE,
            accountType = "",
            authorisationUrl = "url",
            sortCode = "123",
            accountIban = "123",
            bic = "123",
            entity = "entity",
            iconUrl = "iconUrl",
            callbackPath = ""
        )

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.just(PollResult.FinalResult(expectedBank))
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.LinkedBankFailure,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    @Test
    fun pollLinkStatus_error() {
        setupModelWithBankPartner()

        val intent = BankAuthIntent.StartPollingForLinkStatus(linkingBankId)

        whenever(interactor.pollForLinkedBankState(intent.bankId, BankPartner.YAPILY)).thenReturn(
            Single.error(Exception())
        )
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0, defaultState)
        test.assertValueAt(
            1,
            defaultState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
        )
        test.assertValueAt(
            2,
            defaultState.copy(
                errorState = BankAuthError.BankLinkingFailed,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
        )
    }

    private fun setupModelWithBankPartner() {
        defaultState = defaultState.copy(linkBankTransfer = linkBankTransfer)

        model = BankAuthModel(
            interactor = interactor,
            bankService = bankService,
            initialState = defaultState,
            uiScheduler = Schedulers.io(),
            environmentConfig = mock(),
            remoteLogger = mock()
        )
    }
}
