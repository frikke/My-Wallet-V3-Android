package com.blockchain.coincore.impl.txEngine

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NeedsApprovalException
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.txEngine.fiat.FiatDepositTxEngine
import com.blockchain.coincore.impl.txEngine.fiat.WITHDRAW_LOCKS
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.testutil.USD
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.NON_CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankPartnerCallbackProvider
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.SettlementInfo
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.domain.paymentmethods.model.SettlementType
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.testutils.eur
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import java.security.InvalidParameterException
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString

class FiatDepositTxEngineTest : CoincoreTestBase() {

    private lateinit var subject: FiatDepositTxEngine
    private val walletManager: CustodialWalletManager = mock()
    private val bankService: BankService = mock()
    private val withdrawalLocksRepository: WithdrawLocksRepository = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val plaidFeatureFlag: FeatureFlag = mock()
    private val limits = PaymentLimits(
        min = 100.toBigInteger(),
        max = 1000.toBigInteger(),
        currency = TEST_USER_FIAT
    )

    private val limitsDataManager: LimitsDataManager = mock {
        on { getLimits(any(), any(), any(), any(), any(), any()) }.thenReturn(
            Single.just(
                TxLimits(
                    min = TxLimit.Limited(limits.min),
                    max = TxLimit.Limited(limits.max),
                    periodicLimits = emptyList(),
                    suggestedUpgrade = null
                )
            )
        )
    }

    private val userIdentity: UserIdentity = mock {
        on { isVerifiedFor(Feature.TierLevel(KycTier.GOLD)) }.thenReturn(Single.just(true))
    }

