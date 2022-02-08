package piuk.blockchain.android.support

import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class SupportInteractorTest {

    private lateinit var interactor: SupportInteractor
    private val userIdentity: UserIdentity = mock()

    @Before
    fun setup() {
        interactor = SupportInteractor(
            userIdentity = userIdentity
        )
    }

    @Test
    fun `when load user info succeeds with gold then correct calls are made`() {
        val userInfo: BasicProfileInfo = mock()
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(userInfo))

        val result = interactor.loadUserInformation().test()
        result.assertValue {
            it.first && it.second == userInfo
        }

        verify(userIdentity).getHighestApprovedKycTier()
        verify(userIdentity).getBasicProfileInformation()
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `when load user info succeeds with silver then correct calls are made`() {
        val userInfo: BasicProfileInfo = mock()
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.SILVER))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(userInfo))

        val result = interactor.loadUserInformation().test()
        result.assertValue {
            !it.first && it.second == userInfo
        }

        verify(userIdentity).getHighestApprovedKycTier()
        verify(userIdentity).getBasicProfileInformation()
        verifyNoMoreInteractions(userIdentity)
    }
}
