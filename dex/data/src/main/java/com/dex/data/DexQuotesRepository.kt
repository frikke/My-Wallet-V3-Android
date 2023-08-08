package com.dex.data

import com.blockchain.DefiWalletReceiveAddressService
import com.blockchain.api.NabuApiException
import com.blockchain.api.dex.DexQuotesApiService
import com.blockchain.api.dex.FromCurrency
import com.blockchain.api.dex.ToCurrency
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.utils.asFlow
import com.blockchain.utils.toUtcIso8601
import com.dex.domain.DexAccount
import com.dex.domain.DexBalanceService
import com.dex.domain.DexQuote
import com.dex.domain.DexQuoteParams
import com.dex.domain.DexQuotesService
import com.dex.domain.DexTxError
import com.dex.domain.ExchangeAmount
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.web3j.utils.Convert

class DexQuotesRepository(
    private val dexQuotesApiService: DexQuotesApiService,
    private val coincore: Coincore,
    private val defiWalletReceiveAddressService: DefiWalletReceiveAddressService,
    private val assetCatalogue: AssetCatalogue
) : DexQuotesService, DexBalanceService {
    override suspend fun quote(
        dexQuoteParams: DexQuoteParams
    ): Outcome<DexTxError, DexQuote> {
        println(
            "--- Fetching quote with input" +
                " ${dexQuoteParams.inputAmount.amount.toStringWithSymbol()} ---" +
                " @${Date().toUtcIso8601()} "
        )
        val address = defiWalletReceiveAddressService.receiveAddress(dexQuoteParams.sourceAccount.currency)

        val nativeCurrency = dexQuoteParams.sourceAccount.currency.coinNetwork?.nativeAssetTicker?.let {
            assetCatalogue.fromNetworkTicker(it)
        } ?: return Outcome.Failure(
            DexTxError.FatalTxError(IllegalStateException("Unknown native asset ticker"))
        )

        return address.flatMap {
            dexQuotesApiService.quote(
                fromCurrency = FromCurrency(
                    chainId = dexQuoteParams.sourceAccount.currency.chainId,
                    symbol = dexQuoteParams.sourceAccount.currency.networkTicker,
                    address = dexQuoteParams.sourceAccount.currency.contractAddress ?: PLACEHOLDER_CONTRACT_ADDRESS,
                    amount = dexQuoteParams.inputAmount.takeIf { exchange ->
                        exchange is ExchangeAmount.SellAmount
                    }?.amount?.toBigInteger()?.toString()
                ),
                toCurrency = ToCurrency(
                    chainId = dexQuoteParams.destinationAccount.currency.chainId,
                    symbol = dexQuoteParams.destinationAccount.currency.networkTicker,
                    address = dexQuoteParams.destinationAccount.currency.contractAddress
                        ?: PLACEHOLDER_CONTRACT_ADDRESS,
                    amount = dexQuoteParams.inputAmount.takeIf { amount -> amount is ExchangeAmount.BuyAmount }
                        ?.amount?.toBigInteger()
                        ?.toString()
                ),
                slippage = dexQuoteParams.slippage,
                address = it.address,
                skipValidation = dexQuoteParams.sourceHasBeenAllowed.not()
            ).map { resp ->
                DexQuote.ExchangeQuote(
                    sellAmount = ExchangeAmount.SellAmount(
                        amount = Money.fromMinor(
                            currency = dexQuoteParams.sourceAccount.currency,
                            value = resp.quote.sellAmount.amount.toBigInteger()
                        ),
                        minAmount = resp.quote.sellAmount.minAmount?.toBigInteger()?.let { amount ->
                            Money.fromMinor(
                                currency = dexQuoteParams.sourceAccount.currency,
                                value = amount
                            )
                        }
                    ),
                    buyAmount = ExchangeAmount.BuyAmount(
                        amount = Money.fromMinor(
                            currency = dexQuoteParams.destinationAccount.currency,
                            value = resp.quote.buyAmount.amount.toBigInteger()
                        ),
                        minAmount = resp.quote.buyAmount.minAmount?.toBigInteger()?.let { amount ->
                            Money.fromMinor(
                                currency = dexQuoteParams.destinationAccount.currency,
                                value = amount
                            )
                        }
                    ),
                    networkFees = calculateEstimatedQuoteFee(
                        nativeCurrency,
                        resp.transaction.gasLimit.toBigInteger(),
                        resp.transaction.gasPrice.toBigInteger()
                    ),
                    price = Money.fromMajor(
                        dexQuoteParams.destinationAccount.currency,
                        BigDecimal(resp.quote.price)
                    ),
                    blockchainFees = Money.fromMinor(
                        dexQuoteParams.destinationAccount.currency,
                        resp.quote.buyTokenFee.takeIf { it.isNotEmpty() }?.toBigInteger() ?: BigInteger.ZERO
                    ),
                    data = resp.transaction.data,
                    quoteTtl = resp.quoteTtl,
                    gasLimit = resp.transaction.gasLimit,
                    value = resp.transaction.value,
                    gasPrice = resp.transaction.gasPrice,
                    destinationContractAddress = resp.transaction.to
                )
            }
        }.mapError {
            DexTxError.QuoteError(
                title = (it as? NabuApiException)?.let { nabuException ->
                    nabuException.getServerSideErrorInfo()?.title.orEmpty()
                },
                message = (it as? NabuApiException)?.let { nabuException ->
                    nabuException.getServerSideErrorInfo()?.description.orEmpty()
                },
                id = (it as? NabuApiException)?.let { nabuException ->
                    nabuException.getServerSideErrorInfo()?.id.orEmpty()
                }
            )
        }
    }

    private fun calculateEstimatedQuoteFee(
        nativeCurrency: Currency,
        gasLimit: BigInteger,
        gasPriceWei: BigInteger
    ): Money {
        val gasPriceInGwei = Convert.fromWei(
            gasPriceWei.toBigDecimal(),
            Convert.Unit.GWEI
        )

        val feeInWei = Convert.toWei(
            gasPriceInGwei.multiply(gasLimit.toBigDecimal()),
            Convert.Unit.GWEI
        ).toBigInteger()

        return Money.fromMinor(
            nativeCurrency,
            feeInWei
        )
    }

    override suspend fun networkBalance(account: DexAccount): Money {
        val nativeCurrency = account.currency.coinNetwork?.nativeAssetTicker?.let {
            assetCatalogue.fromNetworkTicker(it)
        } ?: throw IllegalArgumentException("Unsupported currency")
        val coincoreAccount = coincore[nativeCurrency].defaultAccount(AssetFilter.NonCustodial).asFlow().first()
        return coincoreAccount.balance().map { it.total }.first()
    }
}

private const val PLACEHOLDER_CONTRACT_ADDRESS = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