    @Before
    fun setup() {
        initMocks()
        subject = FiatDepositTxEngine(
            walletManager = walletManager,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            userIdentity = userIdentity,
            withdrawLocksRepository = withdrawalLocksRepository,
            limitsDataManager = limitsDataManager,
            bankService = bankService,
            plaidFeatureFlag = plaidFeatureFlag
        )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when target account incorrect`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: CryptoAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        whenever(walletManager.getBankTransferLimits(TEST_USER_FIAT, true))
            .thenReturn(Single.just(limits))

        whenever(
            withdrawalLocksRepository.getWithdrawLockTypeForPaymentMethod(
                paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                fiatCurrency = TEST_USER_FIAT
            )
        ).thenReturn(Single.just(BigInteger.TEN))

        val sourceAccount: LinkedBankAccount = mock {
            on { currency }.thenReturn(TEST_USER_FIAT)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TEST_USER_FIAT)

        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeForFullAvailable == zeroFiat &&
                    it.feeAmount == zeroFiat &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.fromAmounts(min = limits.min, max = limits.max) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.containsKey(WITHDRAW_LOCKS) &&
                    it.engineState.containsKey("PAYMENT_METHOD_LIMITS")
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(walletManager).getBankTransferLimits(TEST_USER_FIAT, true)
        verify(limitsDataManager).getLimits(
            outputCurrency = eq(TEST_USER_FIAT),
            sourceCurrency = eq(TEST_USER_FIAT),
            targetCurrency = eq(TGT_ASSET),
            sourceAccountType = eq(NON_CUSTODIAL_LIMITS_ACCOUNT),
            targetAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            legacyLimits = any()
        )
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection()
        )

        val inputAmount = FiatValue.fromMinor(TGT_ASSET, 1000L.toBigInteger())

        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeAmount == zeroFiat
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
    }

    @Test
    fun `validate amount when pendingTx uninitialised`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            validationState = ValidationState.UNINITIALISED,
            totalBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            availableBalance = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection()
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeAmount == zeroFiat
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
    }

    @Test
    fun `validate amount when limits not set`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 1000L.toBigInteger())
        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertError { it is MissingLimitsException }
            .assertNotComplete()
    }

    @Test
    fun `validate amount when under min limit`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 1000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNDER_MIN_LIMIT
            }
    }

    @Test
    fun `validate amount when over max limit`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 1000000.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000.toBigInteger())
        val maxPaymentMethodLimit = FiatValue.fromMinor(TGT_ASSET, 100000.toBigInteger())
        val maxDepositLimit = FiatValue.fromMinor(TGT_ASSET, 10000.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            engineState = mapOf(
                "PAYMENT_METHOD_LIMITS" to TxLimits.fromAmounts(minLimit, maxPaymentMethodLimit)
            ),
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxDepositLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.ABOVE_PAYMENT_METHOD_LIMIT
            }
    }

    @Test
    fun `validate amount when correct`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNotComplete()
            .assertError { it is MissingLimitsException }
    }

    @Test
    fun `executing tx works`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YAPILY)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val txId = "12234"
        whenever(
            bankService.startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
        ).thenReturn(
            Single.just(txId)
        )
        whenever(bankService.getLinkedBankLegacy(ACCOUNT_ID)).thenReturn(Single.just(linkedBank))
        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(false))

        subject.doExecute(
            pendingTx,
            ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.HashedTxResult &&
                    it.txId == txId
            }

        verify(bankService).startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
    }

    @Test
    fun `executing tx throws exception`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YAPILY)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val exception = IllegalStateException("")
        whenever(
            bankService.startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
        ).thenReturn(
            Single.error(exception)
        )
        whenever(bankService.getLinkedBankLegacy(ACCOUNT_ID)).thenReturn(Single.just(linkedBank))
        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(false))

        subject.doExecute(
            pendingTx,
            ""
        ).test()
            .assertError {
                it == exception
            }

        verify(bankService).startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
    }

    @Test
    fun `doExecute() check Settlement for Plaid accounts with settlementType REGULAR should start bank transfer`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.REGULAR)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.PLAID)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val txId = "12234"
        whenever(
            bankService.startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
        ).thenReturn(
            Single.just(txId)
        )
        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(
            pendingTx,
            ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.HashedTxResult &&
                    it.txId == txId
            }

        verify(bankService).startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
    }

    @Test
    fun `doExecute() check Settlement for Plaid accounts with settlementReason NONE should start bank transfer`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.UNAVAILABLE)
            on { settlementReason }.thenReturn(SettlementReason.NONE)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.PLAID)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val txId = "12234"
        whenever(
            bankService.startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
        ).thenReturn(
            Single.just(txId)
        )
        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(
            pendingTx,
            ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.HashedTxResult &&
                    it.txId == txId
            }

        verify(bankService).startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET.networkTicker)
    }

    @Test
    fun `doExecute() check Settlement for Plaid accounts with settlementReason GENERIC should throw error`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.UNAVAILABLE)
            on { settlementReason }.thenReturn(SettlementReason.GENERIC)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.PLAID)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(pendingTx, "")
            .test()
            .assertError {
                it is TransactionError.SettlementGenericError
            }

        verify(bankService, times(0)).startBankTransfer(anyString(), any(), anyString(), any())
    }

    @Test
    fun `doExecute() check Settlement for Plaid accounts with INSUFFICIENT_BALANCE should throw error`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.UNAVAILABLE)
            on { settlementReason }.thenReturn(SettlementReason.INSUFFICIENT_BALANCE)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.PLAID)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(pendingTx, "")
            .test()
            .assertError {
                it is TransactionError.SettlementInsufficientBalance
            }

        verify(bankService, times(0)).startBankTransfer(anyString(), any(), anyString(), any())
    }

    @Test
    fun `doExecute() check Settlement for Plaid accounts with STALE_BALANCE should throw error`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.UNAVAILABLE)
            on { settlementReason }.thenReturn(SettlementReason.STALE_BALANCE)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.PLAID)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(pendingTx, "")
            .test()
            .assertError {
                it is TransactionError.SettlementStaleBalance
            }

        verify(bankService, times(0)).startBankTransfer(anyString(), any(), anyString(), any())
    }

    @Test
    fun `doExecute() check Settlement for Plaid accounts with REQUIRES_UPDATE should throw error`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.UNAVAILABLE)
            on { settlementReason }.thenReturn(SettlementReason.REQUIRES_UPDATE)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.PLAID)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(pendingTx, "")
            .test()
            .assertError {
                it is TransactionError.SettlementRefreshRequired
            }

        verify(bankService, times(0)).startBankTransfer(anyString(), any(), anyString(), any())
    }

    @Test
    fun `doExecute() check Settlement for Yodlee accounts with REQUIRES_UPDATE should throw error`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
            on { accountId }.thenReturn(ACCOUNT_ID)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }
        val settlement: SettlementInfo = mock {
            on { settlementType }.thenReturn(SettlementType.REGULAR)
            on { settlementReason }.thenReturn(SettlementReason.REQUIRES_UPDATE)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YODLEE)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.checkSettlement(bankAccountAddress.address, amount)).thenReturn(Single.just(settlement))
        whenever(bankService.getLinkedBankLegacy(bankAccountAddress.address)).thenReturn(Single.just(linkedBank))

        subject.doExecute(pendingTx, "")
            .test()
            .assertError {
                it is TransactionError.SettlementRefreshRequired
            }

        verify(bankService, times(0)).startBankTransfer(anyString(), any(), anyString(), any())
    }

    @Test
    fun `doPostExecute() - poll for OpenBanking with valid authUrl should return NeedsApprovalException`() {
        // Arrange
        val fiatValue = 10.eur()
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(true)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YAPILY)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { authorisationUrl }.thenReturn(AUTH_URL)
            on { id }.thenReturn(ACCOUNT_ID)
            on { amount }.thenReturn(fiatValue)
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(bankService.getLinkedBankLegacy(ACCOUNT_ID)).thenReturn(Single.just(linkedBank))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertError {
                (it as NeedsApprovalException).bankPaymentData == BankPaymentApproval(
                    paymentId = TX_ID,
                    authorisationUrl = AUTH_URL,
                    linkedBank = linkedBank,
                    orderValue = fiatValue
                )
            }
    }

    @Test
    fun `doPostExecute() - poll for OpenBanking with error should return FiatDepositError`() {
        // Arrange
        val errorCode = "errorCode"
        val fiatValue = 10.eur()
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(true)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YAPILY)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { id }.thenReturn(ACCOUNT_ID)
            on { amount }.thenReturn(fiatValue)
            on { status }.thenReturn(BankTransferStatus.Error(errorCode))
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(bankService.getLinkedBankLegacy(ACCOUNT_ID)).thenReturn(Single.just(linkedBank))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertError {
                (it as TransactionError.FiatDepositError).errorCode == errorCode
            }
    }

    @Test
    fun `doPostExecute() - poll for OpenBanking with missing authUrl should throw InvalidParameterException`() {
        // Arrange
        val fiatValue = 10.eur()
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(true)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YAPILY)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { authorisationUrl }.thenReturn(AUTH_URL, null)
            on { id }.thenReturn(ACCOUNT_ID)
            on { amount }.thenReturn(fiatValue)
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(bankService.getLinkedBankLegacy(ACCOUNT_ID)).thenReturn(Single.just(linkedBank))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertError {
                it is InvalidParameterException
            }
    }

    @Test
    fun `doPostExecute() - poll for Plaid with COMPLETE status should complete`() {
        // Arrange
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(false)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Complete)
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertComplete()
    }

    @Test
    fun `doPostExecute() - poll for Plaid with UNKNOWN status should complete`() {
        // Arrange
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(false)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Unknown)
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertComplete()
    }

    @Test
    fun `doPostExecute() - poll for Plaid with ERROR should return FiatDepositError`() {
        // Arrange
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(false)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Error(ERROR))
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertError {
                (it as TransactionError.FiatDepositError).errorCode == ERROR
            }
    }

    @Test
    fun `doPostExecute() - poll for Plaid with missing ERROR should complete`() {
        // Arrange
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(false)
        }
        val txResult: TxResult.HashedTxResult = mock {
            on { txId }.thenReturn(TX_ID)
        }
        val bankTransferDetails: BankTransferDetails = mock {
            on { status }.thenReturn(BankTransferStatus.Error(null))
        }

        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(bankService.getBankTransferCharge(TX_ID)).thenReturn(Single.just(bankTransferDetails))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), txResult)

        // Assert
        result.test()
            .assertComplete()
    }

    @Test
    fun `doPostExecute() - if account is not OpenBanking or Plaid, should complete`() {
        // Arrange
        val txTarget: FiatAccount = mock()
        val sourceAccount: LinkedBankAccount = mock {
            on { accountId }.thenReturn(ACCOUNT_ID)
            on { isOpenBankingCurrency() }.thenReturn(false)
        }
        val linkedBank: LinkedBank = mock {
            on { partner }.thenReturn(BankPartner.YODLEE)
        }
        whenever(plaidFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(bankService.getLinkedBankLegacy(ACCOUNT_ID)).thenReturn(Single.just(linkedBank))

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        val result = subject.doPostExecute(mock(), mock())

        // Assert
        result.test()
            .assertComplete()
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection) =
        feeSelection.selectedLevel == FeeLevel.None &&
            feeSelection.availableLevels == setOf(FeeLevel.None) &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.customAmount == -1L &&
            feeSelection.asset == null

    companion object {
        private val TGT_ASSET = USD
        private const val ACCOUNT_ID = "accountId"
        private const val TX_ID = "txId"
        private const val AUTH_URL = "authUrl"
        private const val ERROR = "error"
    }
}
