package info.blockchain.wallet.payload.data

import com.blockchain.api.services.NonCustodialBitcoinService
import com.nhaarman.mockitokotlin2.any
import info.blockchain.wallet.WalletApiMockedResponseTest
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.payload.HDWalletsContainer
import info.blockchain.wallet.payload.data.WalletBody.Companion.recoverFromMnemonic
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.parseUnspentOutputsAsUtxoList
import java.math.BigInteger
import java.util.LinkedList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bitcoinj.core.Base58
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class WalletBodyTest : WalletApiMockedResponseTest() {
    private val bitcoinApi = Mockito.mock(
        NonCustodialBitcoinService::class.java
    )

    @Test fun fromJson_1() {
        val body = loadResourceContent("wallet_body_1.txt")
        val wallet = Wallet.fromJson(body, 4)
        val walletBody = wallet.walletBody
        Assert.assertEquals(68, walletBody!!.accounts.size.toLong())
        Assert.assertEquals(
            "i3gtswW35zfbS/23fnh3IzKzcrpD04Tp+zeKbj++rODMOGRMO1aMQukwE3Q+63ds8pUMzBFnzomkjntprhisrQ==",
            walletBody.walletBodyDto.seedHex
        )
        Assert.assertEquals("", walletBody.walletBodyDto.passphrase)
        Assert.assertTrue(walletBody.mnemonicVerified)
        Assert.assertEquals(0, walletBody.defaultAccountIdx.toLong())
    }

    @Test
    fun fromJson_2() {
        val body = loadResourceContent("wallet_body_2.txt")
        val wallet = Wallet.fromJson(body, 4)
        Assert.assertEquals(emptyList<WalletBody>(), wallet.walletBodies)
    }

    @Test
    fun fromJson_6() {
        val body = loadResourceContent("wallet_body_6.txt")
        val wallet = Wallet.fromJson(body, 4)
        val walletBody = wallet.walletBody
        Assert.assertEquals(1, walletBody!!.accounts.size.toLong())
        Assert.assertEquals("bfb70136ef9f973e866dff00817b8070", walletBody.seedHex)
        Assert.assertEquals("somePassPhrase", walletBody.walletBodyDto.passphrase)
        Assert.assertFalse(walletBody.mnemonicVerified)
        Assert.assertEquals(2, walletBody.defaultAccountIdx.toLong())
    }

    @Test
    fun testKotlinSerializerJSON() {

        // Ensure toJson doesn't write any unintended fields
        val body = loadResourceContent("wallet_body_1.txt")
        val wallet = Wallet.fromJson(body, 4)
        val jsonString: String = wallet.toJson()
        val jsonObject = JSONObject(jsonString)
        Assert.assertEquals(9, jsonObject.keySet().size.toLong())
        Assert.assertEquals(5, jsonObject.getJSONArray("hd_wallets").getJSONObject(0).keySet().size)
    }

    @Test
    fun recoverFromMnemonic() {
        val mnemonic = "all all all all all all all all all all all all"
        val xpubs = LinkedList<String>()
        xpubs.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(xpubs)
        val balanceResponse1 = makeBalanceResponse(recoverBalance_5)
        val balanceResponse2 = makeBalanceResponse(recoverBalance_6)
        val balanceResponse3 = makeBalanceResponse(recoverBalance_7)
        val balanceResponse4 = makeBalanceResponse(recoverBalance_8)
        val balanceResponse5 = makeBalanceResponse(recoverBalance_9)
        Mockito.`when`(
            bitcoinApi.getBalance(
                any(), any(), any(), any()
            )
        )
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3)
            .thenReturn(balanceResponse4)
            .thenReturn(balanceResponse5)
        val label = "HDAccount 1"
        val walletBody = recoverFromMnemonic(mnemonic, label, bitcoinApi)
        Assert.assertEquals(walletBody.accounts[0].label, label)
        Assert.assertEquals(10, walletBody.accounts.size.toLong())
    }

    @Test
    fun recoverFromMnemonic_passphrase() {
        val mnemonic = "all all all all all all all all all all all all"
        val balanceResponse1 = makeBalanceResponse(recoverBalance_1)
        val balanceResponse2 = makeBalanceResponse(recoverBalance_2)
        val balanceResponse3 = makeBalanceResponse(recoverBalance_3)
        val balanceResponse4 = makeBalanceResponse(recoverBalance_4)
        Mockito.`when`(
            bitcoinApi.getBalance(
                any(), any(), any(), any()
            )
        )
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3)
            .thenReturn(balanceResponse4)
        val xpubs = LinkedList<String>()
        xpubs.add("HDWallet successfully synced with server")
        mockInterceptor!!.setResponseStringList(xpubs)
        val label = "HDAccount 1"
        val walletBody = recoverFromMnemonic(
            mnemonic,
            "somePassphrase",
            label,
            bitcoinApi
        )
        Assert.assertEquals(walletBody.accounts[0].label, label)
        Assert.assertEquals(1, walletBody.accounts.size.toLong())
    }
    // Available unspents: [8290, 4616, 5860, 3784, 2290, 13990, 8141]

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun hDKeysForSigning() {
        val body = loadResourceContent("hd_wallet_body_1.txt")
        val walletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        walletBody.decryptHDWallet(
            "hello",
            "d14f3d2c-f883-40da-87e2-c8448521ee64",
            5000
        )

        // Available unspents: [8290, 4616, 5860, 3784, 2290, 13990, 8141]
        val body_unspent = loadResourceContent("wallet_body_1_account1_unspent.txt")
        val unspentOutputs = parseUnspentOutputsAsUtxoList(body_unspent)
        val mockApi = Mockito.mock(
            NonCustodialBitcoinService::class.java
        )
        val payment = Payment(mockApi)
        val spendAmount: Long = 40108
        val paymentBundle = payment
            .getSpendableCoins(
                unspentOutputs,
                OutputType.P2PKH,
                OutputType.P2PKH,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(1000L),
                false
            )
        Assert.assertEquals(789, paymentBundle.absoluteFee.toLong())
        val keyList = walletBody
            .getHDKeysForSigning(walletBody.getAccount(0), paymentBundle)

        // Contains 5 matching keys for signing
        Assert.assertEquals(5, keyList.size.toLong())
    }

    // HD seed is encrypted, only xpubs available
    @Test
    fun masterKey() {
        val body = loadResourceContent("hd_wallet_body_2.txt")

        // HD seed is encrypted, only xpubs available
        val walletBody: WalletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        Assert.assertEquals(
            "5F8YjqPVSq9HnXBrDxUmUoDKXsya8q5LGHnAopadTRYE",
            Base58.encode(walletBody.getMasterKey()!!.toDeterministicKey().privKeyBytes)
        )
    }

    @Test(expected = HDWalletException::class)
    fun `masterKey_DecryptionException`() {
        val body = loadResourceContent("hd_wallet_body_1.txt")
        val walletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        walletBody.getMasterKey()
    }

    @Test
    fun mnemonic() {
        val body = loadResourceContent("hd_wallet_body_2.txt")
        val walletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        Assert.assertEquals(
            "[car, region, outdoor, punch, poverty, shadow, insane, claim, one, whisper, learn, alert]",
            walletBody.getMnemonic().toString()
        )
    }

    @Test(expected = HDWalletException::class)
    fun getMnemonic_DecryptionException() {
        val body = loadResourceContent("hd_wallet_body_1.txt")
        val walletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        walletBody.getMnemonic()
    }

    @Test
    fun getXpubToAccountIndexMap() {
        val body = loadResourceContent("hd_wallet_body_1.txt")
        val walletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        val map = walletBody.getXpubToAccountIndexMap()
        Assert.assertEquals(
            0,
            map["xpub6DEe2bJAU7GbP12FBdsBckUkGPzQKMnZXaF2ajz2NCFfYJMEzb5G3oGwYrE6WQjnjhLeB6TgVudV3B9kKtpQmYeBJZLRNyXCobPht2jPUBm"]!!.toInt()
                .toLong()
        )
        Assert.assertEquals(
            1,
            map["xpub6DEe2bJAU7GbQcGHvqgJ4T6pzZUU8j1WqLPyVtaWJFewfjChAKtUX5uRza9rabc6rAgFhXptveBmaoy7ptVGgbYT8KKaJ9E7wmyj5o4aqvr"]!!.toInt()
                .toLong()
        )
        Assert.assertEquals(
            2,
            map["xpub6DEe2bJAU7GbUw3HDGPUY9c77mUcP9xvAWEhx9GReuJM9gppeGxHqBcaYAfrsyY8R6cfVRsuFhi2PokQFYLEQBVpM8p4MTLzEHpVu4SWq9a"]!!.toInt()
                .toLong()
        )
        Assert.assertEquals(
            3,
            map["xpub6DEe2bJAU7GbW4d8d8Cfckg8kbHinDUQYHvXk3AobXNDYwGhaKZ1wZxGCBq67RiYzT3UuQjS3Jy3SGM3b9wz7aHVipE3Bg1HXhLguCgoALJ"]!!.toInt()
                .toLong()
        )
        Assert.assertEquals(
            4,
            map["xpub6DEe2bJAU7GbYjCHygUwVDJYv5fjCUyQ1AHvkM1ecRL2PZ7vYv9a5iRiHjxmRgi3auyaA9NSAw88VwHm4hvw4C8zLbuFjNBcw2Cx7Ymq5zk"]!!.toInt()
                .toLong()
        )
    }

    @Test
    fun `derivationsAfterV4Upgrade`() {
        val body = loadResourceContent("hd_wallet_body_1.txt")
        val walletBody = WalletBody(
            3,
            json.decodeFromString(body),
            HDWalletsContainer()
        )
        walletBody.decryptHDWallet(
            "hello",
            "d14f3d2c-f883-40da-87e2-c8448521ee64",
            5000
        )
        val upgradeWalletBody: WalletBody =
            walletBody.upgradeAccountsToV4(null, "d14f3d2c-f883-40da-87e2-c8448521ee64", 5000)
        for (account in upgradeWalletBody.accounts) {
            if (account is AccountV4) {
                Assert.assertEquals(2, account.derivations.size.toLong())
            }
        }
    }

    private val recoverBalance_1 = """{
    "xpub6BvvF1nwmp51CapAefmDYrKWeGC2Y96TcGtB6BTfiTJezHLjBxgsWdKRvWWChGAhWPjdRjSUsDeEgnSar2xjenixNArkytRU2heAWr3HmQ5": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51MoY8LZ6RqZ6xc9PE5mASd2jpTGARe61HwscsK1tVLF5xJFf1QKnNP2T5YAKDyrK2WGAZS1p5aD9EuYhqC53EFYC7UpnYnz5": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51G75mNUQLmQZm8r3CXBFJChJt6fvoURVS1Nz1jCVN6Nf5nMUfDuT53X8uAXjAX3eHJRPWcpDYMVPwzv1hpMAJKvKQVMefiRJ": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51Gpj1eAidXtRpoq6AUwzZ3L2uv49oWnMQiW9KZ42UYrrM3fHoCyidHzAY14GRrZ8fSS2JZroAEXD5bqiLvjGDNGYbuMCa6vi": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51A6sqV9YTcTGKmWorM48PmZdSXEYgG9pQffDsUavLdPz14RX5tTghiGfApJLqYdv9ramj9agke9o1uKYLesYp6rPKExDmCFX": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    }
}"""
    private val recoverBalance_2 = """{
    "xpub6BvvF1nwmp51N9UVeokUscF6vwT8TN35TSxQmW8GSJPgj7NQwUKrR9rZvug2KLeZf4SnviBmmqgtaWJstuMT18bcNpPttrhrBEWptdYHGcF": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51XdWV7XBeBgcErsvkJ6f79vzppG278gJ4MPfJ9G5mPqaS8w1zWVyhVrXj3nnr2BSaLcNxHVM548go7UvS3MV1uynsi813YrY": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51fx7JnC4c4546HgVt5PJCRf1X2VWmVCJXPztdRxuhUpJYyjzkJQmifMrPAZXBze28og6myNAZSa29PmUXEiTrTrRDUWJhBkm": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51iXFfchx7ov6daSQtm5VafaN47KDvEirVgM7HoYayxndrmpstWt6pWRNKuVUFPjwFxCVPsM3EsXwn5GDosH7BeeMCzhv4tP6": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51e2qGQXDVUX6VMivMHTCFkj9PmDbNQTyedyeansUT6LVrCUxZ2XGtS9e8KybZZ91mDxdPY3FpWE1HRh5vL2RKXc68swdN7MG": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51V9WWEKjQRhqKYmuHj5gkjCr45c4BUAiLkS5y33zcQT39ZnXztG4NSwF98mo4DP1rTyugJsLbFKxDNQCXJegHoULicosyjMG": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51YapEhZtHpMfd1tfjMS2RSd8twpbE8S2aDTxkusxbuwYsJwS1FXPirT3onMXAZvRdMaKjbxSAKDwW6gYc6DgbsWvMcwmC96z": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51nq8joLkkUMDg8x3rFxnSexm5JhXA8Tev9gNW6mk9hp1Lky2aA1HoNVwjyJTubwtYXt7kgehoyzdPitB3osGzKupHhfR568E": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51RG4LdnpS4wW4q7hyjPfujhQ6iWQDKdQPBvjaYQz9CbJD6zYae1M9FfEFCCb2CyjcwPKj2qzQAyYNq3XM5rn1XNanTB8Mc3p": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BvvF1nwmp51akH4ab1pd87W7yxes74P66SSCJhYQAZFKdT1qtSN5dsPfyhnYphP3Bu6EXfz6waPAWkSfzPVsHeSow3yzH9B5SSVbBivLRT": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    }
}"""
    private val recoverBalance_3 = """{
    "xpub6DF4BaPZFrgT9CAStNC5E5We9apLTjjHSa9PHPs4q1wuLPnbAwjVDenrSv4GzdsoadA39b6EhwenAvtYBJmDqFEFtW3VrWeb7UyDDKT8nqA": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTAyDGbafqwnsyHojMGWHAwyqk9FfG3PsyS7RykaotPsVjyqpp2W5PJcF1evtB8h8J98rjWzNm22AgHAc6eBqanmNEc1T3RMU": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTDJjAKYTz7EW3th2488HoadUPiSSoAD2RNYonwa7D3WQQZBoVDHTrLzNpgbtTgAoNFHbeFMAZ7B1Qt5dbaYnK6DTfnMAyjW1": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTGxRq19s8BV7UsHKb483wJ9aphctog8P8wFDqCPKsSceaCComRavmufJkCTHnNf1NdjFcNF6wE1N3V7jCDUBripis5Df39KE": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTJrY4URAJtgdAMWCSrttbUyN5vpzfaiH26Azgt7itwAseGxq9U8nfVBKXMwuG8Hxtdd98pobxBWcoohwYJdyd7dT38VecvHY": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    }
}"""
    private val recoverBalance_4 = """{
    "xpub6DF4BaPZFrgTMa4Uq8PthGNG9fXZBQDE978eNJuPsyUdNT5DuZWqEjvenBm1sogh2dxrfHVSPAEZu2C7seBuYkwk9CYNftZiZf5uZD2JmDD": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTNthzAbgZHEB3K1S1CxWw6Sm6ihLy5epsgTLVZ3EweLWHgRsHUnSpqy4XtakS7XFwwoQ3bkxHge9MPzArpNFAffiwWFUXUCo": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTTbXS1Hjw93MrmX6rCccrxXNYC6n3MNDyFYwghsYD9pSRBDbALJ9vN3SGs6xzn5zwCTGECewwuxrxo56vBuSQJAgitwzymF8": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTV6xrEfePLkTxUwiwAcFfrSSNpVSEwiu6G3sT3hPS3qpteRj3zoXB4g4JWwhF19zRWmdCmAHdPN6SFCpb7kb9A3enAXDHcsH": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTYjTPvzVRNCpGomRFVgE6HqWbN9yP5fP1MMEjBqEn6rGWmbUw6JoiYT9uVXxywxK5MgPCqvi7c5aogmCKTPYnPpmz8Wuut1i": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTYuh5f1KyEHovEDBDx9ckJVVV7mK7eqxXfA9wWN3Q15fkRxMsQv3cRCgZaAFDHek8FJZz2qE2as5johRoR87J14w3RZioBQJ": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTct841Wgv6aKc2sioejTehf4Bx498b4VdvBdbFGnVASf6Jt42p9AWgoTFVtkDzfUUCaZxrmEuHEdE8mxwqWi6WCsc153DDEz": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTeQkoFQq2XGhdtDSbugaa3Gkzt3UQDT1RLq2r7niNnZgdhDN2w6aiWkHh1pxLX18rnLfHgzaESymz9CCw4CQz8w2xDNHwHin": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTiTFmkv6noTHuwtjhEQYacwRv6fPXpqzrhKaDRYCtJbxruwFTEcTYbK6K9ydHfBihXUeoY9etUVG1nHGYpdLrk5pH2TqPNDy": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DF4BaPZFrgTkVYR7Z2bYK3sGzWoRghDC5GwxzPwPUuE2VjcpSye83bVhmhzoj3acWXnBY8CrYY3jaVdxn1VyG9q57VwwbN1utGGxD5TvSV": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    }
}"""
    private val recoverBalance_5 = """{
    "xpub6BiVtCpG9fQQ77Qr7WArXSG3yWYm2bkRYpoSYtRkVEAk5nrcULBG8AeRYMMKVUXAsNeXdR7TGuL6SkUc4RF2YC7X4afLyZrT9NrrUFyotkH": {
        "final_balance": 0,
        "n_tx": 23,
        "total_received": 22154257
    },
    "xpub6BiVtCpG9fQQ4xJHzNkdmqspAeMdBTDFZ2kYM39RzDYMAcb4wtkWZNSu7k3BbJgoPgTzx62G69mBiUjDnD3EJrTA5ZYZg4vfz1YWcGBnX2x": {
        "final_balance": 0,
        "n_tx": 20,
        "total_received": 18192818
    },
    "xpub6BiVtCpG9fQQ1EW99bMSYwySbPWvzTFRQZCFgTmV3samLSZAYU7C3f4Je9vkNh7h1GAWi5Fn93BwoGBy9EAXbWTTgTnVKAbthHpxM1fXVRL": {
        "final_balance": 0,
        "n_tx": 36,
        "total_received": 12099702
    },
    "xpub6BiVtCpG9fQQ8pVjVF7jm3kLahkNbQRkWGUvzsKQpXWYvhYD4d4UDADxZUL4xp9UwsDT5YgwNKofTWRtwJgnHkbNxuzLDho4mxfS9KLesGP": {
        "final_balance": 0,
        "n_tx": 20,
        "total_received": 11963629
    },
    "xpub6BiVtCpG9fQQCgxA541qm9qZ9VrGLScde4zsAMj2d15ewiMysCAnbgvSDSZXhFUdsyA2BfzzMrMFJbC4VSkXbzrXLZRitAmUVURmivxxqMJ": {
        "final_balance": 0,
        "n_tx": 20,
        "total_received": 15137242
    }
}"""
    private val recoverBalance_6 = """{
    "xpub6BiVtCpG9fQQGq7bXBjjf5zyguEXHrmxDu4t7pdTFUtDWD5epi4ecKmWBTMHvPQtRmQnby8gET7ArTzxjL4SNYdD2RYSdjk7fwYeEDMzkce": {
        "final_balance": 0,
        "n_tx": 2,
        "total_received": 4242108
    },
    "xpub6BiVtCpG9fQQUGTtsZvQdWaXHNmNd1Rzo8C8kfhzJQsLw1nijQ3HNSGMrLyMygHMvRTv9SL7o29hMPrtC32vfoW3NkGjCETYZpH4s6isLX3": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQNBuKZoKzhzmENDKdCeXQsNVPF2Ynt8rhyYznmPURQNDmnNnX9SYahZ1DVTaNtsh3pJ4b2jKvsZhpv2oVj76YETCGztKJ3LM": {
        "final_balance": 0,
        "n_tx": 9,
        "total_received": 4346308
    },
    "xpub6BiVtCpG9fQQXPkGarFwhcPbhRN5TEfpCfHPe37cdG7iYgYMjt85hZ1HHPAbqYneHs4bZtJ47dGRncD2z5q1aix83zgjEwQ3KkNuyyK8eFx": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQdziwDT8EyYPLnuXs14FwNZqGHhMzPDMdLKc97agwFKMb3FfiweRsnqkeHYymF31RJc9EozZxHUSHzkjQ2H9SKGe7GmRDGPM": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQV7PkQJFKHKs2BQVYJ2k7bF8E2dTtqb61viou61EaAm2McoArGW2pjfe8wxLmESVEcDo4pHLLe2KZkLthXBXBR8rvem35ZnN": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQDvwDNekCEzAr3gYcoGXEF27bMwSBsCVP3bJYdUZ6m3jhv9vSG7hVxff3VEfnfK4fcMr2YRwfTfHcJwM4ioS6Eiwnrm1wcuf": {
        "final_balance": 0,
        "n_tx": 9,
        "total_received": 4785453
    },
    "xpub6BiVtCpG9fQQamLupKW3xzULucDGpsp3KgKfVdfmP65MJPJ6bU7UuaKBZeUYQW58hU5iAKEdMJHeQNsMEquLMf8he4M6wZ3fA6P1vAHGdhH": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQJXDcLwQU1cXECNqaGYb3nNSu1ZEuwFKMXjDbCni6eMhN6rFkdxQsgF1amKAqeLSN63zrYPKJ3GU2ppowBWZSdGBk7QUxgLV": {
        "final_balance": 0,
        "n_tx": 4,
        "total_received": 4285772
    },
    "xpub6BiVtCpG9fQQR6cSuFeDaSvCDgNvNme499JUGX4RHDiZVWwZy9NwNieWKXHLe8XRbdrEmY87aqztBCbRJkXWV7VJB96XBT5cpkqYMHwvLWB": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    }
}"""
    private val recoverBalance_7 = """{
    "xpub6BiVtCpG9fQR4Bp1D4k4P1a48uHPJPtHmnHjrvwpZgg47sJfg9e5wqjEVZs1YdhR3EsfWo16qPcA7fsk6Hzr5e8VAjNbgmVy67DGkoGJfv4": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQmHu21ccttmBpbz5uT8zUQ5nXoTBkMbJBAZ35KTZ9bCi6ChqHZFUc6D2UnrZwLWZZqye9GDtRDw8T9kxEt13fN2UVFBgBEzJ": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRBDv37eyBUDVV4Wpp5w5G1ZdCBwv3cBEUor71SXG48SqYtKccateyEfjoRwDYSojk8XDkBaK6HrGt4A68oJzb536gPQG5c36": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRCXQmCHhnL9AqkNyVEesEsP7xunYZrtboZpqUEne9MQqGc9dZDryV27179yfD9rRQsxErUwwEgDKz1EJLS9i1sh6XPG8yoH6": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRSxhKnoTeamr7c6LnWkFqUASymUyfga1r2sjttqqyjXk5N5ec36HfD1XL2475EwMsN3pSyvDhuqU7v4Rv6mryVNGjSrhzFc1": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQgFPkux7cvyVgyAwWWYRG935BDBYTcXEJGnr1H3vfTfaA8Zg2pRPKxLPKRSY9ztrirhhD2Ud4KKeR11oWpomvNUY8jgXcSWN": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQrFR7q8AQCrDwH5ZiPv9ozL6xg4eiXCGCTDYmw4uZkZgYDfaS42jeA2tjMWQ2vzaHtRjFTTMYCYpGEsgFku324rRdMDckp1i": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRPvqDD7BCc9fCy1WhR7h7N3QnVPHW3QrhmXXrzpUnWogvr2x6ekxX1jCCvyDo11jz75zzC6AS2TU5DE7CcV6xaLt1X8h3iPe": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQwh4qKjcJnN2NdZWDqKjKsAMFMbmrsgAKSQBhZDc5cubR5ZhoBt51jjZY79NYpVpRJBWJAHsnaXtT6uqQ6Ps9d1YkDpY7QH2": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQhRocxvUvFdoG8FJqm8PYPjgGKrryMpRDCcHjzXkYbjuEDFdPRKUv7jJ5H2FUuCFLY2FBNA7gosDpf36coCvBtc48DkoqX5M": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRVMuTAkEfkJmL7vsFbw8cRtexWLnG98KSnHy4akwmQrLUnszJL4TTvuvKgtCGr6s7fS8py5ZaGfBupSxy8qynUUg9ynP66L8": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQR6krjJPX1Gi9R5aPzGHCgjL6CEEeYRemjYPuTocawpXgJLMNbJvrToG94hVpT4RxakNde8UEnePB2rJKheeUd89dvaHK7cnT": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRLT8VREWcckFJD7syF6hF6W7PKcbyjKpQdh2Aj46zm2nXaLRJmTak6E9VBq2c5ZDMuJNU4dvkEsRfZXXQL7Agxwy2pURfWBA": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQxjv3ciBgJguCKHLN25rUzZDnDPRLssQVqtZJTgRFPNMXpkE5RUSBxiBx4twZ9ecfHtNUnxvsURC9whqRazKa6ziYSC9rvEj": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQR2aaiVePK2BVjSwVa8uNe3uKTKZEtagRqarGM4BHcFV7K6KS5cUU5DkFJLDvwH289DskNRysWZZtmBKZb4fTCXQgNjtrwsun": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRGMmvS6eCvosDg67t6hwX5pp3cLLNUvsoTHHpK2Yf6cYRJgK6XYVzrHHV5YL42kdUaz1oQdbyTdJCfpiehde5r6SVJHUHs8A": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQprM8465d1xUgyrh1KY7UrkPa9f5pt58znTb4rf1bgvvGgxj3ASXvcn9yyGqinFcV7n5LpW5vAg95k99zYZ5BXFQS4J7yZw9": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRJVkYhiHDzuZyDeCmch3agVDwV9ryfx87gPBXa7LbSHWM9sn9aFmSgouwDLbH2hesXt8VZUrzKdMdMbJ3ayxbfgygqZqRrsV": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQRRqpQafDuYL8X1N2ffjW1UhUZ2TU1H8dJhprANMLAuF2bqrK3iPWxHzEvFWQaEga3wkq956WVfZBL1fgNWMyK1YMkQbeTnBt": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6BiVtCpG9fQQuhhzr1YNBXEPPCLBYVu6FpSYYpeBHaSVBDhavkcTZZr7ZGPULBUzAQ8QaxYtG5U1KyBq61cAP6VedtPXYvXu1ch4rNLDZYd": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    }
}"""
    private val recoverBalance_8 = """{
    "xpub6DDUPHpUo4pd1hyVtRaknvZvCgdPdEDMKx3bB5UFcx73pEHRDVK4rwEZUgeUbVuYWGMNLvuBHp5WeyPevN2Gv7m9FnLHQE6XaKNRPZcYcHH": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pd5Z4Dmuk7igUc5DcYBoJXcVA1NJbKaRX1M2WKsTqHF5igMbwLpA23iHBwPXY11cidR2kiJVsQWfuJgaQJuxFrjm7iEhsMm4y": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pd97vxcds4Qf1oN6zgLqSJLj17Es2mTozPawXwztmDTm9EDzBthkCoArbawqa66hd3v9Kx1h7ekiMKGb5ywWFCRJjTMNC85Zq": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pd9Uvp828RbCzZugQVZy78HupxyvtbwBCbWJWYRtf9eFAuVTgsQQS8wWzx8gHw3MUy8NAGUXBqM9Sm3YwzSYMF6gMcXGwV8Rp": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pdCGoBKy5XuYCAKp7wz5LG4xr3YS8wuHBFPLYmFZ5VLsHhDKygeDFoo5A45MbomhzQvKuCwwNeAzYhuHQmnFa3gaHV7rJREEW": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   }
}"""
    private val recoverBalance_9 = """{
    "xpub6DDUPHpUo4pdFBMHCpQAAoC6riigM4anNgqZbMQuDm7jVikGMNQ3Vax6RvSUruUAyNkestzxydQwoUWHGrwt8dbGDHN488DKxWzQR615vxf": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pdJkch1JQLNptgvxZxYFcukZmShjwLXhFQkEt5qQvo1kURQCXFa6jLpdqPXZz4PQ3zkCJY5ANiETaFzgUz3wotwM8gyWGq951": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pdLNsdRMwHP5N6NENYWg2FtXo9LFybC6Z6B2t81EibDZER4Lg7eHGd5fJUn52YqZNQB8zd1svoPqPBzje7UtvUULRW5KAouGA": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pdNWG2DeJoHr7NPj38WgvhSqE5Do39nKZrwjTLFHZuDiBozMYew5VVpCXtwNVBKmRun1e2QHH3BJHfa1mui9JYAhGx4pRd6wq": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
    },
    "xpub6DDUPHpUo4pdSFNkVDW7tzJXQjL9Pgr553EtkRrdTncrr4rhJTzfMjQNxgJyrxKTzJPcPguSDMCV1MjJEDZVzYjx75sZGfHbMRcbGrci5o8": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   },
    "xpub6DDUPHpUo4pdTw4sbJRGiA7w22786dNR53eUu3XkMAmZR7pkKVdyZ2d56fdgdXxWvqLjfyY85unpiHVe8CEc41dQxSmmcg3bQH5BSc25cho": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   },
    "xpub6DDUPHpUo4pdWSBc5C2eCyPsgSUXwTUpeB17rkh3EcvE62GRLibiPm3WanCkrYvEifpUQuuMf7H8nzG7yqYQtDpmV9GLrApwCLkFhD1A5bt": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   },
    "xpub6DDUPHpUo4pdWSBc5C2eCyPsgSUXwTUpeB17rkh3EcvE62GRLibiPm3WanCkrYvEifpUQuuMf7H8nzG7yqYQtDpmV9GLrApwCLkFhD1A5bt": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   },
    "xpub6DDUPHpUo4pdZKGpg28gZicqwqNLjAKhbbTPHaYYtYTvh4bvPkxHbQNeB2gNcQQNy5Wu6HZkegWLXoo5TjE3gTwpw7aa34wTR5pQYjX61zG": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   },
    "xpub6DDUPHpUo4pdd5gSNBcH1DKk5T4Ah9Ud5UqTjStKUSDquWS3ndZP5cgisBtfhFiPAigs5opkKhjDifa8CtJuznrTjawvA7Ch2rkN1w1H9k5": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   },
    "xpub6DDUPHpUo4pddNa6JG4FQuzpQ3isztCBtfgJWWZRD2eoYT3ipyDguFcoS1BnaH61YMYTCi3zEXhWuHUzPY1mVFwS7sLALzJjwmkyQan4rgJ": {
        "final_balance": 0,
        "n_tx": 0,
        "total_received": 0
   }
}"""
}
