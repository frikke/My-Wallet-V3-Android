package info.blockchain.wallet.payload

import com.blockchain.AppVersion
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.Device
import info.blockchain.wallet.WalletApiMockedResponseTest
import info.blockchain.wallet.payload.data.AccountV3
import info.blockchain.wallet.payload.data.AccountV4
import info.blockchain.wallet.payload.data.AddressCache
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.Options
import java.util.LinkedList
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

class PayloadMangerTestV2 : WalletApiMockedResponseTest() {

    private val balanceManagerBtc: BalanceManagerBtc = mock()

    private val balanceManagerBch: BalanceManagerBch = mock()

    private lateinit var payloadManager: PayloadManager
    @Before fun setup() {
        MockitoAnnotations.openMocks(this)
        doNothing().`when`(balanceManagerBtc).updateAllBalances(any(), any())
        doNothing().`when`(balanceManagerBch).updateAllBalances(any(), any())
        payloadManager = PayloadManager(
            walletApi,
            mock(),
            mock(),
            balanceManagerBtc,
            balanceManagerBch,
            object : Device {
                override val osType: String
                    get() = "android"
            },
            object : AppVersion {
                override val appVersion: String
                    get() = "8.18"
            },
            mock(),
        )
    }

    private fun initializeAndDecryptTestWallet(resourse: String, password: String) {
        val walletBase = loadResourceContent(resourse)
        val responseList = LinkedList<String>()
        responseList.add(walletBase)
        mockInterceptor!!.setResponseStringList(responseList)
        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            password
        )
    }

    @Test
    fun `initializeAndDecryptV4payload should decrypt the right fields`() {
        initializeAndDecryptTestWallet("wallet_v4_encrypted_2.txt", "LivingDome1040")
        assert(payloadManager.payload!!.guid == "4c4ae436-3688-4beb-90ba-8ddeff23f3b9")
        assert(payloadManager.payload!!.dpasswordhash!!.isEmpty())
        assert(
            payloadManager.payload!!.options == Options(
                pbkdf2Iterations = 5000, feePerKb = 10000, _isHtml5Notifications = false, _logoutTime = 600000
            )
        )
        assert(payloadManager.payload?.importedAddressList!!.isEmpty())
        assert(payloadManager.payload?.isUpgradedToV3 == true)
        println(payloadManager.payload?.sharedKey == "ed8c9149-e9c2-4145-bc80-c8834e5eec50")
        assert(payloadManager.payload?.txNotes == emptyMap<String, String>())

        assert(
            payloadManager.payload?.walletBody?.accounts == listOf(
                AccountV4(
                    label = "Private Key Wallet", _defaultType = "bech32", _isArchived = false,
                    derivations = listOf(
                        Derivation(
                            type = "legacy", purpose = 44,
                            xpriv = "xprv9yWWrg4pye8RGB4vyjMdsqbrMihwo1Tpu7Kn5sZywYTG2rVBzyJBwoTodBn8SvWWLxt9nR5hL5fmJS5vQ5aBpqksjaDQ4vP8PkwWn6hb113",
                            xpub = "xpub6CVsGBbip1giUf9Q5kteEyYaukYSCUBgGLFNtFybVszEuepLYWcSVbnHUTh5ASY6q2HGf5tJjhmXm7YjssMnKru7TUv1FDU23W5ubBQoMvv",
                            cache = AddressCache(
                                _receiveAccount = "xpub6EHzP8D6edmXjSAt2aw2bDXmPd4Aa5q8FSP5Qe5Y7fMLb5Ly8wMfPxPmZncxbEJwJ7Hx25C7ybHQgi8QQXdNP2niABC94JqndHnUznviuHg",
                                _changeAccount = "xpub6EHzP8D6edmXkSQ3FLcCQKHD3moJmkVUjeJYCKjLcUA8Yg2Ws8L9aE2sDbDvgkFBVgqxbSjXkbpn8zye6iBiLnbxzGYaf8TAdiMgzwqNK87"
                            ),
                            _addressLabels = emptyList()
                        ),
                        Derivation(
                            type = "bech32", purpose = 84,
                            xpriv = "xprv9yDDzgiPtLsELMgwKxQP6URNujTTB3Lyn1MwgGQ2kHDxF6PyXJMazgAcyQsxzgACF7bJd8iwY6jPgDiFvAXdDVEfjjP4a2XpkywY3xp6F3B",
                            xpub = "xpub6CCaQCFHiiRXYqmQRywPTcN7TmHwaW4q9EHYUeoeJckw7tj84qfqYUV6pedgtJi1p7QNLnnn5Gki2R5wR6pYSHWptaPJgD3219iZRd5H5TU",
                            cache = AddressCache(
                                _receiveAccount = "xpub6EHzP8D6edmXjSAt2aw2bDXmPd4Aa5q8FSP5Qe5Y7fMLb5Ly8wMfPxPmZncxbEJwJ7Hx25C7ybHQgi8QQXdNP2niABC94JqndHnUznviuHg",
                                _changeAccount = "xpub6EHzP8D6edmXkSQ3FLcCQKHD3moJmkVUjeJYCKjLcUA8Yg2Ws8L9aE2sDbDvgkFBVgqxbSjXkbpn8zye6iBiLnbxzGYaf8TAdiMgzwqNK87"
                            ),
                            _addressLabels = emptyList()
                        )
                    )
                ),
                AccountV4(
                    label = "test", _defaultType = "bech32", _isArchived = false,
                    derivations = listOf(
                        Derivation(
                            type =
                            "legacy",
                            purpose = 44,
                            xpriv = "xprv9yWWrg4pye8RL75R4tjnDMxhMja13iRiBvAFY2jdnKyN9Y4a7kH36AVQyhH16Xw1xxHdpXjV1Kpm4JAKXPCWgZgbzUyUmckTqS2BnGNpbdE",
                            xpub = "xpub6CVsGBbip1giYb9tAvGnaVuRumQVTB9ZZ95rLR9FLfWM2LPifHbHdxotpxAa7PDjyApbXDWwAnJ4z7h33DkciHuN2UQMviBRnkXfTBxWsgV",
                            cache = AddressCache(
                                _receiveAccount = "xpub6DzsXA1tBsegPgd1eMJ1Nu6LAgLoXCmYgtbg15rpMr7YUwfXGw2zk4pitR8EaRyfv29uQ53gXXZoZ3UQvAyz5v7G8TKMJD7Q5BNrkP9Utdg",
                                _changeAccount = "xpub6DzsXA1tBsegSJDwSCeXjRAPF98evP74t6qdwWyZXCSFaa9dB21RsgYViKFSgYetrgpKzW5C3Ag8b4QaPHdGELLMmCcgdmy6MrQ4Egapk5v"
                            ),
                            _addressLabels = listOf()
                        ),
                        Derivation(
                            type = "bech32", purpose = 84,
                            xpriv = "xprv9yDDzgiPtLsEN9B4dsgujWhNKGSdJZz3wrbyNNDYZv64PFpFubG89rq92yztrhyCKUjmeRA6PM5Z2meaZiDN73WSv9rY6QTd3C29M5rcyLG",
                            xpub = "xpub6CCaQCFHiiRXadFXjuDv6ee6sJH7i2huK5XaAkdA8Fd3G49QT8aNhf9ctEmmvNVVPL3jkuwCb47cc9EYv5jMGbkQdFg3E4EE6sdwgTc7c2H",
                            cache = AddressCache(
                                _receiveAccount = "xpub6DzsXA1tBsegPgd1eMJ1Nu6LAgLoXCmYgtbg15rpMr7YUwfXGw2zk4pitR8EaRyfv29uQ53gXXZoZ3UQvAyz5v7G8TKMJD7Q5BNrkP9Utdg",
                                _changeAccount = "xpub6DzsXA1tBsegSJDwSCeXjRAPF98evP74t6qdwWyZXCSFaa9dB21RsgYViKFSgYetrgpKzW5C3Ag8b4QaPHdGELLMmCcgdmy6MrQ4Egapk5v"
                            ),
                            _addressLabels = emptyList()
                        )
                    )
                ),
                AccountV4(
                    label = "Tavo Tevas", _defaultType = "bech32", _isArchived = false,
                    derivations = listOf(
                        Derivation(
                            type = "legacy", purpose = 44,
                            xpriv = "xprv9yWWrg4pye8RNb8GWsQijCwC4rbk7i68iQE9qhR2b3Bd8yzy7Q7C23KyBHzXtQfMmoUe11xU1Vg9auNXeLEPNhfMoFAKwCN8eFj46YwJLtw",
                            xpub = "xpub6CVsGBbip1gib5Cjctwj6LsvctSEXAoz5d9ke5pe9Nic1nL7ewRSZqeT2Zy2g2o6Lh5LVeXkNH79TNNEiUGaREqE3BWSJfMYWA65uCrSrqh",
                            cache = AddressCache(
                                _receiveAccount = "xpub6DaK6gXFZBp87XfyzKpYoCZ7Ka8hzuqqkD88D2gd9dormH4duz9DsoES4vgH8WdhQJdxzEptdnEFgjHMi1RgtY1B9MeTxuQzBEhqx4KPWbN",
                                _changeAccount = "xpub6DaK6gXFZBp8ANiDAeMdCr4jgHjDi9FdbQcSrXZayAmqL56ogcyHyM8KnFohKP8czf2Vr1r4QPg1nLghTj88ksmHNLtQUfBPvUEKNGFiaN7"
                            ),
                            _addressLabels = emptyList()
                        ),
                        Derivation(
                            type = "bech32", purpose = 84,
                            xpriv = "xprv9yDDzgiPtLsEQeMAenjKJLUfUBmnwkRDdpU7vV1Y3in16WoZHHi8Xck2U2jCHGGfZ2dFnhpieJNHDvZjHexnVmvi1zhoZ2vAvbPaneHgZQQ",
                            xpub = "xpub6CCaQCFHiiRXd8RdkpGKfURQ2DcHMD9513PiisR9c4JyyK8hpq2P5R4WKJV3FwFR864Rt7kxbrqX9BEKKFWfoN9LArXixCFFfUrv6dArZZK",
                            cache = AddressCache(
                                _receiveAccount = "xpub6DaK6gXFZBp87XfyzKpYoCZ7Ka8hzuqqkD88D2gd9dormH4duz9DsoES4vgH8WdhQJdxzEptdnEFgjHMi1RgtY1B9MeTxuQzBEhqx4KPWbN",
                                _changeAccount = "xpub6DaK6gXFZBp8ANiDAeMdCr4jgHjDi9FdbQcSrXZayAmqL56ogcyHyM8KnFohKP8czf2Vr1r4QPg1nLghTj88ksmHNLtQUfBPvUEKNGFiaN7"
                            ),
                            _addressLabels = emptyList()
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `updateDerivationsForAccounts should update the account payloads and sync back to the api`() {
        initializeAndDecryptTestWallet("wallet_v3_encrypted.txt", "1234")
        assert(payloadManager.payload!!.walletBodies!![0].accounts[0] is AccountV3)
        val responseList = LinkedList<String>()
        responseList.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(responseList)
        payloadManager.updateDerivationsForAccounts(payloadManager.payload!!.walletBody!!.accounts)
        assert(payloadManager.payload!!.walletBodies!!.size == 1)
        assert(payloadManager.payload!!.walletBodies!![0].accounts.size == 1)
        assert(payloadManager.payload!!.walletBodies!![0].accounts[0] is AccountV4)
        assert((payloadManager.payload!!.walletBodies!![0].accounts[0] as AccountV4).derivations.size == 2)
    }

    @Test
    fun `updateAccountLabel  should update the account payloads and sync back to the api`() {
        initializeAndDecryptTestWallet("wallet_v4_encrypted_2.txt", "LivingDome1040")
        assert(payloadManager.payload!!.walletBodies!![0].accounts[0].label == "Private Key Wallet")
        val responseList = LinkedList<String>()
        responseList.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(responseList)
        payloadManager.updateAccountLabel(payloadManager.payload!!.walletBodies!![0].accounts[0], "Random Label")
        assert(payloadManager.payload!!.walletBodies!![0].accounts[0].label == "Random Label")
        assert(payloadManager.payload!!.walletBodies!![0].accounts.none { it.label == "Private Key Wallet" })
    }

    @Test
    fun `updateArchivedAccountState  should update the account payloads and sync back to the api`() {
        initializeAndDecryptTestWallet("wallet_v4_encrypted_2.txt", "LivingDome1040")
        assert(!payloadManager.payload!!.walletBodies!![0].accounts[0].isArchived)
        val responseList = LinkedList<String>()
        responseList.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(responseList)
        payloadManager.updateArchivedAccountState(payloadManager.payload!!.walletBodies!![0].accounts[0], true)
        assert(payloadManager.payload!!.walletBodies!![0].accounts[0].isArchived)
    }

    @Test
    fun `updateMnemonicVerified  should update the account payloads and sync back to the api`() {
        initializeAndDecryptTestWallet("wallet_v4_encrypted_2.txt", "LivingDome1040")
        assert(payloadManager.payload!!.walletBodies!![0].mnemonicVerified)
        val responseList = LinkedList<String>()
        responseList.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(responseList)
        payloadManager.updateMnemonicVerified(false)
        assert(!payloadManager.payload!!.walletBodies!![0].mnemonicVerified)
    }

    @Test
    fun `updateDefaultIndex should update the account payloads and sync back to the api`() {
        initializeAndDecryptTestWallet("wallet_v4_encrypted_2.txt", "LivingDome1040")
        assert(payloadManager.payload!!.walletBodies!![0].defaultAccountIdx == 0)
        val responseList = LinkedList<String>()
        responseList.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(responseList)
        payloadManager.updateDefaultIndex(21)
        assert(payloadManager.payload!!.walletBodies!![0].defaultAccountIdx == 21)
    }
}
