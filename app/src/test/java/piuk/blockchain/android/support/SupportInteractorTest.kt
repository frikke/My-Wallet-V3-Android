package piuk.blockchain.android.support

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.nabu.BasicProfileInfo
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
    private val kycService: KycService = mock()

    @Before
    fun setup() {
        interactor = SupportInteractor(
            userIdentity = userIdentity,
            kycService = kycService,
            isIntercomEnabledFlag = mock {
                on { this.enabled }.thenReturn(Single.just(false))
            }
        )
    }

    @Test
    fun `when load user info succeeds with gold then correct calls are made`() {
        val userInfo: BasicProfileInfo = mock()
        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.GOLD))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(userInfo))

        val result = interactor.loadUserInformation().test()
        result.assertValue {
            it.isUserGold && it.basicInfo == userInfo
        }

        verify(kycService).getHighestApprovedTierLevelLegacy()
        verify(userIdentity).getBasicProfileInformation()
        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `when load user info succeeds with silver then correct calls are made`() {
        val userInfo: BasicProfileInfo = mock()
        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.SILVER))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(userInfo))

        val result = interactor.loadUserInformation().test()
        result.assertValue {
            !it.isUserGold && it.basicInfo == userInfo
        }

        verify(kycService).getHighestApprovedTierLevelLegacy()
        verify(userIdentity).getBasicProfileInformation()
        verifyNoMoreInteractions(userIdentity)
    }
}
