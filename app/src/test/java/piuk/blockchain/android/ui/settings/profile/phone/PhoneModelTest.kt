package piuk.blockchain.android.ui.settings.profile.phone

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneError
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneIntent
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneInteractor
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneModel
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneState
import piuk.blockchain.androidcore.data.settings.InvalidPhoneNumber

class PhoneModelTest {
    private lateinit var model: PhoneModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: PhoneInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = PhoneModel(
            initialState = PhoneState(),
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
        model.process(PhoneIntent.LoadProfile)

        testState
            .assertValueAt(0) {
                it == PhoneState()
            }.assertValueAt(1) {
                it == PhoneState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == PhoneState(
                    isLoading = false,
                    userInfoSettings = userInfoSettings
                )
            }
    }

    @Test
    fun `when LoadProfile fails from Cache then fetchProfileSettings`() {
        whenever(interactor.cachedSettings).thenReturn(Single.error(Throwable()))

        val testState = model.state.test()
        model.process(PhoneIntent.LoadProfile)

        testState
            .assertValueAt(0) {
                it == PhoneState()
            }.assertValueAt(1) {
                it == PhoneState(
                    isLoading = true
                )
            }
    }

    @Test
    fun `when FetchProfile is successfully then state will update user settings`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()

        whenever(interactor.fetchProfileSettings()).thenReturn(Single.just(userInfoSettings))

        val testState = model.state.test()
        model.process(PhoneIntent.FetchProfile)

        testState
            .assertValueAt(0) {
                it == PhoneState()
            }.assertValueAt(1) {
                it == PhoneState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == PhoneState(
                    isLoading = false,
                    userInfoSettings = userInfoSettings
                )
            }
    }

    @Test
    fun `when FetchProfile fails then state should contain LoadProfileError`() {
        whenever(interactor.fetchProfileSettings()).thenReturn(Single.error { Throwable() })

        val testState = model.state.test()
        model.process(PhoneIntent.FetchProfile)

        testState
            .assertValueAt(0) {
                it == PhoneState()
            }.assertValueAt(1) {
                it == PhoneState(isLoading = true)
            }.assertValueAt(2) {
                it == PhoneState(
                    isLoading = false,
                    error = PhoneError.LoadProfileError
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
            on { smsDialCode }.thenReturn("+34")
        }

        whenever(interactor.savePhoneNumber(userInfoSettings.mobileWithPrefix.orEmpty())).thenReturn(
            Single.just(settings)
        )

        val testState = model.state.test()
        model.process(PhoneIntent.SavePhoneNumber(settings.smsNumber))

        testState.assertValueAt(0) {
            it == PhoneState()
        }.assertValueAt(1) {
            it == PhoneState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PhoneState(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = it.userInfoSettings?.email,
                    emailVerified = it.userInfoSettings?.emailVerified ?: false,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified,
                    smsDialCode = settings.smsDialCode
                ),
                codeSent = true
            )
        }
    }

    @Test
    fun `when SavePhoneNumber fails then state should contain SavePhoneError`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()
        val phoneNumber = "+34655819515"

        val error: HDWalletException = mock {
            on { message }.thenReturn("whatever")
        }

        whenever(userInfoSettings.mobileWithPrefix).thenReturn(phoneNumber)

        whenever(interactor.savePhoneNumber(phoneNumber)).thenReturn(
            Single.error { error }
        )

        val testState = model.state.test()
        model.process(PhoneIntent.SavePhoneNumber(phoneNumber))

        testState.assertValueAt(0) {
            it == PhoneState()
        }.assertValueAt(1) {
            it == PhoneState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PhoneState(
                isLoading = false,
                error = PhoneError.SavePhoneError
            )
        }
    }

    @Test
    fun `when SavePhoneNumber fails then state should contain PhoneNumberNotValidError`() {
        val userInfoSettings = mock<WalletSettingsService.UserInfoSettings>()
        val phoneNumber = "+34123456789"

        whenever(userInfoSettings.mobileWithPrefix).thenReturn(phoneNumber)

        whenever(interactor.savePhoneNumber(phoneNumber)).thenReturn(
            Single.error { InvalidPhoneNumber() }
        )

        val testState = model.state.test()
        model.process(PhoneIntent.SavePhoneNumber(phoneNumber))

        testState.assertValueAt(0) {
            it == PhoneState()
        }.assertValueAt(1) {
            it == PhoneState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PhoneState(
                isLoading = false,
                error = PhoneError.PhoneNumberNotValidError
            )
        }
    }

    @Test
    fun `when ResendCode is successfully then state isVerificationSent codeSent will be true`() {
        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+34655819515")
            on { smsDialCode }.thenReturn("+34")
        }

        whenever(
            interactor.resendCodeSMS(
                mobileWithPrefix = PhoneState().userInfoSettings?.mobileWithPrefix.orEmpty()
            )
        ).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(PhoneIntent.ResendCodeSMS)

        testState
            .assertValueAt(0) {
                it == PhoneState()
            }.assertValueAt(1) {
                it == PhoneState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == PhoneState(
                    isLoading = false,
                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                        email = it.userInfoSettings?.email,
                        emailVerified = it.userInfoSettings?.emailVerified ?: false,
                        mobileWithPrefix = settings.smsNumber,
                        mobileVerified = settings.isSmsVerified,
                        smsDialCode = settings.smsDialCode
                    ),
                    codeSent = true,
                )
            }
    }

    @Test
    fun `when ResendCode fails then state should contain ResendSmsError`() {
        whenever(
            interactor.resendCodeSMS(
                mobileWithPrefix = PhoneState().userInfoSettings?.mobileWithPrefix.orEmpty()
            )
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(PhoneIntent.ResendCodeSMS)

        testState.assertValueAt(0) {
            it == PhoneState()
        }.assertValueAt(1) {
            it == PhoneState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PhoneState(
                isLoading = false,
                error = PhoneError.ResendSmsError
            )
        }
    }
}
