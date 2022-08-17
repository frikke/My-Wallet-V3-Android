package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.QuestionnaireNode
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CowboysPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.testutils.USD
import com.blockchain.walletmode.WalletMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.cowboys.CowboysPromoDataProvider
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class DashboardActionInteractorTest {

    private lateinit var actionInteractor: DashboardActionInteractor
    private val linkedBanksFactory: LinkedBanksFactory = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val bankService: BankService = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val userIdentity: NabuUserIdentity = mock()
    private val kycService: KycService = mock()
    private val dataRemediationService: DataRemediationService = mock()
    private val nftWaitlistService: NftWaitlistService = mock()
    private val nftAnnouncementPrefs: NftAnnouncementPrefs = mock()
    private val model: DashboardModel = mock()
    private val targetFiatAccount: FiatAccount = mock {
        on { currency }.thenReturn(USD)
    }
    private val referralPrefs: ReferralPrefs = mock()
    private val cowboysFeatureFlag: FeatureFlag = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val cowboysDataProvider: CowboysPromoDataProvider = mock()
    private val referralService: ReferralService = mock()
    private val cowboysPrefs: CowboysPrefs = mock()

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
            kycService = kycService,
            dataRemediationService = dataRemediationService,
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            },
            getDashboardOnboardingStepsUseCase = mock(),
            nftWaitlistService = nftWaitlistService,
            nftAnnouncementPrefs = nftAnnouncementPrefs,
            exchangeRates = mock(),
            walletModeBalanceCache = mock(),
            bankService = bankService,
            referralPrefs = referralPrefs,
            cowboysFeatureFlag = cowboysFeatureFlag,
            settingsDataManager = settingsDataManager,
            cowboysDataProvider = cowboysDataProvider,
            referralService = referralService,
            cowboysPrefs = cowboysPrefs
        )
    }

    @Test
    fun `for both available methods with no available bank transfer banks, chooser should be triggered`() = runTest {
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
        whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
            .thenReturn(Outcome.Success(null))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = false
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
    fun `for only bank transfer available with no available bank transfer banks, bank link should launched`() =
        runTest {
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
            whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
                .thenReturn(Outcome.Success(null))

            actionInteractor.getBankDepositFlow(
                model = model,
                targetAccount = targetFiatAccount,
                action = AssetAction.FiatDeposit,
                shouldLaunchBankLinkTransfer = false,
                shouldSkipQuestionnaire = false
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
    fun `for only funds with no available bank transfer banks, wire transfer should launched`() = runTest {
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
        whenever(userIdentity.isArgentinian()).thenReturn(Single.just(false))
        whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
            .thenReturn(Outcome.Success(null))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = false
        )

        verify(model).process(
            DashboardIntent.ShowBankLinkingSheet(targetFiatAccount)
        )
    }

    @Test
    fun `when Argentinian users deposit, wire transfer should launched`() {
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
        whenever(userIdentity.isArgentinian()).thenReturn(Single.just(true))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = true
        )

        verify(model).process(
            DashboardIntent.ShowBankLinkingSheet(targetFiatAccount)
        )
    }

    @Test
    fun `when Argentinian users with no linked banks withdraw, the alias link flow should be launched`() {
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
        whenever(userIdentity.isArgentinian()).thenReturn(Single.just(true))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatWithdraw,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = true
        )

        verify(model).process(
            DashboardIntent.ShowBankLinkingWithAlias(targetFiatAccount)
        )
    }

    @Test
    fun `with 1 available bank transfer, flow should be launched and wire transfer should get ignored`() = runTest {
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
        whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
            .thenReturn(Outcome.Success(null))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = false
        )

        verify(model).process(any<DashboardIntent.UpdateNavigationAction>())
    }

    @Test
    fun `if linked bank should launched then wire transfer should get ignored and link bank should be launched`() =
        runTest {
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
            whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
                .thenReturn(Outcome.Success(null))

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
                shouldLaunchBankLinkTransfer = true,
                shouldSkipQuestionnaire = false
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
    fun `if deposit fiat is blocked, blocked due to sanctions sheet should be shown`() = runTest {
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
        whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
            .thenReturn(Outcome.Success(null))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = false
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
    fun `if deposit fiat requires filling a questionnaire, questionnaire should be shown`() = runTest {
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
        val questionnaire = Questionnaire(
            header = null,
            context = QuestionnaireContext.FIAT_DEPOSIT,
            nodes = listOf(
                QuestionnaireNode.Selection("s1", "text1", emptyList(), false),
                QuestionnaireNode.Selection("s2", "text2", emptyList(), false),
            ),
            isMandatory = true
        )
        whenever(dataRemediationService.getQuestionnaire(QuestionnaireContext.FIAT_DEPOSIT))
            .thenReturn(Outcome.Success(questionnaire))

        actionInteractor.getBankDepositFlow(
            model = model,
            targetAccount = targetFiatAccount,
            action = AssetAction.FiatDeposit,
            shouldLaunchBankLinkTransfer = false,
            shouldSkipQuestionnaire = false
        )

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateNavigationAction
                val action = intent?.action as? DashboardNavigationAction.DepositQuestionnaire
                action?.questionnaire == questionnaire &&
                    action.callbackIntent == DashboardIntent.LaunchBankTransferFlow(
                    account = targetFiatAccount,
                    action = AssetAction.FiatDeposit,
                    shouldLaunchBankLinkTransfer = false,
                    shouldSkipQuestionnaire = true
                )
            }
        )
    }

    @Test
    fun `loading profile then check getHighestApprovedKycTier`() {
        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(
            Single.just(KycTier.GOLD)
        )
        actionInteractor.canDeposit().test()

        verify(kycService).getHighestApprovedTierLevelLegacy()
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `given cowboys check when flag is off then view state is hidden`() {
        whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))

        actionInteractor.checkCowboysFlowSteps(model)

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateCowboysViewState
                intent?.cowboysState is DashboardCowboysState.Hidden
            }
        )

        verify(cowboysFeatureFlag).enabled
        verify(userIdentity).isCowboysUser()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `given cowboys check when user not tagged then view state is hidden`() {
        whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(false))

        actionInteractor.checkCowboysFlowSteps(model)

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateCowboysViewState
                intent?.cowboysState is DashboardCowboysState.Hidden
            }
        )

        verify(cowboysFeatureFlag).enabled
        verify(userIdentity).isCowboysUser()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `given cowboys check when flag is on, user is tagged but email is not verified then view state is email check`() {
        whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))

        val settings: Settings = mock()
        whenever(settings.isEmailVerified).thenReturn(false)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        val data = PromotionStyleInfo(
            "title", "message", "", "", "", emptyList(), emptyList()
        )
        whenever(cowboysDataProvider.getWelcomeAnnouncement()).thenReturn(Single.just(data))

        actionInteractor.checkCowboysFlowSteps(model)

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateCowboysViewState
                intent?.cowboysState is DashboardCowboysState.CowboyWelcomeCard &&
                    (intent.cowboysState as DashboardCowboysState.CowboyWelcomeCard).cardInfo == data
            }
        )

        verify(cowboysFeatureFlag).enabled
        verify(userIdentity).isCowboysUser()
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `given cowboys check when flag is on, user is tagged, email verified but bronze kyc then view state is verify SDD`() {
        whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))
        val data = PromotionStyleInfo(
            "title", "message", "", "", "", emptyList(), emptyList()
        )
        whenever(cowboysDataProvider.getRaffleAnnouncement()).thenReturn(Single.just(data))

        val settings: Settings = mock()

        whenever(settings.isEmailVerified).thenReturn(true)

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.BRONZE))

        actionInteractor.checkCowboysFlowSteps(model)

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateCowboysViewState
                intent?.cowboysState is DashboardCowboysState.CowboyRaffleCard &&
                    (intent.cowboysState as DashboardCowboysState.CowboyRaffleCard).cardInfo == data
            }
        )

        verify(cowboysFeatureFlag).enabled
        verify(userIdentity).isCowboysUser()
        verify(kycService).getHighestApprovedTierLevelLegacy()
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `given cowboys check when flag is on, user is tagged, email verified but silver kyc then view state is verify gold`() {
        whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))
        val settings: Settings = mock()
        whenever(settings.isEmailVerified).thenReturn(true)
        val data = PromotionStyleInfo(
            "title", "message", "", "", "", emptyList(), emptyList()
        )
        whenever(cowboysDataProvider.getIdentityAnnouncement()).thenReturn(Single.just(data))

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.SILVER))

        actionInteractor.checkCowboysFlowSteps(model)

        val captor = argumentCaptor<DashboardIntent>()
        verify(model, atLeastOnce()).process(captor.capture())
        assert(
            captor.allValues.any {
                val intent = it as? DashboardIntent.UpdateCowboysViewState
                intent?.cowboysState is DashboardCowboysState.CowboyIdentityCard &&
                    (intent.cowboysState as DashboardCowboysState.CowboyIdentityCard).cardInfo == data
            }
        )

        verify(cowboysFeatureFlag).enabled
        verify(userIdentity).isCowboysUser()
        verify(kycService).getHighestApprovedTierLevelLegacy()
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `given cowboys check when flag is on, user is tagged, email verified, gold kyc and view previously dismissed then view state is hidden`() =
        runTest {
            val settings: Settings = mock()

            whenever(settings.isEmailVerified).thenReturn(true)
            whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(true))
            whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))
            whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
            whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.GOLD))
            whenever(cowboysPrefs.hasCowboysReferralBeenDismissed).thenReturn(true)

            val referralInfo: ReferralInfo.Data = mock()
            whenever(referralService.fetchReferralData()).thenReturn(Outcome.Success(referralInfo))

            val cowboysData: PromotionStyleInfo = mock()
            whenever(cowboysDataProvider.getReferFriendsAnnouncement()).thenReturn(Single.just(cowboysData))

            actionInteractor.checkCowboysFlowSteps(model)

            val captor = argumentCaptor<DashboardIntent>()
            verify(model, atLeastOnce()).process(captor.capture())
            assert(
                captor.allValues.any {
                    val intent = it as? DashboardIntent.UpdateCowboysViewState
                    intent?.cowboysState is DashboardCowboysState.Hidden
                }
            )

            verify(cowboysFeatureFlag).enabled
            verify(userIdentity).isCowboysUser()
            verify(kycService).getHighestApprovedTierLevelLegacy()
            verify(settingsDataManager).getSettings()
            verify(cowboysPrefs).hasCowboysReferralBeenDismissed
            verifyNoMoreInteractions(settingsDataManager)
            verifyNoMoreInteractions(userIdentity)
            verifyNoMoreInteractions(cowboysDataProvider)
            verifyNoMoreInteractions(referralService)
            verifyNoMoreInteractions(cowboysPrefs)
        }

    @Test
    fun `given cowboys check when flag is on, user is tagged, email verified, gold kyc and not previously dismissed then view state is referral`() =
        runTest {
            val settings: Settings = mock()

            whenever(settings.isEmailVerified).thenReturn(true)
            whenever(cowboysFeatureFlag.enabled).thenReturn(Single.just(true))
            whenever(userIdentity.isCowboysUser()).thenReturn(Single.just(true))
            whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
            whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.GOLD))
            whenever(cowboysPrefs.hasCowboysReferralBeenDismissed).thenReturn(false)

            val referralInfo: ReferralInfo.Data = mock()
            whenever(referralService.fetchReferralData()).thenReturn(Outcome.Success(referralInfo))

            val cowboysData: PromotionStyleInfo = mock()
            whenever(cowboysDataProvider.getReferFriendsAnnouncement()).thenReturn(Single.just(cowboysData))

            actionInteractor.checkCowboysFlowSteps(model)

            val captor = argumentCaptor<DashboardIntent>()
            verify(model, atLeastOnce()).process(captor.capture())
            assert(
                captor.allValues.any {
                    val intent = it as? DashboardIntent.UpdateCowboysViewState
                    intent?.cowboysState is DashboardCowboysState.CowboyReferFriendsCard &&
                        (intent.cowboysState as DashboardCowboysState.CowboyReferFriendsCard)
                        .referralData == referralInfo &&
                        (intent.cowboysState as DashboardCowboysState.CowboyReferFriendsCard).cardInfo == cowboysData
                }
            )

            verify(cowboysFeatureFlag).enabled
            verify(userIdentity).isCowboysUser()
            verify(kycService).getHighestApprovedTierLevelLegacy()
            verify(settingsDataManager).getSettings()
            verify(cowboysDataProvider).getReferFriendsAnnouncement()
            verify(referralService).fetchReferralData()
            verify(cowboysPrefs).hasCowboysReferralBeenDismissed
            verifyNoMoreInteractions(settingsDataManager)
            verifyNoMoreInteractions(userIdentity)
            verifyNoMoreInteractions(cowboysDataProvider)
            verifyNoMoreInteractions(referralService)
            verifyNoMoreInteractions(cowboysPrefs)
        }
}
