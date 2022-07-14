package com.blockchain.coincore.impl

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.junit.Before
import org.junit.Test

class CryptoInterestAccountActionsTest : CoincoreTestBase() {

    private val custodialManager: CustodialWalletManager = mock()
    private val interestBalanceDataManager: InterestBalanceDataManager = mock()
    private val userIdentity: UserIdentity = mock()

    @Before
    fun setup() {
        whenever(exchangeRates.exchangeRateToUserFiat(TEST_ASSET))
            .thenReturn(Observable.just(TEST_TO_USER_RATE))
    }

    @Test
    fun `when user is bronze then actions are not returned`() {
        val subject = configureActionSubject()

        configureActionTest(Tier.BRONZE)

        subject.stateAwareActions
            .test().assertValue {
                it.isEmpty()
            }
    }

    @Test
    fun `when user is silver then actions are not returned`() {
        val subject = configureActionSubject()

        configureActionTest(Tier.SILVER)

        subject.stateAwareActions
            .test().assertValue {
                it.isEmpty()
            }
    }

    @Test
    fun `when user is gold but interest not enabled then Deposit is locked for availability`() {
        val subject = configureActionSubject()

        configureActionTest(
            tier = Tier.GOLD,
            isInterestEnabled = false,
            userAccessForFeature = FeatureAccess.Blocked(BlockedReason.NotEligible),
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN)
        )

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.InterestDeposit &&
                    it.elementAt(0).state == ActionState.Unavailable &&
                    it.elementAt(1).action == AssetAction.InterestWithdraw &&
                    it.elementAt(1).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.ViewStatement &&
                    it.elementAt(2).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.ViewActivity &&
                    it.elementAt(3).state == ActionState.Available
            }
    }

    @Test
    fun `when user is gold but interest eligibility is blocked Deposit is blocked`() {
        val subject = configureActionSubject()

        configureActionTest(
            tier = Tier.GOLD,
            isInterestEnabled = true,
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.TEN),
            userAccessForFeature = FeatureAccess.Blocked(BlockedReason.NotEligible)
        )

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.InterestDeposit &&
                    it.elementAt(0).state == ActionState.Unavailable &&
                    it.elementAt(1).action == AssetAction.InterestWithdraw &&
                    it.elementAt(1).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.ViewStatement &&
                    it.elementAt(2).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.ViewActivity &&
                    it.elementAt(3).state == ActionState.Available
            }
    }

    @Test
    fun `when user is gold and has no withdrawable balance then Withdraw is locked for balance`() {
        val subject = configureActionSubject()

        configureActionTest(
            tier = Tier.GOLD,
            isInterestEnabled = true,
            accountBalance = CryptoValue.fromMinor(TEST_ASSET, BigInteger.ZERO),
            userAccessForFeature = FeatureAccess.Granted()
        )

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.InterestDeposit &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(1).action == AssetAction.InterestWithdraw &&
                    it.elementAt(1).state == ActionState.LockedForBalance &&
                    it.elementAt(2).action == AssetAction.ViewStatement &&
                    it.elementAt(2).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.ViewActivity &&
                    it.elementAt(3).state == ActionState.Available
            }
    }

    @Test
    fun `when user is gold and all criteria match then all actions are available`() {
        val subject = configureActionSubject()

        configureActionTest(
            tier = Tier.GOLD,
            isInterestEnabled = true,
            accountBalance = CryptoValue.Companion.fromMinor(TEST_ASSET, BigInteger.TEN),
            userAccessForFeature = FeatureAccess.Granted()
        )

        subject.stateAwareActions
            .test().assertValue {
                it.elementAt(0).action == AssetAction.InterestDeposit &&
                    it.elementAt(0).state == ActionState.Available &&
                    it.elementAt(1).action == AssetAction.InterestWithdraw &&
                    it.elementAt(1).state == ActionState.Available &&
                    it.elementAt(2).action == AssetAction.ViewStatement &&
                    it.elementAt(2).state == ActionState.Available &&
                    it.elementAt(3).action == AssetAction.ViewActivity &&
                    it.elementAt(3).state == ActionState.Available
            }
    }

    private fun configureActionSubject(): CryptoInterestAccount =
        CryptoInterestAccount(
            label = "Test Account",
            currency = TEST_ASSET,
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            interestBalance = interestBalanceDataManager,
            identity = userIdentity,
            internalAccountLabel = "Trading Account"
        )

    private fun configureActionTest(
        tier: Tier,
        userAccessForFeature: FeatureAccess = FeatureAccess.Granted(),
        accountBalance: CryptoValue = CryptoValue.zero(TEST_ASSET),
        isInterestEnabled: Boolean = true,
        ineligibilityReason: IneligibilityReason = IneligibilityReason.NONE,
    ) {

        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(tier))
        whenever(userIdentity.userAccessForFeature(Feature.DepositInterest)).thenReturn(
            Single.just(userAccessForFeature)
        )

        val balance = InterestAccountBalance(
            totalBalance = accountBalance,
            pendingInterest = Money.zero(TEST_ASSET),
            pendingDeposit = Money.zero(TEST_ASSET),
            totalInterest = Money.zero(TEST_ASSET),
            lockedBalance = Money.zero(TEST_ASSET),
            hasTransactions = true
        )

        whenever(custodialManager.getInterestEligibilityForAsset(TEST_ASSET)).thenReturn(
            Single.just(Eligibility(isInterestEnabled, ineligibilityReason))
        )
        whenever(interestBalanceDataManager.getBalanceForAsset(TEST_ASSET))
            .thenReturn(Observable.just(balance))
    }

    companion object {
        private val TEST_ASSET = object : CryptoCurrency(
            displayTicker = "NOPE",
            networkTicker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}

        private val TEST_TO_USER_RATE = ExchangeRate(
            from = TEST_USER_FIAT,
            to = TEST_USER_FIAT,
            rate = 1.toBigDecimal()
        )
    }
}
