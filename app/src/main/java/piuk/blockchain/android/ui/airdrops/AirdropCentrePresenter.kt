package piuk.blockchain.android.ui.airdrops

import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.AirdropStatus
import com.blockchain.nabu.models.responses.nabu.AirdropStatusList
import com.blockchain.nabu.models.responses.nabu.CampaignState
import com.blockchain.nabu.models.responses.nabu.CampaignTransactionState
import com.blockchain.nabu.models.responses.nabu.UserCampaignState
import com.blockchain.nabu.models.responses.nabu.sunriverCampaignName
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.lang.IllegalStateException
import java.util.Date
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import timber.log.Timber

interface AirdropCentreView : MvpView {
    fun renderList(statusList: List<Airdrop>)
    fun renderListUnavailable()
}

class AirdropCentrePresenter(
    private val nabu: NabuDataManager,
    private val assetCatalogue: AssetCatalogue,
    private val remoteLogger: RemoteLogger
) : MvpPresenter<AirdropCentreView>() {

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = false

    override fun onViewCreated() {}

    override fun onViewAttached() {
        fetchAirdropStatus()
    }

    override fun onViewDetached() {}

    private fun fetchAirdropStatus() {
        compositeDisposable += nabu.getAirdropCampaignStatus()
            .map { list -> remapStateList(list) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { renderUi(it) },
                onError = {
                    remoteLogger.logException(it)
                    view?.renderListUnavailable()
                }
            )
    }

    private fun remapStateList(statusList: AirdropStatusList): List<Airdrop> =
        statusList.airdropList.mapNotNull { transformAirdropStatus(it) }.toList()

    private fun transformAirdropStatus(item: AirdropStatus): Airdrop? {
        val name = item.campaignName
        val asset = when (name) {
            sunriverCampaignName -> CryptoCurrency.XLM
            else -> return null
        }

        val status = parseState(item)
        val date = parseDate(item)
        val (amountFiat, amountCrypto) = parseAmount(item)

        return Airdrop(
            name,
            asset,
            status,
            amountFiat,
            amountCrypto,
            date
        )
    }

    private fun parseAmount(item: AirdropStatus): Pair<Money?, Money?> {
        val tx = item.txResponseList
            .firstOrNull {
                it.transactionState == CampaignTransactionState.FinishedWithdrawal
            }

        return tx?.let {
            Pair(
                Money.fromMinor(
                    assetCatalogue.fromNetworkTicker(tx.fiatCurrency) ?: throw IllegalStateException(
                        "Unknown crypto currency: ${tx.fiatCurrency}"
                    ),
                    tx.fiatValue.toBigInteger()
                ),
                CryptoValue.fromMinor(
                    parseAsset(tx.withdrawalCurrency),
                    tx.withdrawalQuantity.toBigDecimal()
                )
            )
        } ?: Pair(null, null)
    }

    private fun parseAsset(ticker: String): AssetInfo {
        val asset = assetCatalogue.assetInfoFromNetworkTicker(ticker)
        // STX is not, currently, a supported asset so we'll have to check that manually here for now.
        // When we get full-dynamic assets _and_ it's supported, we can take this out TODO
        return when {
            asset != null -> asset
            else -> throw IllegalStateException("Unknown crypto currency: $ticker")
        }
    }

    private fun parseState(item: AirdropStatus): AirdropState =
        if (item.campaignState == CampaignState.Ended) {
            when (item.userState) {
                UserCampaignState.RewardReceived -> AirdropState.RECEIVED
                else -> AirdropState.EXPIRED
            }
        } else {
            AirdropState.UNKNOWN
        }

    private fun parseDate(item: AirdropStatus): Date? {
        with(item) {
            return if (txResponseList.isEmpty()) {
                when (campaignName) {
                    sunriverCampaignName -> campaignEndDate
                    else -> null
                }
            } else {
                txResponseList.maxByOrNull {
                    it.withdrawalAt
                }?.withdrawalAt ?: throw IllegalStateException("Can't happen")
            }
        }
    }

    private fun renderUi(statusList: List<Airdrop>) {
        Timber.d("Got status!")
        view?.renderList(statusList)
    }
}

enum class AirdropState {
    UNKNOWN,
    REGISTERED,
    EXPIRED,
    PENDING,
    RECEIVED
}

data class Airdrop(
    val name: String,
    val asset: AssetInfo,
    val status: AirdropState,
    val amountFiat: Money?,
    val amountCrypto: Money?,
    val date: Date?
) {
    val isActive: Boolean = (status == AirdropState.PENDING || status == AirdropState.REGISTERED)
}
