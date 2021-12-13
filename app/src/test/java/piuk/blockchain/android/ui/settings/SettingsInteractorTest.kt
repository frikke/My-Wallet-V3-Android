package piuk.blockchain.android.ui.settings

import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.SettingsInteractor

class SettingsInteractorTest {

    private lateinit var interactor: SettingsInteractor
    private val userIdentity: UserIdentity = mock()

    @Before
    fun setup() {
        interactor = SettingsInteractor(
            userIdentity = userIdentity
        )
    }

    @Test
    fun `Load eligibility`() {
        val mockProfileInfo: BasicProfileInfo = mock()
        whenever(userIdentity.isEligibleFor(Feature.SimpleBuy)).thenReturn(Single.just(true))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(mockProfileInfo))

        val observer = interactor.checkContactSupportEligibility().test()
        observer.assertValue {
            it.first && it.second == mockProfileInfo
        }

        verify(userIdentity).isEligibleFor(Feature.SimpleBuy)
        verify(userIdentity).getBasicProfileInformation()

        verifyNoMoreInteractions(userIdentity)
    }
}
