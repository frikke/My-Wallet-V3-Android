package piuk.blockchain.android.ui.settings.v2

import com.blockchain.core.Database
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.Bank
import com.blockchain.nabu.datamanagers.BankState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EligiblePaymentMethodType
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.settings.LinkablePaymentMethods
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class SettingsInteractor internal constructor(
    private val userIdentity: UserIdentity,
    private val database: Database,
    private val credentialsWiper: CredentialsWiper,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs
) {
    private val userSelectedFiat by lazy {
        currencyPrefs.selectedFiatCurrency
    }

    private val eligiblePaymentMethodsForSession: Single<List<EligiblePaymentMethodType>> by lazy {
        custodialWalletManager.getEligiblePaymentMethodTypes(userSelectedFiat).cache()
    }

    fun getUserFiat() = userSelectedFiat

    fun getSupportEligibilityAndBasicInfo(): Single<UserDetails> =
        Singles.zip(
            userIdentity.getHighestApprovedKycTier(),
            userIdentity.getBasicProfileInformation()
        ).map { (tier, basicInfo) ->
            UserDetails(userTier = tier, userInfo = basicInfo)
        }

    fun unpairWallet(): Completable = Completable.fromAction {
        credentialsWiper.wipe()
        database.historicRateQueries.clear()
    }

    fun getExistingPaymentMethods(): Single<PaymentMethods> =
        Singles.zip(
            canLinkPaymentMethods(),
            getLinkedCards(),
            getBankDetails().map { (linkableBanks, linkedBanks) ->
                linkedBanks.getEligibleLinkedBanks(linkableBanks)
            }
        ).map { (eligiblePaymentMethods, linkedCards, linkedBanks) ->
            PaymentMethods(
                eligiblePaymentMethods = eligiblePaymentMethods,
                linkedCards = linkedCards,
                linkedBanks = linkedBanks
            )
        }

    fun getBankLinkingInfo(): Single<LinkBankTransfer> =
        custodialWalletManager.linkToABank(userSelectedFiat)

    private fun canLinkPaymentMethods(): Single<Map<PaymentMethodType, Boolean>> =
        eligiblePaymentMethodsForSession
            .map { eligiblePaymentMethods ->
                mapOf(
                    PaymentMethodType.BANK_ACCOUNT to eligiblePaymentMethods.any {
                        it.paymentMethodType == PaymentMethodType.BANK_ACCOUNT
                    },
                    PaymentMethodType.BANK_TRANSFER to eligiblePaymentMethods.any {
                        it.paymentMethodType == PaymentMethodType.BANK_TRANSFER
                    },
                    PaymentMethodType.PAYMENT_CARD to eligiblePaymentMethods.any {
                        it.paymentMethodType == PaymentMethodType.PAYMENT_CARD
                    }
                )
            }

    private fun getLinkedCards(): Single<List<PaymentMethod.Card>> =
        eligiblePaymentMethodsForSession.map { eligiblePaymentMethods ->
            eligiblePaymentMethods.firstOrNull { it.paymentMethodType == PaymentMethodType.PAYMENT_CARD } != null
        }
            .flatMap { isCardEligible ->
                if (isCardEligible) {
                    custodialWalletManager.updateSupportedCardTypes(userSelectedFiat)
                        .thenSingle {
                            custodialWalletManager.fetchUnawareLimitsCards(
                                listOf(CardStatus.ACTIVE, CardStatus.EXPIRED)
                            )
                        }
                } else {
                    Single.just(emptyList())
                }
            }

    private fun getBankDetails(): Single<Pair<Set<LinkablePaymentMethods>, Set<Bank>>> =
        eligibleBankPaymentMethods().zipWith(linkedBanks().onErrorReturn { emptySet() })

    private fun Set<Bank>.getEligibleLinkedBanks(linkableBanks: Set<LinkablePaymentMethods>): Set<Bank> {
        val paymentMethod = linkableBanks.filter { it.currency == userSelectedFiat }.distinct().firstOrNull()
        val eligibleLinkedBanks = this.filter { paymentMethod?.linkMethods?.contains(it.paymentMethodType) ?: false }

        return this.map {
            it.copy(
                canBeUsedToTransact = eligibleLinkedBanks.contains(it)
            )
        }.toSet()
    }

    private fun linkedBanks(): Single<Set<Bank>> =
        custodialWalletManager.getBanks().map { banks ->
            banks.filter { it.state == BankState.ACTIVE }
        }.map { banks ->
            banks.toSet()
        }

    private fun eligibleBankPaymentMethods(): Single<Set<LinkablePaymentMethods>> =
        eligiblePaymentMethodsForSession.map { methods ->
            val bankPaymentMethods = methods.filter {
                it.paymentMethodType == PaymentMethodType.BANK_TRANSFER ||
                    it.paymentMethodType == PaymentMethodType.BANK_ACCOUNT
            }

            bankPaymentMethods.map { method ->
                LinkablePaymentMethods(
                    method.currency,
                    bankPaymentMethods.filter { it.currency == method.currency }.map { it.paymentMethodType }.distinct()
                )
            }.toSet()
        }
}
