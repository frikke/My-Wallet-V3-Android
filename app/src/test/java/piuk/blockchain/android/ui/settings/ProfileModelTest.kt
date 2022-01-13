package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.WalletSettingsService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.ProfileIntent
import piuk.blockchain.android.ui.settings.v2.profile.ProfileInteractor
import piuk.blockchain.android.ui.settings.v2.profile.ProfileModel
import piuk.blockchain.android.ui.settings.v2.profile.ProfileState
import piuk.blockchain.android.ui.settings.v2.profile.ProfileViewState
import piuk.blockchain.android.ui.settings.v2.profile.VerificationSent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
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
            interactor = interactor
        )
    }

    @Test
    fun `loadProfile successfully`() {
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
    fun `loadProfile fails`() {
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
                    isLoading = false
                )
            }
    }

    @Test
    fun `saveProfile successfully`() {
        val userInfoSettings: WalletSettingsService.UserInfoSettings = mock {
            on { email }.thenReturn("paco@gmail.com")
            on { mobileWithPrefix }.thenReturn("+34655819515")
        }

        val settings: Settings = mock {
            on { email }.thenReturn("paco@gmail.com")
            on { smsNumber }.thenReturn("+34655819515")
        }

        val email: Email = mock {
            on { address }.thenReturn("paco@gmail.com")
            on { isVerified }.thenReturn(false)
        }
        whenever(
            interactor.saveProfile(
                email = userInfoSettings.email.orEmpty(),
                mobileWithPrefix = userInfoSettings.mobileWithPrefix.orEmpty()
            )
        )
            .thenReturn(
                Single.just(Pair(email, settings))
            )

        val testState = model.state.test()
        model.process(ProfileIntent.SaveProfile(userInfoSettings))

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
                        email = email.address,
                        emailVerified = email.isVerified,
                        mobileWithPrefix = settings.smsNumber,
                        mobileVerified = settings.isSmsVerified
                    )
                )
            }
    }

    @Test
    fun `saveProfile failed`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()

        val emailAddress = "paco@gmail.com"
        val phoneNumber = "+34655819515"

        whenever(userInfoSettings.email).thenReturn(emailAddress)
        whenever(userInfoSettings.mobileWithPrefix).thenReturn(phoneNumber)

        whenever(interactor.saveProfile(emailAddress, phoneNumber)).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.SaveProfile(userInfoSettings))

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false,
                savingHasFailed = true,
                profileViewState = ProfileViewState.View
            )
        }
    }

    @Test
    fun `saveAndSendEmail successfully`() {
        val userInfoSettings: WalletSettingsService.UserInfoSettings = mock {
            on { email }.thenReturn("paco@gmail.com")
        }

        val email: Email = mock {
            on { address }.thenReturn("paco@gmail.com")
            on { isVerified }.thenReturn(false)
        }
        whenever(
            interactor.saveAndSendEmail(
                email = userInfoSettings.email.orEmpty()
            )
        ).thenReturn(Single.just(email))

        val testState = model.state.test()
        model.process(ProfileIntent.SaveAndSendEmail(userInfoSettings.email.orEmpty()))

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
                    isVerificationSent = VerificationSent(emailSent = true)
                )
            }
    }

    @Test
    fun `saveAndSendEmail failed`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()
        val emailAddress = "paco@gmail.com"

        whenever(userInfoSettings.email).thenReturn(emailAddress)
        whenever(
            interactor.saveAndSendEmail(
                email = userInfoSettings.email.orEmpty()
            )
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.SaveAndSendEmail(userInfoSettings.email.orEmpty()))

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
                isVerificationSent = VerificationSent(emailSent = false)
            )
        }
    }

    @Test
    fun `saveAndSendSMS successfully`() {
        val userInfoSettings: WalletSettingsService.UserInfoSettings = mock {
            on { mobileWithPrefix }.thenReturn("+34655819515")
        }

        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+34655819515")
        }

        whenever(
            interactor.saveAndSendSMS(
                mobileWithPrefix = userInfoSettings.mobileWithPrefix.orEmpty()
            )
        ).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(ProfileIntent.SaveAndSendSMS(userInfoSettings.mobileWithPrefix.orEmpty()))

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
                    isVerificationSent = VerificationSent(codeSent = true)
                )
            }
    }

    @Test
    fun `saveAndSendSMS failed`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()
        val phoneNumber = "+34655748394"

        whenever(userInfoSettings.mobileWithPrefix).thenReturn(phoneNumber)
        whenever(
            interactor.saveAndSendSMS(
                mobileWithPrefix = userInfoSettings.mobileWithPrefix.orEmpty()
            )
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(ProfileIntent.SaveAndSendSMS(userInfoSettings.mobileWithPrefix.orEmpty()))

        testState.assertValueAt(0) {
            it == ProfileState()
        }.assertValueAt(1) {
            it == ProfileState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == ProfileState(
                isLoading = false
            )
        }
    }

    @Test
    fun `VerifyPhoneNumber successfully`() {
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
    fun `VerifyPhoneNumber failed`() {
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
                isLoading = false
            )
        }
    }
}
