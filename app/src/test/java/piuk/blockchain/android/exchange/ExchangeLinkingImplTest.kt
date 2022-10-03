package piuk.blockchain.android.exchange

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExchangeLinkingImplTest {

    private val userService: UserService = mock()
    private val nabuUser: NabuUser = mock()

    private lateinit var subject: ExchangeLinkingImpl

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    @Before
    fun setup() {
        subject = ExchangeLinkingImpl(
            userService = userService
        )
    }

    @Test
    fun `fetch user data on subscribe, user is linked`() {
        // Arrange
        whenever(nabuUser.exchangeEnabled).thenReturn(true)
        whenever(userService.getUser()).thenReturn(Single.just(nabuUser))

        // Act
        val test = subject.state.test()

        // Assert
        verify(userService).getUser()

        test.assertValue { it.isLinked }
        test.assertNoErrors()
        test.assertNotComplete()
    }

    @Test
    fun `fetch user data on subscribe, user is not linked`() {
        // Arrange
        whenever(nabuUser.userName).thenReturn(null)
        whenever(nabuUser.exchangeEnabled).thenReturn(false)
        whenever(userService.getUser()).thenReturn(Single.just(nabuUser))

        // Act
        val test = subject.state.test()

        // Assert
        verify(userService).getUser()

        test.assertValue { !it.isLinked }
        test.assertNoErrors()
        test.assertNotComplete()
    }

    @Test
    fun `two subscriptions with isExchangeLinked() helper function`() {
        // Arrange
        whenever(nabuUser.exchangeEnabled).thenReturn(true)
        whenever(userService.getUser()).thenReturn(Single.just(nabuUser))

        // Act
        val test1 = subject.state.test()
        val test2 = subject.isExchangeLinked().test()

        // Assert
        verify(userService, times(2)).getUser()

        with(test1) {
            assertValueAt(0) { it.isLinked }
            assertValueAt(1) { it.isLinked }
            assertNoErrors()
            assertNotComplete()
        }

        with(test2) {
            assertResult(true)
        }
    }
}
