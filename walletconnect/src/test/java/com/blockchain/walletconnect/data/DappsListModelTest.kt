package com.blockchain.walletconnect.data

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletconnect.domain.ClientMeta
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletInfo
import com.blockchain.walletconnect.ui.dapps.DappsListIntent
import com.blockchain.walletconnect.ui.dapps.DappsListModel
import com.blockchain.walletconnect.ui.dapps.DappsListState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DappsListModelTest {
    private lateinit var subject: DappsListModel

    private val enviromentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val remoteLogger = mock<RemoteLogger>()
    private var sessionsRepository = mock<SessionRepository>()
    private var walletConnectServiceAPI = mock<WalletConnectServiceAPI>()
    private var walletConnectV2Service = mock<WalletConnectV2Service>()
    private val walletConnectV2FeatureFlag = mock<FeatureFlag>()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        subject = DappsListModel(
            Schedulers.io(),
            enviromentConfig,
            remoteLogger,
            sessionsRepository,
            walletConnectServiceAPI,
            walletConnectV2Service,
            walletConnectV2FeatureFlag
        )

        whenever(walletConnectV2FeatureFlag.enabled).thenReturn(Single.just(false))
    }

    @Test
    fun `Repository  sessions should be represented in the state`() {
        whenever(sessionsRepository.retrieve()).thenReturn(
            Single.just(
                listOf(
                    fakeSession1,
                    fakeSession2
                )
            )
        )

        val test = subject.state.test()

        subject.process(DappsListIntent.LoadDapps)

        test.assertValueAt(0) {
            it == DappsListState()
        }.assertValueAt(1) {
            it == DappsListState(
                listOf(
                    fakeSession1,
                    fakeSession2
                )
            )
        }
        test.assertValueCount(2)
    }

    @Test
    fun `when repository errors then no sessions should be represented in the state`() {
        whenever(sessionsRepository.retrieve()).thenReturn(
            Single.error(Throwable())
        )

        val test = subject.state.test()

        subject.process(DappsListIntent.LoadDapps)

        test.assertValueAt(0) {
            it == DappsListState()
        }
        test.assertValueCount(1)
    }

    @Test
    fun `when disconnected dapps should be reloaded`() {
        whenever(walletConnectServiceAPI.disconnect(fakeSession1)).thenReturn(
            Completable.complete()
        )

        whenever(sessionsRepository.retrieve()).thenReturn(
            Single.just(
                listOf(
                    fakeSession2
                )
            )
        )

        val test = subject.state.test()

        subject.process(DappsListIntent.Disconnect(fakeSession1))

        test.assertValueAt(0) {
            it == DappsListState()
        }.assertValueAt(1) {
            it == DappsListState(
                listOf(
                    fakeSession2
                )
            )
        }
    }

    @Test
    fun `when disconnected errors, dapps should be reloaded`() {
        whenever(walletConnectServiceAPI.disconnect(fakeSession1)).thenReturn(
            Completable.error(Throwable())
        )
        whenever(sessionsRepository.retrieve()).thenReturn(
            Single.just(
                listOf(
                    fakeSession2
                )
            )
        )

        val test = subject.state.test()

        subject.process(DappsListIntent.Disconnect(fakeSession1))

        test.assertValueAt(0) {
            it == DappsListState()
        }.assertValueAt(1) {
            it == DappsListState(
                listOf(
                    fakeSession2
                )
            )
        }
    }

    companion object {
        val fakeSession1 = WalletConnectSession(
            url = "sessionUrl",
            dAppInfo = DAppInfo(
                peerId = "peerid",
                peerMeta = ClientMeta("description", "url", emptyList(), "name"),
                chainId = -1
            ),
            walletInfo = WalletInfo("!23", "Android")
        )
        val fakeSession2 = WalletConnectSession(
            url = "sessionUrl12312",
            dAppInfo = DAppInfo(
                peerId = "peer123id",
                peerMeta = ClientMeta(description = "descrip213tion", url = "url123", emptyList(), name = "name213"),
                chainId = -1
            ),
            walletInfo = WalletInfo("!22133233", "Ios")
        )
    }
}
