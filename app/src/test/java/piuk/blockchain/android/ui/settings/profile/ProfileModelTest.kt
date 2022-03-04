package piuk.blockchain.android.ui.settings.profile

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.ProfileError
import piuk.blockchain.android.ui.settings.v2.profile.ProfileIntent
import piuk.blockchain.android.ui.settings.v2.profile.ProfileInteractor
import piuk.blockchain.android.ui.settings.v2.profile.ProfileModel
import piuk.blockchain.android.ui.settings.v2.profile.ProfileState

class ProfileModelTest {
    private lateinit var model: ProfileModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: ProfileInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = ProfileModel(
            initialState = ProfileState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor,
            _activityIndicator = mock()
        )
    }

    @Test
    fun `when LoadProfile is successfully loaded from Cache then state will update user settings`() {
        val settings: Settings = mock {
            on { email }.thenReturn("lmiguelez@blockchain.com")
            on { isEmailVerified }.thenReturn(true)
            on { smsNumber }.thenReturn("3465589125")
            on { isSmsVerified }.thenReturn(false)
            on { smsDialCode }.thenReturn("34")
            on { authType }.thenReturn(0)
        }

        val userInfoSettings = WalletSettingsService.UserInfoSettings(
            email = settings.email,
            emailVerified = settings.isEmailVerified,
            mobileWithPrefix = settings.smsNumber,
            mobileVerified = settings.isSmsVerified,
            smsDialCode = settings.smsDialCode,
            authType = settings.authType
        )

        whenever(interactor.cachedSettings).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(ProfileIntent.LoadProfile)

        testState
            .assertValueAt(0) {
                it == ProfileState()
            }.assertValueAt(1) {
                it == ProfileState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == ProfileState(
                    isLoading = false,
                    userInfoSettings = userInfoSettings
                )
            }
    }

    @Test
    fun `when LoadProfile fails from Cache then fetchProfileSettings`() {
        whenever(interactor.cachedSettings).thenReturn(Single.error(Throwable()))

        val testState = model.state.test()
        model.process(ProfileIntent.LoadProfile)

        testState
            .assertValueAt(0) {
                it == ProfileState()
            }.assertValueAt(1) {
                it == ProfileState(
                    isLoading = true
                )
            }
    }

    @Test
    fun `when FetchProfile is successfully then state will update user settings`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()

        whenever(interactor.fetchProfileSettings()).thenReturn(Single.just(userInfoSettings))

        val testState = model.state.test()
        model.process(ProfileIntent.FetchProfile)

        testState
            .assertValueAt(0) {
                it == ProfileState()
            }.assertValueAt(1) {
                it == ProfileState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == ProfileState(
                    isLoading = false,
                    userInfoSettings = userInfoSettings
                )
            }
    }

    @Test
    fun `when FetchProfile fails then state should contain LoadProfileError`() {
        whenever(interactor.fetchProfileSettings()).thenReturn(Single.error { Throwable() })

        val testState = model.state.test()
        model.process(ProfileIntent.FetchProfile)

        testState
            .assertValueAt(0) {
                it == ProfileState()
            }.assertValueAt(1) {
                it == ProfileState(isLoading = true)
            }.assertValueAt(2) {
                it == ProfileState(
                    isLoading = false,
                    error = ProfileError.LoadProfileError
                )
            }
    }
}
