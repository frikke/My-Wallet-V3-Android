package piuk.blockchain.android.ui.settings.notifications

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.notifications.EmailNotVerifiedException
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsError
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsIntent
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsInteractor
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsModel
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class NotificationsModelTest {

    private lateinit var model: NotificationsModel
    private val defaultState = spy(NotificationsState())

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: NotificationsInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = NotificationsModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun loadNotificationInfo_success() {
        whenever(interactor.getNotificationsEnabled()).thenReturn(Single.just(Pair(first = true, second = true)))

        val testState = model.state.test()
        model.process(NotificationsIntent.LoadNotificationInfo)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.emailNotificationsEnabled && it.pushNotificationsEnabled
        }
    }

    @Test
    fun loadNotificationInfo_error() {
        whenever(interactor.getNotificationsEnabled()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(NotificationsIntent.LoadNotificationInfo)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.errorState == NotificationsError.NOTIFICATION_INFO_LOAD_FAIL
        }
    }

    @Test
    fun toggleEmailNotificationsOn_success() {
        whenever(interactor.toggleEmailNotifications(false)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(NotificationsIntent.ToggleEmailNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            !it.pushNotificationsEnabled && it.emailNotificationsEnabled
        }
    }

    @Test
    fun toggleEmailNotificationsOff_success() {
        whenever(defaultState.emailNotificationsEnabled).thenReturn(true)

        whenever(interactor.toggleEmailNotifications(true)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(NotificationsIntent.ToggleEmailNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            !it.pushNotificationsEnabled && !it.emailNotificationsEnabled
        }
    }

    @Test
    fun toggleEmailNotifications_error_not_verified() {
        whenever(interactor.toggleEmailNotifications(any())).thenReturn(Completable.error(EmailNotVerifiedException()))

        val testState = model.state.test()
        model.process(NotificationsIntent.ToggleEmailNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            it.errorState == NotificationsError.EMAIL_NOT_VERIFIED
        }
    }

    @Test
    fun toggleEmailNotifications_error() {
        whenever(interactor.toggleEmailNotifications(any())).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(NotificationsIntent.ToggleEmailNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            it.errorState == NotificationsError.EMAIL_NOTIFICATION_UPDATE_FAIL
        }
    }

    @Test
    fun togglePushNotificationsOn_success() {
        whenever(defaultState.emailNotificationsEnabled).thenReturn(true)
        whenever(defaultState.pushNotificationsEnabled).thenReturn(false)
        whenever(interactor.arePushNotificationsEnabled()).thenReturn(false)
        whenever(interactor.enablePushNotifications()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(NotificationsIntent.TogglePushNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            it.emailNotificationsEnabled && it.pushNotificationsEnabled
        }
    }

    @Test
    fun togglePushNotificationsOff_success() {
        whenever(defaultState.emailNotificationsEnabled).thenReturn(true)
        whenever(defaultState.pushNotificationsEnabled).thenReturn(true)
        whenever(interactor.arePushNotificationsEnabled()).thenReturn(true)
        whenever(interactor.disablePushNotifications()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(NotificationsIntent.TogglePushNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            it.emailNotificationsEnabled && !it.pushNotificationsEnabled
        }
    }

    @Test
    fun togglePushNotifications_error() {
        whenever(defaultState.emailNotificationsEnabled).thenReturn(true)
        whenever(defaultState.pushNotificationsEnabled).thenReturn(true)
        whenever(interactor.arePushNotificationsEnabled()).thenReturn(true)
        whenever(interactor.disablePushNotifications()).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(NotificationsIntent.TogglePushNotifications)

        testState.assertValueAt(0) {
            allStateFieldsMatchDefault(it)
        }.assertValueAt(1) {
            it.errorState == NotificationsError.PUSH_NOTIFICATION_UPDATE_FAIL
        }
    }

    private fun allStateFieldsMatchDefault(state: NotificationsState): Boolean =
        state.errorState == defaultState.errorState &&
            state.emailNotificationsEnabled == defaultState.emailNotificationsEnabled &&
            state.pushNotificationsEnabled == defaultState.pushNotificationsEnabled
}
