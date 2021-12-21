package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
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
    fun `checkContactSupportEligibility is NOT isSupportChatEnabled`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(
            Single.just(Pair(Tier.SILVER, userInformation))
        )

        val testState = model.state.test()
        model.process(SettingsIntent.LoadSupportEligibilityAndUserInfo)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    basicProfileInfo = userInformation,
                    tier = Tier.SILVER
                )
            }
    }

    @Test
    fun `checkContactSupportEligibility is tier=GOLD and isSupportChatEnabled`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo())
            .thenReturn(Single.just(Pair(Tier.GOLD, userInformation)))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadSupportEligibilityAndUserInfo)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    basicProfileInfo = userInformation,
                    tier = Tier.GOLD
                )
            }
    }

    @Test
    fun `checkContactSupportEligibility throws error`() {
        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(Single.error { Throwable() })

        val testState = model.state.test()
        model.process(SettingsIntent.LoadSupportEligibilityAndUserInfo)

        testState
            .assertValueAt(0) { it == SettingsState() }
    }
}
