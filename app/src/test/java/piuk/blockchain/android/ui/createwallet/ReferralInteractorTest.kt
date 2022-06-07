package piuk.blockchain.android.ui.createwallet

import com.blockchain.android.testutils.rxInit
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralValidity
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReferralInteractorTest {

    private val referralService: ReferralService = mock()

    private lateinit var referralInteractor: ReferralInteractor

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        referralInteractor = ReferralInteractor(referralService)
    }

    @Test
    fun `should not validate when referral code empty`() {
        // Act
        val observer = referralInteractor.validateReferralIfNeeded("").test()

        // Assert
        observer.assertValue(ReferralCodeState.VALID)
        observer.assertComplete()
        observer.assertNoErrors()
        verifyZeroInteractions(referralService)
    }

    @Test
    fun `should validate VALID`(): Unit = runBlocking {
        // Arrange
        val code = "code"
        whenever(referralService.validateReferralCode(code)).thenReturn(Outcome.Success(ReferralValidity.VALID))

        // Act
        val observer = referralInteractor.validateReferralIfNeeded("code").test()

        // Assert
        observer.await()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(ReferralCodeState.VALID)
    }

    @Test
    fun `should validate INVALID`(): Unit = runBlocking {
        // Arrange
        val code = "code"
        whenever(referralService.validateReferralCode(code)).thenReturn(Outcome.Success(ReferralValidity.INVALID))

        // Act
        val observer = referralInteractor.validateReferralIfNeeded("code").test()

        // Assert
        observer.await()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(ReferralCodeState.INVALID)
    }

    @Test
    fun `should forward error from outcome`(): Unit = runBlocking {
        // Arrange
        val code = "code"
        val throwable = Throwable("Some error")
        whenever(referralService.validateReferralCode(code)).thenReturn(Outcome.Failure(throwable))

        // Act
        val observer = referralInteractor.validateReferralIfNeeded("code").test()

        // Assert
        observer.await()
        observer.assertError(throwable)
    }
}
