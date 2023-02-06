package info.blockchain.wallet.dynamicselfcustody

import com.blockchain.domain.wallet.CoinType
import info.blockchain.balance.NetworkType
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.payload.data.Derivation
import org.bitcoinj.core.ECKey
import org.junit.Assert
import org.junit.Test
import org.spongycastle.util.encoders.Hex

class DynamicHDAccountTest {

    @Test
    fun `get stx pubkey from DynamicHDAccount`() {
        val mnemonic =
            "local fortune day bubble faint volcano dial brief tower meat view furnace"
        val expectedPubKeyAsHex = "0299c4f35bfed5d3fc0c0568b168de0638e0af88afbe18ea8dcad9351804eb91c4"

        val wallet = HDWalletFactory
            .restoreWallet(
                HDWalletFactory.Language.US,
                mnemonic,
                "",
                1,
                Derivation.LEGACY_PURPOSE
            )
        val stxAccount = wallet.getDynamicHdAccount(stxCoinType)
        val pubKeyAsHex = String(Hex.encode(stxAccount.address.pubKey))

        Assert.assertEquals(
            expectedPubKeyAsHex,
            pubKeyAsHex
        )
    }

    @Test
    fun `get stx signing key from DynamicHDAccount`() {
        // bicycle balcony prefer kid flower pole goose crouch century lady worry flavor
        val hdWallet = getWallet("15e23aa73d25994f1921a1256f93f72c")
        val expectedSigningKey = "d31db6548076af1be636af1d633674258509326dc7ec4794b196fcb38d53569f"
        val stxAccount = hdWallet.getDynamicHdAccount(stxCoinType)
        Assert.assertEquals(
            expectedSigningKey,
            stxAccount.signingKey.privateKeyAsHex
        )
    }

    @Test
    fun `get stx signing key as ECKey from DynamicHDAccount`() {
        // radar blur cabbage chef fix engine embark joy scheme fiction master release
        val hdWallet = getWallet("b0a30c7e93a58094d213c4c0aaba22da")
        val privateKeyAsHex = "389f0c7f676249958c9977d114885ed66e42e2859d3af266ddd4d65482804729"
        val expectedECKey = ECKey.fromPrivate(privateKeyAsHex.toBigInteger(16))
        val stxAccount = hdWallet.getDynamicHdAccount(stxCoinType)
        Assert.assertEquals(
            expectedECKey,
            stxAccount.signingKey.toECKey()
        )
    }

    private fun getWallet(seedHex: String) = HDWalletFactory.restoreWallet(
        HDWalletFactory.Language.US,
        seedHex,
        "",
        1, Derivation.LEGACY_PURPOSE
    )

    companion object {
        private const val STX_TYPE = 5757
        private val stxCoinType = CoinType(
            network = NetworkType.STX,
            type = STX_TYPE,
            purpose = Derivation.LEGACY_PURPOSE
        )
    }
}
