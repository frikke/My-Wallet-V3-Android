package piuk.blockchain.android.support

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.BasicProfileInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SupportModelTest {
    private lateinit var model: SupportModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: SupportInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = SupportModel(
            initialState = SupportState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `when load user info then state is updated`() {
        val userInfo: BasicProfileInfo = mock()
        whenever(interactor.loadUserInformation()).thenReturn(
            Single.just(
                UserInfo(
                    true,
                    userInfo,
                    false
                )
            )
        )

        val test = model.state.test()
        model.process(SupportIntent.LoadUserInfo)

        test.assertValueAt(0) {
            it == SupportState()
        }.assertValueAt(1) {
            it.viewState == SupportViewState.Loading
        }.assertValueAt(2) {
            it.viewState is SupportViewState.ShowInfo &&
                (it.viewState as SupportViewState.ShowInfo).userInfo.isUserGold &&
                (it.viewState as SupportViewState.ShowInfo).userInfo.basicInfo == userInfo &&
                !(it.viewState as SupportViewState.ShowInfo).userInfo.isIntercomEnabled
        }
    }

    @Test
    fun `when load user info fails then state is updated`() {
        whenever(interactor.loadUserInformation()).thenReturn(Single.error(Exception()))

        val test = model.state.test()
        model.process(SupportIntent.LoadUserInfo)

        test.assertValueAt(0) {
            it == SupportState()
        }.assertValueAt(1) {
            it.viewState == SupportViewState.Loading
        }.assertValueAt(2) {
            it.supportError == SupportError.ErrorLoadingProfileInfo
        }
    }
}
