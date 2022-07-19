package com.blockchain.coincore

import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.testutil.CoincoreTestBase.Companion.TEST_ASSET
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.mock
import exchange.ExchangeLinking
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

private val nonCustodialAccounts = listOf<CryptoNonCustodialAccount>(
    mock {
        on { label }.thenReturn("Label1")
    },
    mock {
        on { label }.thenReturn("Label2")
    }
)

internal class CryptoCustodialTestAsset : CryptoAssetBase() {

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> {
        return Single.just(nonCustodialAccounts)
    }

    override val currency: AssetInfo
        get() = TEST_ASSET

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> =
        Maybe.empty()
}

class CryptoAssetBaseTest : KoinTest {
    private val walletModeService = mock<WalletModeService>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            mockedModule,
            module {
                factory {
                    walletModeService
                }
                factory {
                    mock<DefaultLabels> {
                        on { getAssetMasterWalletLabel(TEST_ASSET) }.thenReturn("TEST_ASSET")
                        on { getDefaultCustodialWalletLabel() }.thenReturn("Custodial label ")
                        on { getDefaultInterestWalletLabel() }.thenReturn("Interest label ")
                    }
                }
                factory {
                    mock<CustodialWalletManager> {
                        on { getInterestAvailabilityForAsset(TEST_ASSET) }.thenReturn(Single.just(true))
                    }
                }
            }
        )
    }
}

private val mockedModule = module {
    factory {
        mock<ExchangeRatesDataManager>()
    }
    factory {
        mock<InterestBalanceDataManager>()
    }

    factory {
        mock<TradingBalanceDataManager>()
    }

    factory {
        mock<ExchangeLinking>()
    }

    factory {
        mock<RemoteLogger>()
    }

    factory {
        mock<UserIdentity>()
    }
}
