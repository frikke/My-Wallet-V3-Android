package com.blockchain.core.payload

import com.blockchain.AppVersion
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.Device
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.AccountV4
import info.blockchain.wallet.payload.data.AddressCache
import info.blockchain.wallet.payload.data.Derivation
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

/**
 * ð
 */
class PayloadDataManagerIntegrationTest {
    private val walletApi: WalletApi = mock()
    private val payloadManager = spy(
        PayloadManager(
            walletApi,
            mock(),
            mock(),
            mock(),
            mock(),
            object : Device {
                override val osType: String
                    get() = "Asda"
            },
            mock(),
            object : AppVersion {
                override val appVersion: String
                    get() = "23"
            }
        )
    )
    private val payloadService: PayloadService = PayloadService(payloadManager)
    private lateinit var subject: PayloadDataManager

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        mainTrampoline()
    }

    @Before
    fun setup() {
        val response = Response.success(mock<ResponseBody>())
        val fetchWalletDataResponse = Response.success<ResponseBody>(
            (
                "{\"extra_seed\":\"55a5cbe20a56866034fc2887af719fb9902045d18d645a84c0044de62a2bdd1e3054223b0c09d8baadf42eb73515213a75cb4ae31354f225a2ac4606344" +
                    "4a3af\",\"payload\":\"{\\\"version\\\":4,\\\"pbkdf2_iterations\\\":5000,\\\"payload\\\":\\\"kQAUYc5TIUdBovvdCLEVcVrcFxkXw" +
                    "o5KBsNzABnx6U3+WJeIETTXFXEv2c\\/WPaCYuL2PqSJ+Hfj9g1G0BnhQnBLhKQpIvcloskmiUxTZ500i3d+lMmWYEGtQXZ2cDm5okg\\/7OnUYMit8iPmyNjP4l" +
                    "zKvfs3xEyQXRvM\\/d\\/UsI7orfVBc69kB3sx3ta4uVkExehmu2f0h8bN6SiM4HYCdgDqy2UYIeZT0m7wz\\/2mpICcO8RefLXvxu2aW9W\\/7eLk5taAhqw2M9Ar+" +
                    "FF5Zl9uVJEEout6\\/lbBVRyF5MnxtXm7fWrogUoOEc5RZAOGxWg3Ox76t86sw5ro6XmHD3nsgx1lt5Alpi72UIDoDiZhTCqt92VDrV5i8VGxw5JewEd8m7ouJhwGm5g\\/IwNUdGm" +
                    "IMtDYW2CCp6VjPpfhxQTYLkvZlhusRjsB3cfAC7mffcrVDShBBj1cT3OFCZUqHldEoUIzxrssyRx5hylMVN2jSv5Pr2ws2ffZsoeEJ79VQCL6al5JGXbFQFyhUcO+8ipgmDRqeflJ9rV" +
                    "aFKY5NmY4nMmkueaspgkgFpBRT7HyHGQUFRB6HwGIaHVimRkX2B1BtE\\/BciF9SJUwOWMZ6u\\/+y63C6vOipjRRiHheId3m5tc5zAQEFwdMYm7rCnrOTrt7EXrNqZuO3ObIYgtP29ScqpE7" +
                    "jCr6fSnwhPARLETzxsaT6+etZuOKVIoELsdV1GTk18ZU3zEX6u8E8HcltlgAg1ldSv0EcTfKMBxwagKZCaJxpItj0cE0xCzrY9UHloy2nNNgjTOtxKu55eiSksLiPb7GI1\\/2wBtlGv0iF+Xam" +
                    "sNe04nl9i9Y18EkxTDHTA2ry9bKTKG5qRDluAMusT31O9F6CbW+LzHE\\/a2\\/mkuxxaY\\/PGp1zhEXHiaaOnafTjPr2Q5\\/Y0TfKLy18D5uHTIGuMR3P2jBbkXIqFdxCwS+RkTWI\\/tWWa" +
                    "p9nvOp6xHCKFmUQjJXvG8ljovg1k2bgt5CX7AvxtYfYmTLGW9zzU7agfDasbBrNmwF3qRSJogkWwO1xDLW2Au82i\\/3Vf9PgUQQseRs\\/c\\/TFE18uZn\\/e\\/K7Fr54tx7j7qOkAaE0JXFKx" +
                    "HdZjs2O4Sw1QOIRh0F9qghfSUcLtAIoIEWQOGiYZtMbyLnLt0s+fdu3B0FU15Ha5ZkkoQjMyIG1IfoCLft5NdBdWatpuiyvJr9FB48IjFSc91+4tCZOCo6mb3BTaOBIlB6\\/OTyjVvXgujafmntL" +
                    "mEifrLudFiN32+zOQ3JgaHRxi0L7JwL8w+HVxZUgfZ6SwBHOEi1qaQrLvd4rPxTqGSjizwMEq5p9mh4l6PJ4Q2i1ah8cuoUCvBcioAxLUR9oae4D38kxc4I+jDNGVTkaNy8O5yKHXH0e7dipv2FEu" +
                    "X5WpsFIbTPjNu73vrQGfS\\/VhldyymQh13oQoXsrPTXSUE0QJT9EkqM9NyUi\\/YGIIRi60vBBacNKD5QVo\\/2pwrHXNX+VT0ZVfYad3sOaAa09m2qp90gFnQG9xRP\\/cD314Ag2Wt7LaQ2Ta" +
                    "dOZh872NJQcN0yNr+ca\\/VE4pO0jreJU8FU5cwFrvekPbL9a3vdeKql05NvwhZQsRX3JoBrUODsOm3pL3JW0oySwRganXD1DGw4+38\\/OZL4RPL00gqpTZ46COBppoL3BY3eEa" +
                    "b6dqkLHDpzdS\\/GoZewlHpRvr2uwEaBESMbjnMwDn\\/fne45tmbjAD0CfNp+8+yk8BjMgPo0oOTRrflXthrKqWNvJt2ZuQNphfbu\\/T\\/KnMeCpjuK90v8BHqP0MRLQoBU55" +
                    "xBhXpWYfqNI7ghJr20l7vduBLZWBYfJ7K+AO\\/0hHnjwgqZnywdho4EtWRx3VD1YaphVEhmV6TUCcOTswVpDvoQn6S1ZE7D6eyvmhZ3EbG2pfON8xXSPO52XH7GjBQYU5N7kHqy" +
                    "e67D1li9IIK8j9jxHZN3R9FN73\\/8ZGAbKMVfoEy6nIUb++2EnoXoMxXvvY9i0akE9jyI9K\\/XpfDxnCsx3gja5J1ZJhXvTuvv75eIJ6o\\/E0vjALRpT9yzlt2MxgAj5+5xNyKM" +
                    "C8gY1M5t4hpRh6dVHusbPsTPw=\\\"}\",\"symbol_local\":{\"symbol\":\"\$\",\"code\":\"USD\",\"symbolAppearsAfter\":false,\"na" +
                    "me\":\"U.S. dollar\",\"local\":true,\"conversion\":0},\"payload_checksum\":\"1db6acfcce7fdcb2780dd5a7b6f8c7e32ff50702a8c851" +
                    "317a3903d86b6dea9b\",\"war_checksum\":\"1b3fdc8d8c5439f0\",\"language\":\"en\",\"symbol_btc\":{\"symbol\":\"BTC\",\"code\":\"BTC\",\"symbolAp" +
                    "pearsAfter\":true,\"name\":\"Bitcoin\",\"local\":false,\"conversion\":100000000.00000000},\"storage_token\":\"c1e11432bc156344c55e21a207" +
                    "03d1c4\",\"sync_pubkeys\":false}"
                ).toResponseBody(
                "application/json".toMediaTypeOrNull()
            )
        )
        val mockCall = mock<Call<ResponseBody>> {
            on { execute() }.thenReturn(response)
        }
        val mockCallFetchWalletData = mock<Call<ResponseBody>> {
            on { execute() }.thenReturn(fetchWalletDataResponse)
        }
        whenever(walletApi.updateWallet(any(), any(), any(), any(), any(), any(), any())).thenReturn(mockCall)
        whenever(walletApi.fetchWalletData(any(), any())).thenReturn(mockCallFetchWalletData)

        subject = PayloadDataManager(
            payloadService,
            mock(),
            mock(),
            payloadManager,
            mock(),
        )
    }

    @Test
    fun `initialiseAndDecrypt should not throw any exceptions`() {
        val test = subject.initializeAndDecrypt(
            sharedKey = "17a31bd7-49d4-45b5-8835-7a663b082fcb",
            guid = "d5ec733a-e018-4f2b-9154-f1d6431e9eba",
            password = "1234"
        ).test()
        test.assertComplete()

        subject.guid `should be equal to` "d5ec733a-e018-4f2b-9154-f1d6431e9eba"
        subject.sharedKey `should be equal to` "17a31bd7-49d4-45b5-8835-7a663b082fcb"
        subject.isWalletUpgradeRequired `should be equal to` false
        subject.isDoubleEncrypted `should be equal to` false
        subject.mnemonic.joinToString() `should be equal to` "cook, tilt, thought, chuckle, regret, priority, country, useful, seven, field, title, amused"
        subject.payloadChecksum `should be equal to` "1db6acfcce7fdcb2780dd5a7b6f8c7e32ff50702a8c851317a3903d86b6dea9b"
        subject.defaultAccount `should be equal to` AccountV4(
            label = "Private Key Wallet",
            _defaultType = "bech32", _isArchived = null,
            derivations = listOf(
                Derivation(
                    type = "legacy", purpose = 44,
                    xpriv = "xprv9y8noc7G5TnerpABXnnX5rUvKaEG1bUsAGnK8tHGZM33tF4x2CpZe69FH8aXoAEBRf5yDELSxhz6C76XB49BNrfoujBE91dtfk3rxhgJa7j",
                    xpub = "xpub6C89D7e9uqLx5JEedpKXSzResc4kR4CiXVhuwGgt7ga2m3Q6Zk8pBtTj8PHhTwezeptj6TpwL8i72FgMHrwoSmynfnEaoeGxHZq5DyFK6Lo",
                    cache = AddressCache(
                        _receiveAccount = "xpub6EUSVd5FeWUdoFatehs8x1Rc5WsgMHhkUGNEs9RDWYQzQPQBgsFXjHPBKPQ3AsZKsocW6cNrM2mKKrgbZvsadUQAaXXfWi96t4j71hLLpCq",
                        _changeAccount = "xpub6EUSVd5FeWUdsDshHMcMWwhGVVugfYviocSmB6VsjuFVRCaJruhAWZBVaFaZurpc1R87oVPSPMUqoScAmCJjuHEBZQ2ZUW4u2GwXDwyfr1B"
                    ),
                    _addressLabels = null
                ),
                Derivation(
                    type = "bech32", purpose = 84,
                    xpriv = "xprv9yCt31fSgZ2LUZ36xEwqryrjqGn19naBDe8wXh3Cg93sjPsT8zMcRbb42imnGL86FhjNkDD3ACmf6a2wFSqtJQqf5Ncjfj9k7oNGcUXrxH3",
                    xpub = "xpub6CCESXCLWvadh37a4GUrE7oUPJcVZFJ2as4YL5SpEUarcCCbgXfryPuXszwJRd3Mo7hsZgeMAtVKybeJwFmVkdTzdLv9cAintb8ymK38j1U",
                    cache = AddressCache(
                        _receiveAccount = "xpub6DqLELQ9ASryfCVyaV9hQFrhGKkJgWQvvudkFTz4pcz6QsQqH8CbGreCsJLvb9QSzvK9Hg662ugjNxCUrfqEe3aVZNnRNAMWYPp83uWJnxj",
                        _changeAccount = "xpub6DqLELQ9ASryi41aFb2YKfZ2zW2TB4YB5bHmRGD87FnwSj3JiUXXZr1TiPPvYyhWoiMR5KiQUUSHgkiSSGm4UnQFb7BtmJ4b8guokqyV1Jz"
                    ),
                    _addressLabels = null
                )
            )
        )
        subject.masterKey.toDeterministicKey().pathAsString `should be equal to` "m"

        verify(payloadManager, never()).updateDerivationsForAccounts(any())
        verify(payloadManager, never()).updateDefaultIndex(any())
    }

    @Test
    fun `When v2 payload detected then it should be updated to v4`() {
        val fetchWalletV2DataResponse = Response.success<ResponseBody>(
            (
                "{\"extra_seed\":\"48c9b51c508f3b3cadc32ffa586f66399ef5d3b7892e603a4378b95a1f9e7f71ed69d8f296ecfbb8905dd2f5364dbbb0" +
                    "15b1acc51268f8f24c94631b1593ee3e\",\"auth_type\":0,\"initial_success\":\"Reminder: Verify your email.\",\"real" +
                    "_auth_type\":0,\"payload\":\"{\\\"pbkdf2_iterations\\\":5000,\\\"version\\\":2,\\\"payload\\\":\\\"tk2mSGtp" +
                    "EsW8Um\\/2aXaA7Gd\\/k8ucqswLpzZ1OU5Q1p4csILNhycufhXhgleIInQzNz8jLOzB\\/zcpkxBCSWRLvSNNB1F\\/WAnMd5Eop7xSQMxDo3At1c" +
                    "5SSs8QiDoBlI2XSYtbWaufdZB6145ASrHM6uB40pmhnxsdDnpzoY7mXQjpNCls2ljX1o6c4SoqsevpLEiPxvvf9GFjVKvQpJyaUfNBSMxSxP2qu+HHjbvdlsX" +
                    "enGEKNRkX7fVtIMZxdjQIJXLh\\/Wi+LDpCkTUXEmv7lqSuwr5hCQ2zz76NyJn1acMv7\\/HnR6VpFQOev11nc1alp3AIKaq0JcDUHjQ23KOBWmCEt+1m\\/And7KN8b" +
                    "gLNBBWygaFwHcVvUFPHwTi8+723VKNZUNmdncxLl50MXob7c2DTfUfKoi0S1xoIJd+TJfsEFtMOjBQtEvZfBv\\/sbhSnJVtuy4Qp6BP5D\\/P\\/1iK" +
                    "dY8oU5AC5FEv\\/adCG4qDHKgdzG3wB1HHEsLypZivrR5Q+\\\"}\",\"symbol_local\":{\"symbol\":\"\$\",\"code\":\"USD\",\"symbolAppe" +
                    "arsAfter\":false,\"name\":\"U.S. dollar\",\"local\":true,\"conversion\":0},\"guid\":\"fa311856-db2a-4939-b6c3-9b5a05eda5b5\",\"payload_che" +
                    "cksum\":\"aba44efae23a007c411498e557fe61d49efd111385ed713af33d6a2f107d858e\",\"war_checksum\":\"1b3fdc8d8c5439f0\",\"language\":\"en\",\"sym" +
                    "bol_btc\":{\"symbol\":\"BTC\",\"code\":\"BTC\",\"symbolAppearsAfter\":true,\"name\":\"Bitcoin\",\"local\":false,\"conversion\":100000000.00" +
                    "000000},\"sync_pubkeys\":false}\n" +
                    ""
                ).toResponseBody(
                "application/json".toMediaTypeOrNull()
            )
        )
        val mockCallFetchWalletData = mock<Call<ResponseBody>> {
            on { execute() }.thenReturn(fetchWalletV2DataResponse)
        }
        whenever(walletApi.fetchWalletData(any(), any())).thenReturn(mockCallFetchWalletData)
        val test = subject.initializeAndDecrypt(
            sharedKey = "38f39fad-fd5d-449a-85e9-adcc84fffcf1",
            guid = "fa311856-db2a-4939-b6c3-9b5a05eda5b5",
            password = "1234"
        ).test()
        test.assertComplete()

        val upgradeWalletPayloadTest = subject.upgradeWalletPayload(
            null,
            "lalala"
        ).test()
        upgradeWalletPayloadTest.assertComplete()
        /**
         * Verify the corresponding methods get called
         */
        verify(payloadManager).upgradeV2PayloadToV3(null, "lalala")
        verify(payloadManager).upgradeV3PayloadToV4(null)
        /**
         * verify local data updated
         */
        verify(walletApi, times(2)/*One for v2->v3 and one for v3->v4*/).updateWallet(
            eq("fa311856-db2a-4939-b6c3-9b5a05eda5b5"),
            eq("38f39fad-fd5d-449a-85e9-adcc84fffcf1"),
            any(),
            any(),
            any(),
            any(),
            eq("Asda")
        )
        /**
         * verify we now have v4
         */
        subject.isWalletUpgradeRequired `should be equal to` false
        assert(subject.defaultAccount is AccountV4)
        val derivations = (subject.defaultAccount as AccountV4).derivations
        derivations.size `should be` 2
        derivations.first { it.type == "legacy" && it.purpose == 44 }
        derivations.first { it.type == "bech32" && it.purpose == 84 }
        subject.accounts.size `should be equal to` 1
    }
}
