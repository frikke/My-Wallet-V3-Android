package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.BasicProfileInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.SettingsIntent
import piuk.blockchain.android.ui.settings.v2.SettingsInteractor
import piuk.blockchain.android.ui.settings.v2.SettingsModel
import piuk.blockchain.android.ui.settings.v2.SettingsState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class SettingsModelTest {
    private lateinit var model: SettingsModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: SettingsInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = SettingsModel(
            initialState = SettingsState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `checkContactSupportEligibility is simple buy NOT Eligible`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(Single.just(Pair(false, userInformation)))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadInitialInformation)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(basicProfileInfo = userInformation, isSupportChatEnabled = false)
            }
    }

    @Test
    fun `checkContactSupportEligibility is simple buy eligible`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(Single.just(Pair(true, userInformation)))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadInitialInformation)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(basicProfileInfo = userInformation, isSupportChatEnabled = true)
            }
    }

    @Test
    fun `checkContactSupportEligibility throws error`() {
        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(Single.error { Throwable() })

        val testState = model.state.test()
        model.process(SettingsIntent.LoadInitialInformation)

        testState
            .assertValueAt(0) { it == SettingsState() }
    }
}
