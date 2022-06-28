package piuk.blockchain.android.ui.dashboard.announcements

import android.content.Context
import androidx.annotation.VisibleForTesting
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.campaign.CampaignType
import timber.log.Timber

interface AnnouncementHost {
    val disposables: CompositeDisposable
    val context: Context?

    fun showAnnouncementCard(card: AnnouncementCard)
    fun dismissAnnouncementCard()
    fun startSwap()

    // Actions
    fun startKyc(campaignType: CampaignType)

    fun startFundsBackup()
    fun startSetup2Fa()
    fun startVerifyEmail()
    fun startEnableFingerprintLogin()
    fun startTransferCrypto()

    fun startStxReceivedDetail()
    fun finishSimpleBuySignup()
    fun startSimpleBuy(asset: AssetInfo, paymentMethodId: String? = null)
    fun startInterestDashboard()
    fun startBuy()
    fun startSell()
    fun startSend()
    fun startRecurringBuyUpsell()

    fun showFiatFundsKyc()
    fun showBankLinking()
    fun openBrowserLink(url: String)

    fun joinNftWaitlist()
}

abstract class AnnouncementRule(private val dismissRecorder: DismissRecorder) {

    protected val dismissEntry by lazy { dismissRecorder[dismissKey] }

    abstract val dismissKey: String
    abstract val name: String

    abstract fun shouldShow(): Single<Boolean>
    abstract fun show(host: AnnouncementHost)
    fun isDismissed(): Boolean = dismissEntry.isDismissed
}

class AnnouncementList(
    private val mainScheduler: Scheduler,
    private val orderAdapter: AnnouncementConfigAdapter,
    private val availableAnnouncements: List<AnnouncementRule>,
    private val dismissRecorder: DismissRecorder
) {

    fun checkLatest(host: AnnouncementHost, disposables: CompositeDisposable) {
        host.dismissAnnouncementCard()

        disposables += showNextAnnouncement(host)
            .doOnSubscribe { Timber.d("SB Sync: Checking announcements...") }
            .subscribeBy(
                onComplete = { Timber.d("SB Sync: Announcements checked") },
                onError = Timber::e
            )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun buildAnnouncementList(order: List<String>): List<AnnouncementRule> {
        return order.mapNotNull { availableAnnouncements.find(it) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showNextAnnouncement(host: AnnouncementHost): Maybe<AnnouncementRule> =
        getNextAnnouncement()
            .observeOn(mainScheduler)
            .doOnSuccess { it.show(host) }

    private fun getNextAnnouncement(): Maybe<AnnouncementRule> =
        orderAdapter.announcementConfig
            .doOnSuccess { dismissRecorder.setPeriod(it.interval) }
            .map { buildAnnouncementList(it.order) }
            .flattenAsObservable { it }
            .concatMap { a ->
                Observable.defer {
                    a.shouldShow()
                        .filter { it }
                        .map { a }
                        .toObservable()
                }
            }
            .firstElement()

    internal fun dismissKeys(): List<String> = availableAnnouncements.map { it.dismissKey }

    private fun List<AnnouncementRule>.find(name: String): AnnouncementRule? =
        this.find { it.name == name }
}
