package com.blockchain.coincore

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.DomainAddressNotFound
import com.blockchain.coincore.eth.EthAddress
import com.blockchain.coincore.eth.EthAsset
import com.blockchain.testutils.waitForCompletionWithoutErrors
import info.blockchain.balance.CryptoCurrency
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.CompositeException
import org.junit.Rule
import org.junit.Test

class AddressTest {

    @get:Rule
    val initSchedulers = rxInit {
        computationTrampoline()
        ioTrampoline()
    }

    private val ethAsset: EthAsset = mockk()
    private val coincore: Coincore = mockk {
        every { get(CryptoCurrency.ETHER) } returns ethAsset
    }
    private val addressResolver: AddressMappingService = mockk()

    private val subject: AddressFactory = AddressFactoryImpl(
        coincore = coincore,
        addressResolver = addressResolver
    )

    @Test
    fun `successfully validate ETH addresss`() {
        val ethAddress = "valid1Address"
        val receiveAddress = EthAddress(ethAddress)
        every { ethAsset.parseAddress(ethAddress) } returns Maybe.just(receiveAddress)

        subject.parse(ethAddress, CryptoCurrency.ETHER).test()
            .waitForCompletionWithoutErrors()
            .assertValue(receiveAddress)
        verify { coincore[CryptoCurrency.ETHER] }
        verify { ethAsset.parseAddress(ethAddress) }
    }

    @Test
    fun `successfully validate EIP-681 payment link`() {
        val ethAddress = "ethereum:valid1Address/transfer?address=valid2Address&uint256=0.1"
        val receiveAddress = EthAddress(ethAddress)
        every { ethAsset.parseAddress(ethAddress) } returns Maybe.just(receiveAddress)

        subject.parse(ethAddress, CryptoCurrency.ETHER).test()
            .waitForCompletionWithoutErrors()
            .assertValue(receiveAddress)
        verify { coincore[CryptoCurrency.ETHER] }
        verify { ethAsset.parseAddress(ethAddress) }
    }

    @Test
    fun `successfully validate domain`() {
        val domain = "domain.crypto"
        val resolvedAddress = "resolvedAddress"
        val receiveAddress = EthAddress(resolvedAddress)
        every { ethAsset.parseAddress(resolvedAddress, domain, true) } returns Maybe.just(receiveAddress)

        every {
            addressResolver.resolveAssetAddress(domain, CryptoCurrency.ETHER.networkTicker)
        } returns Single.just(resolvedAddress)

        subject.parse(domain, CryptoCurrency.ETHER).test()
            .assertValue(receiveAddress)

        verify { addressResolver.resolveAssetAddress(domain, CryptoCurrency.ETHER.networkTicker) }
        verify { coincore[CryptoCurrency.ETHER] }
        verify { ethAsset.parseAddress(resolvedAddress, domain, true) }
    }

    @Test
    fun `validate emojis as domains`() {
        val emojisDomain = "🌊🐙️🌊"
        val resolvedAddress = "resolvedAddress"
        val receiveAddress = EthAddress(resolvedAddress)
        every {
            ethAsset.parseAddress(resolvedAddress, emojisDomain, true)
        } returns Maybe.just(receiveAddress)

        every {
            addressResolver.resolveAssetAddress(emojisDomain, CryptoCurrency.ETHER.networkTicker)
        } returns Single.just(resolvedAddress)

        subject.parse(emojisDomain, CryptoCurrency.ETHER).test()
            .assertValue { result ->
                result == receiveAddress
            }

        verify { addressResolver.resolveAssetAddress(emojisDomain, CryptoCurrency.ETHER.networkTicker) }
        verify { coincore[CryptoCurrency.ETHER] }
        verify { ethAsset.parseAddress(resolvedAddress, emojisDomain, true) }
    }

    @Test
    fun `fail to resolve mix of emojis and alphanumeric`() {
        val mix = "abc👾️💻12"

        every { ethAsset.parseAddress(mix) } returns Maybe.empty()

        every {
            addressResolver.resolveAssetAddress(mix, CryptoCurrency.ETHER.networkTicker)
        } returns Single.error(DomainAddressNotFound())

        subject.parse(mix, CryptoCurrency.ETHER).test()
            .assertError { error ->
                error is TxValidationFailure
            }
        verify { addressResolver.resolveAssetAddress(mix, CryptoCurrency.ETHER.networkTicker) }
    }

    @Test
    fun `fail to resolve domain`() {
        val domain = "invalid.domain"

        every {
            addressResolver.resolveAssetAddress(domain, CryptoCurrency.ETHER.networkTicker)
        } returns Single.just("")

        subject.parse(domain, CryptoCurrency.ETHER).test()
            .assertError { error ->
                // TODO: this is really bad, we need to get rid of using Exceptions as GOTO and
                // clean up error handling around the AddressFactory
                error is CompositeException && error.exceptions.first() is TxValidationFailure
            }
        verify { addressResolver.resolveAssetAddress(domain, CryptoCurrency.ETHER.networkTicker) }
    }
}
