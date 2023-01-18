package piuk.blockchain.android.util

import android.content.res.Resources
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.`should be equal to`
import org.junit.Test
import piuk.blockchain.android.R

class ResourceDefaultLabelsTest {

    private val resources: Resources = mock {
        on { getString(R.string.default_v2_crypto_non_custodial_wallet_label) }.thenReturn("Private Key")
    }

    private val defaultLabels: DefaultLabels =
        ResourceDefaultLabels(resources)

    @Test
    fun `btc default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should be equal to` "Private Key"
    }

    @Test
    fun `ether default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should be equal to` "Private Key"
    }

    @Test
    fun `bch default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should be equal to` "Private Key"
    }

    @Test
    fun `xlm default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should be equal to` "Private Key"
    }
}
