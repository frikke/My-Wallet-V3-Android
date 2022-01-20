package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
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
import piuk.blockchain.android.ui.settings.v2.profile.VerificationSent
import piuk.blockchain.androidcore.data.settings.Email

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
    fun `when LoadProfile is successfully then state will update user settings`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()

        whenever(interactor.fetchProfileSettings()).thenReturn(
            Single.just(userInfoSettings)
        )

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
                    userInfoSettings = userInfoSettings,
                    isLoading = false
                )
            }
    }

    @Test
    fun `when LoadProfile fails then state should contain LoadProfileError`() {
        whenever(interactor.fetchProfileSettings()).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.LoadProfile)

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

    @Test
    fun `when SaveEmail is successfully then state will update settings with the email`() {
        val userInfoSettings: WalletSettingsService.UserInfoSettings = mock {
            on { email }.thenReturn("paco@gmail.com")
        }

        val settings: Settings = mock {
            on { email }.thenReturn("paco@gmail.com")
        }

        whenever(
            interactor.saveEmail(
                email = userInfoSettings.email.orEmpty()
            )
        ).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(ProfileIntent.SaveEmail(settings.email))

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
                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                        email = settings.email,
                        emailVerified = settings.isEmailVerified,
                        mobileWithPrefix = it.userInfoSettings?.mobileWithPrefix,
                        mobileVerified = it.userInfoSettings?.mobileVerified ?: false
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
        model.process(ProfileIntent.SaveEmail(emailAddress))

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                error = ProfileError.SaveEmailError
            )
        }
    }

    @Test
    fun `when SavePhoneNumber is successfully then state isVerificationSent codeSent will be true`() {
        val userInfoSettings: WalletSettingsService.UserInfoSettings = mock {
            on { mobileWithPrefix }.thenReturn("+34655819515")
        }

        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+34655819515")
        }

        whenever(interactor.savePhoneNumber(userInfoSettings.mobileWithPrefix.orEmpty())).thenReturn(
            Single.just(settings)
        )

        val testState = model.state.test()
        model.process(ProfileIntent.SavePhoneNumber(settings.smsNumber))

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = it.userInfoSettings?.email,
                    emailVerified = it.userInfoSettings?.emailVerified ?: false,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified
                ),
                isVerificationSent = VerificationSent(
                    codeSent = true,
                    emailSent = it.isVerificationSent?.emailSent ?: false,
                )
            )
        }
    }

    @Test
    fun `when SavePhoneNumber fails then state should contain SavePhoneError`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()
        val phoneNumber = "+34655819515"

        whenever(userInfoSettings.mobileWithPrefix).thenReturn(phoneNumber)

        whenever(interactor.savePhoneNumber(phoneNumber)).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.SavePhoneNumber(phoneNumber))

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                error = ProfileError.SavePhoneError
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
                email = ProfileState().userInfoSettings?.email.orEmpty()
            )
        ).thenReturn(Single.just(email))

        val testState = model.state.test()
        model.process(ProfileIntent.ResendEmail)

        testState
            .assertValueAt(0) {
                it == ProfileState()
            }.assertValueAt(1) {
                it == ProfileState(
                    isLoading = true,
                    isVerificationSent = VerificationSent(emailSent = false)
                )
            }.assertValueAt(2) {
                it == ProfileState(
                    isLoading = false,
                    isVerificationSent = VerificationSent(emailSent = true),
                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                        email = email.address,
                        emailVerified = email.isVerified,
                        mobileWithPrefix = it.userInfoSettings?.mobileWithPrefix,
                        mobileVerified = it.userInfoSettings?.mobileVerified ?: false
                    )
                )
            }
    }

    @Test
    fun `when ResendEmail fails then state should contain ResendEmailError`() {
        whenever(
            interactor.resendEmail(
                email = ProfileState().userInfoSettings?.email.orEmpty()
            )
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.ResendEmail)

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true,
                isVerificationSent = VerificationSent(emailSent = false)
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                isVerificationSent = VerificationSent(emailSent = false),
                error = ProfileError.ResendEmailError
            )
        }
    }

    @Test
    fun `when ResendCode is successfully then state isVerificationSent codeSent will be true`() {
        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+34655819515")
        }

        whenever(
            interactor.resendCodeSMS(
                mobileWithPrefix = ProfileState().userInfoSettings?.mobileWithPrefix.orEmpty()
            )
        ).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(ProfileIntent.ResendCodeSMS)

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
                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                        email = it.userInfoSettings?.email,
                        emailVerified = it.userInfoSettings?.emailVerified ?: false,
                        mobileWithPrefix = settings.smsNumber,
                        mobileVerified = settings.isSmsVerified
                    ),
                    isVerificationSent = VerificationSent(
                        codeSent = true,
                        emailSent = it.isVerificationSent?.emailSent ?: false,
                    )
                )
            }
    }

    @Test
    fun `when ResendCode fails then state should contain ResendSmsError`() {
        whenever(
            interactor.resendCodeSMS(
                mobileWithPrefix = ProfileState().userInfoSettings?.mobileWithPrefix.orEmpty()
            )
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.ResendCodeSMS)

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                error = ProfileError.ResendSmsError
            )
        }
    }

    @Test
    fun `when VerifyPhone is successfully then no error is set`() {
        val code = "1234AB"

        whenever(interactor.verifyPhoneNumber(code))
            .thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(ProfileIntent.VerifyPhoneNumber(code))

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
    fun `when VerifyPhone fails then state should contain VerifyPhoneError`() {
        val code = "1234AB"

        whenever(interactor.verifyPhoneNumber(code = code))
            .thenReturn(Completable.error { Throwable() })

        val testState = model.state.test()
        model.process(ProfileIntent.VerifyPhoneNumber(code))

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                error = ProfileError.VerifyPhoneError
            )
        }
    }
}
