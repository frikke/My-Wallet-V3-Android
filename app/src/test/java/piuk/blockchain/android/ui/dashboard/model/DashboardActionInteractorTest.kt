package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.testutils.USD
import com.blockchain.walletmode.WalletMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods

class DashboardActionInteractorTest {

    private lateinit var actionInteractor: DashboardActionInteractor
    private val linkedBanksFactory: LinkedBanksFactory = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val bankService: BankService = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val userIdentity: NabuUserIdentity = mock()
    private val nftWaitlistService: NftWaitlistService = mock()
    private val nftAnnouncementPrefs: NftAnnouncementPrefs = mock()
    private val model: DashboardModel = mock()
    private val targetFiatAccount: FiatAccount = mock {
        on { currency }.thenReturn(USD)
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
        mainTrampoline()
    }

    @Before
    fun setUp() {
        actionInteractor = DashboardActionInteractor(
            coincore = mock(),
            payloadManager = mock(),
            custodialWalletManager = custodialWalletManager,
            linkedBanksFactory = linkedBanksFactory,
            remoteLogger = mock(),
            analytics = mock(),
            simpleBuyPrefs = mock(),
            currencyPrefs = currencyPrefs,
            onboardingPrefs = mock(),
            userIdentity = userIdentity,
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            },
            getDashboardOnboardingStepsUseCase = mock(),
            nftWaitlistService = nftWaitlistService,
            nftAnnouncementPrefs = nftAnnouncementPrefs,
            exchangeRates = mock(),
            walletModeBalanceCache = mock(),
            bankService = bankService
        )
    }

    @Test
    fun `for both available methods with no available bank transfer banks, chooser should be triggered`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositFiat))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(
            DashboardIntent.ShowLinkablePaymentMethodsSheet(
                targetFiatAccount,
                LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                    LinkablePaymentMethods(
                        USD,
                        listOf(
                            PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `for only bank transfer available with no available bank transfer banks, bank link should launched`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_TRANSFER)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )
        whenever(bankService.linkBank(USD)).thenReturn(
            Single.just(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                )
            )
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositFiat))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(
            DashboardIntent.LaunchBankLinkFlow(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                ),
                targetFiatAccount,
                AssetAction.FiatDeposit
            )
        )
    }

    @Test
    fun `for only funds with no available bank transfer banks, wire transfer should launched`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositFiat))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(
            DashboardIntent.ShowBankLinkingSheet(targetFiatAccount)
        )
    }

    @Test
    fun `with 1 available bank transfer, flow should be launched and wire transfer should get ignored`() {
        val linkedBankAccount: LinkedBankAccount = mock {
            on { currency }.thenReturn(USD)
            on { type }.thenReturn(PaymentMethodType.BANK_TRANSFER)
        }
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT, PaymentMethodType.BANK_TRANSFER)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                listOf(
                    linkedBankAccount
                )
            )
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositFiat))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        verify(model).process(any<DashboardIntent.UpdateNavigationAction>())
    }

    @Test
    fun `if linked bank should launched then wire transfer should get ignored and link bank should be launched`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT, PaymentMethodType.BANK_TRANSFER)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositFiat))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        whenever(bankService.linkBank(USD)).thenReturn(
            Single.just(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                )
            )
        )

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = true
        )

        verify(model).process(
            DashboardIntent.LaunchBankLinkFlow(
                LinkBankTransfer(
                    "123", BankPartner.YODLEE, YodleeAttributes("", "", "")
                ),
                targetFiatAccount,
                AssetAction.FiatDeposit
            )
        )
    }

    @Test
    fun `if deposit fiat is blocked, blocked due to sanctions sheet should be shown`() {
        whenever(linkedBanksFactory.eligibleBankPaymentMethods(any())).thenReturn(
            Single.just(
                setOf(PaymentMethodType.BANK_ACCOUNT)
            )
        )
        whenever(linkedBanksFactory.getNonWireTransferBanks()).thenReturn(
            Single.just(
                emptyList()
            )
        )
        whenever(userIdentity.userAccessForFeature(Feature.DepositFiat))
            .thenReturn(Single.just(FeatureAccess.Blocked(BlockedReason.Sanctions.RussiaEU5)))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false
        )

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateNavigationAction
                val action = intent?.action as? DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions
                action?.reason is BlockedReason.Sanctions.RussiaEU5
            }
        )
    }

    @Test
    fun `loading profile then check getHighestApprovedKycTier`() {
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(
            Single.just(Tier.GOLD)
        )
        actionInteractor.canDeposit().test()

        verify(userIdentity).getHighestApprovedKycTier()
        verifyNoMoreInteractions(userIdentity)
    }
}
