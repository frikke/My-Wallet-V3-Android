package piuk.blockchain.android.exchange

import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.NabuUser
import exchange.ExchangeLinking
import exchange.ExchangeLinkingState
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

class ExchangeLinkingImpl(
    private val nabu: NabuDataManager,
    private val nabuToken: NabuToken,
) : ExchangeLinking {

    private val disposables = CompositeDisposable()

    private fun publish(state: ExchangeLinkingState) {
        internalState.onNext(state)
    }

    private val internalState = BehaviorSubject.create<ExchangeLinkingState>()
    private val refreshEvents = PublishSubject.create<Unit>()

    override val state: Observable<ExchangeLinkingState> = internalState.doOnSubscribe { onNewSubscription() }

    init {
        disposables += refreshEvents.switchMapSingle {
            nabuToken.fetchNabuToken().flatMap { token -> nabu.getUser(token) }
        }.subscribeOn(Schedulers.computation())
            .map { it.toLinkingState() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { publish(it) },
                onComplete = { },
                onError = { }
            )
    }

    private fun NabuUser.toLinkingState(): ExchangeLinkingState {
        return ExchangeLinkingState(
            isLinked = exchangeEnabled,
            emailVerified = emailVerified,
            email = email
        )
    }

    private fun onNewSubscription() {
        // Re-fetch the user state
        refreshEvents.onNext(Unit)
    }

    // Temporary helper method, for all the MVP clients:
    override fun isExchangeLinked(): Single<Boolean> =
        state.flatMapSingle { state -> Single.just(state.isLinked) }
            .firstOrError()
            .onErrorResumeNext { Single.just(false) }
}
