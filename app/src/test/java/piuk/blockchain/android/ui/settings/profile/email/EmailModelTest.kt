package piuk.blockchain.android.ui.settings.profile.email

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.Email

class EmailModelTest {
    private lateinit var model: EmailModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    val settings: Settings = mock {
        on { email }.thenReturn("lmiguelez@blockchain.com")
        on { isEmailVerified }.thenReturn(true)
        on { smsNumber }.thenReturn("3465589125")
        on { isSmsVerified }.thenReturn(false)
        on { smsDialCode }.thenReturn("34")
        on { authType }.thenReturn(0)
    }

    private val userInfoSettings = WalletSettingsService.UserInfoSettings(
        email = settings.email,
        emailVerified = settings.isEmailVerified,
        mobileWithPrefix = settings.smsNumber,
        mobileVerified = settings.isSmsVerified,
        smsDialCode = settings.smsDialCode,
        authType = settings.authType
    )

    private val interactor: EmailInteractor = mock {
        on { fetchProfileSettings() }.thenReturn(Single.just(userInfoSettings))
    }

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val activityIndicator: Lazy<ActivityIndicator> = mock {
        on { value }.thenReturn(ActivityIndicator())
    }

    @Before
    fun setUp() {
        model = EmailModel(
            initialState = EmailState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor,
            _activityIndicator = activityIndicator
        )
    }

    @Test
    fun `when LoadProfile is successfully loaded from Cache then state will update user settings`() {

        whenever(interactor.cachedSettings).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(EmailIntent.LoadProfile)

        testState
            .assertValueAt(0) {
                it == EmailState()
            }.assertValueAt(1) {
                it == EmailState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == EmailState(
                    isLoading = false,
                    userInfoSettings = userInfoSettings
                )
            }
    }

    @Test
    fun `when LoadProfile fails from Cache then fetchProfileSettings`() {
        whenever(interactor.cachedSettings).thenReturn(Single.error(Throwable()))

        val testState = model.state.test()
        model.process(EmailIntent.LoadProfile)

        testState
            .assertValueAt(0) {
                it == EmailState()
            }.assertValueAt(1) {
                it == EmailState(
                    isLoading = true
                )
            }
    }

    @Test
    fun `when FetchProfile is successfully then state will update user settings`() {

        val testState = model.state.test()
        model.process(EmailIntent.FetchProfile)

        testState
            .assertValueAt(0) {
                it == EmailState()
            }.assertValueAt(1) {
                it == EmailState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == EmailState(
                    isLoading = false,
                    userInfoSettings = userInfoSettings
                )
            }
    }

    @Test
    fun `when FetchProfile fails then state should contain LoadProfileError`() {
        whenever(interactor.fetchProfileSettings()).thenReturn(Single.error { Throwable() })

        val testState = model.state.test()
        model.process(EmailIntent.FetchProfile)

        testState
            .assertValueAt(0) {
                it == EmailState()
            }.assertValueAt(1) {
                it == EmailState(isLoading = true)
            }.assertValueAt(2) {
                it == EmailState(
                    isLoading = false,
                    error = EmailError.LoadProfileError
                )
            }
    }

    @Test
    fun `when SaveEmail is successfully then state will update settings with the email`() {
        val userInfoSettings: WalletSettingsService.UserInfoSettings = mock {
            on { email }.thenReturn("paco@gmail.com")
        }

        val settings: Email = mock {
            on { address }.thenReturn("paco@gmail.com")
            on { isVerified }.thenReturn(false)
        }

        whenever(
            interactor.saveEmail(
                email = userInfoSettings.email.orEmpty()
            )
        ).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(EmailIntent.SaveEmail(settings.address))

        testState
            .assertValueAt(0) {
                it == EmailState()
            }.assertValueAt(1) {
                it == EmailState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == EmailState(
                    isLoading = false,
                    emailSent = true,
                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                        email = settings.address,
                        emailVerified = settings.isVerified,
                        mobileWithPrefix = it.userInfoSettings?.mobileWithPrefix,
                        mobileVerified = it.userInfoSettings?.mobileVerified ?: false,
                        smsDialCode = it.userInfoSettings?.smsDialCode.orEmpty()
                    )
                )
            }
    }

    @Test
    fun `when SaveEmail fails then state should contain SaveEmailError`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()
        val emailAddress = "paco@gmail.com"

        whenever(userInfoSettings.email).thenReturn(emailAddress)

        whenever(interactor.saveEmail(emailAddress)).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(EmailIntent.SaveEmail(emailAddress))

        testState.assertValueAt(0) {
            it == EmailState()
        }.assertValueAt(1) {
            it == EmailState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == EmailState(
                isLoading = false,
                error = EmailError.SaveEmailError
            )
        }
    }

    @Test
    fun `when ResendEmail is successfully then state isVerificationSent emailSent will be true`() {
        val email: Email = mock {
            on { address }.thenReturn("paco@gmail.com")
            on { isVerified }.thenReturn(false)
        }
        whenever(
            interactor.resendEmail(
                email = EmailState().userInfoSettings?.email.orEmpty()
            )
        ).thenReturn(Single.just(email))

        val testState = model.state.test()
        model.process(EmailIntent.ResendEmail)

        testState
            .assertValueAt(0) {
                it == EmailState()
            }.assertValueAt(1) {
                it == EmailState(
                    isLoading = true,
                    emailSent = false
                )
            }.assertValueAt(2) {
                it == EmailState(
                    isLoading = false,
                    emailSent = true,
                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                        email = email.address,
                        emailVerified = email.isVerified,
                        mobileWithPrefix = it.userInfoSettings?.mobileWithPrefix,
                        mobileVerified = it.userInfoSettings?.mobileVerified ?: false,
                        smsDialCode = it.userInfoSettings?.smsDialCode.orEmpty()
                    )
                )
            }
    }

    @Test
    fun `when ResendEmail fails then state should contain ResendEmailError`() {
        whenever(
            interactor.resendEmail(
                email = EmailState().userInfoSettings?.email.orEmpty()
            )
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(EmailIntent.ResendEmail)

        testState.assertValueAt(0) {
            it == EmailState()
        }.assertValueAt(1) {
            it == EmailState(
                isLoading = true,
                emailSent = false
            )
        }.assertValueAt(2) {
            it == EmailState(
                isLoading = false,
                emailSent = false,
                error = EmailError.ResendEmailError
            )
        }
    }
}
