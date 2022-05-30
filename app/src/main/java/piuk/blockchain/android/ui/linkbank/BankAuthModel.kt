package piuk.blockchain.android.ui.linkbank

import com.blockchain.api.NabuApiException
import com.blockchain.banking.BankTransferAction
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.LinkedBankErrorState
import com.blockchain.domain.paymentmethods.model.LinkedBankState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.RemoteLogger
import com.blockchain.network.PollResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor

class BankAuthModel(
    private val interactor: SimpleBuyInteractor,
    initialState: BankAuthState,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<BankAuthState, BankAuthIntent>(
    initialState,
    uiScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(previousState: BankAuthState, intent: BankAuthIntent): Disposable? =
        when (intent) {
            is BankAuthIntent.CancelOrder,
            is BankAuthIntent.CancelOrderAndResetAuthorisation -> (
                previousState.id?.let {
                    interactor.cancelOrder(it)
                } ?: Completable.complete()
                )
                .subscribeBy(
                    onComplete = {
                        process(BankAuthIntent.OrderCanceled)
                    },
                    onError = {
                        process(BankAuthIntent.ErrorIntent())
                    }
                )
            is BankAuthIntent.UpdateAccountProvider -> processBankLinkingUpdate(intent)
            is BankAuthIntent.GetLinkedBankState -> processBankLinkStateUpdate(intent)
            is BankAuthIntent.StartPollingForLinkStatus -> processLinkStatusPolling(
                intent, previousState.linkBankTransfer?.partner
            )
            is BankAuthIntent.StartBankApproval -> {
                interactor.updateApprovalStatus(intent.callbackPath)
                null
            }
            else -> null
        }

    private fun processBankLinkingUpdate(intent: BankAuthIntent.UpdateAccountProvider) =
        interactor.updateSelectedBankAccountId(
            linkingId = intent.linkingBankId,
            providerAccountId = intent.accountProviderId,
            accountId = intent.accountId,
            partner = intent.linkBankTransfer.partner,
            source = intent.authSource,
            action = BankTransferAction.LINK
        ).subscribeBy(
            onComplete = {
                process(BankAuthIntent.StartPollingForLinkStatus(intent.linkingBankId))
            },
            onError = {
                process(BankAuthIntent.ProviderAccountIdUpdateError)
            }
        )

    private fun processBankLinkStateUpdate(intent: BankAuthIntent.GetLinkedBankState) =
        interactor.pollForBankLinkingCompleted(
            intent.linkingBankId
        ).subscribeBy(
            onSuccess = {
                when (it.state) {
                    LinkedBankState.ACTIVE -> process(BankAuthIntent.LinkedBankStateSuccess(it))
                    LinkedBankState.BLOCKED,
                    LinkedBankState.UNKNOWN -> handleBankLinkingError(it)
                    LinkedBankState.PENDING,
                    LinkedBankState.CREATED -> process(
                        BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingTimeout)
                    )
                }
            },
            onError = {
                (it as? NabuApiException)?.getServerSideErrorInfo()?.let { info ->
                    process(
                        BankAuthIntent.BankAuthErrorState(
                            BankAuthError.ServerSideDrivenLinkedBankError(
                                title = info.title,
                                message = info.description,
                                iconUrl = info.iconUrl,
                                statusIconUrl = info.statusUrl
                            )
                        )
                    )
                } ?: process(BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingFailed))
            }
        )

    private fun processLinkStatusPolling(
        intent: BankAuthIntent.StartPollingForLinkStatus,
        partner: BankPartner?
    ) = Single.defer {
        interactor.pollForLinkedBankState(
            intent.bankId,
            partner
        )
    }
        .subscribeBy(
            onSuccess = {
                when (it) {
                    is PollResult.FinalResult -> {
                        interactor.updateOneTimeTokenPath(it.value.callbackPath)
                        updateIntentForLinkedBankState(it, partner)
                    }
                    is PollResult.Cancel -> {
                    }
                    is PollResult.TimeOut -> process(
                        BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingTimeout)
                    )
                }
            },
            onError = {
                process(BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingFailed))
            }
        )

    private fun updateIntentForLinkedBankState(
        pollResult: PollResult<LinkedBank>,
        partner: BankPartner?
    ) {
        when (pollResult.value.state) {
            LinkedBankState.ACTIVE -> {
                process(BankAuthIntent.LinkedBankStateSuccess(pollResult.value))
            }
            LinkedBankState.BLOCKED -> {
                handleBankLinkingError(pollResult.value)
            }
            LinkedBankState.PENDING,
            LinkedBankState.CREATED -> {
                when (partner) {
                    BankPartner.YODLEE -> process(
                        BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingTimeout)
                    )
                    BankPartner.YAPILY -> process(
                        BankAuthIntent.UpdateLinkingUrl(pollResult.value.authorisationUrl)
                    )
                }
            }
            LinkedBankState.UNKNOWN -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingFailed)
            )
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun handleBankLinkingError(it: LinkedBank) {
        when (it.errorStatus) {
            LinkedBankErrorState.ACCOUNT_ALREADY_LINKED -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankAlreadyLinked)
            )
            LinkedBankErrorState.UNKNOWN -> process(BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingFailed))
            LinkedBankErrorState.NOT_INFO_FOUND -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankInfoNotFound)
            )
            LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankAccountUnsupported)
            )
            LinkedBankErrorState.NAMES_MISMATCHED -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankNamesMismatched)
            )
            LinkedBankErrorState.REJECTED -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankRejected)
            )
            LinkedBankErrorState.EXPIRED -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankExpired)
            )
            LinkedBankErrorState.FAILURE -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankFailure)
            )
            LinkedBankErrorState.INTERNAL_FAILURE -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankInternalFailure)
            )
            LinkedBankErrorState.INVALID -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankInvalid)
            )
            LinkedBankErrorState.FRAUD -> process(
                BankAuthIntent.BankAuthErrorState(BankAuthError.LinkedBankFraud)
            )
            LinkedBankErrorState.NONE -> {
                // check the state is not a linking final state
                if (it.state == LinkedBankState.BLOCKED) {
                    process(BankAuthIntent.BankAuthErrorState(BankAuthError.BankLinkingFailed))
                } else {
                    // do nothing
                }
            }
        }.exhaustive
    }
}
