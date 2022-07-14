package info.blockchain.wallet.payload.data

import info.blockchain.wallet.MockedResponseTest
import org.junit.Assert
import org.junit.Test

class WalletWrapperTest : MockedResponseTest() {

    @Test
    fun `Wallet wrapper should be parsed normally`() {
        val body: String = loadResourceContent("wallet_wrapper.txt")
        val walletWrapper = WalletWrapper.fromJson(body)

        Assert.assertEquals("test_payload", walletWrapper.payload)
        Assert.assertEquals(
            7,
            walletWrapper.pbkdf2Iterations
        )
        Assert.assertEquals(
            4,
            walletWrapper.version
        )
    }

    @Test
    fun `Wallet wrapper with no payload should be parsed normally`() {
        val body: String = loadResourceContent("wallet_wrapper_2.txt")
        val walletWrapper = WalletWrapper.fromJson(body)

        Assert.assertEquals(null, walletWrapper.payload)
        Assert.assertEquals(
            7,
            walletWrapper.pbkdf2Iterations
        )
        Assert.assertEquals(
            4,
            walletWrapper.version
        )
    }

    @Test
    fun `Wallet wrapper should be serialised normally with no payload`() {
        val body: String = loadResourceContent("wallet_wrapper_2.txt")
        val walletWrapper = WalletWrapper.fromJson(body)

        Assert.assertEquals(
            "{\"version\":4,\"pbkdf2_iterations\":7}",
            walletWrapper.toJson()
        )
    }

    @Test
    fun `Wallet wrapper should be serialised normally`() {
        val body: String = loadResourceContent("wallet_wrapper.txt")
        val walletWrapper = WalletWrapper.fromJson(body)

        Assert.assertEquals(
            "{\"version\":4,\"pbkdf2_iterations\":7,\"payload\":\"test_payload\"}",
            walletWrapper.toJson()
        )
    }
}
