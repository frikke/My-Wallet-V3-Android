package piuk.blockchain.android.ui.kyc.autocomplete

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.kyc.profile.models.AddressDetailsModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class KycAutocompleteAddressModel(
    initialState: KycAutocompleteAddressState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: KycAutocompleteAddressInteractor
) : MviModel<KycAutocompleteAddressState, KycAutocompleteAddressIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    sealed class Action {
        data class NavigateToAddressFragment(val addressDetails: AddressDetailsModel) : Action()
        data class UpdateSearchList(val addresses: List<KycAddressResult>) : Action()
    }

    private val _actions = MutableLiveData<Event<Action>>()
    val actions: LiveData<Event<Action>> = _actions

    override fun performAction(
        previousState: KycAutocompleteAddressState,
        intent: KycAutocompleteAddressIntents
    ): Disposable? {
        when (intent) {
            is KycAutocompleteAddressIntents.SelectAddress -> {
                interactor.getAddressDetails(intent.selectedAddress.placeId)
                    .subscribeBy(
                        onSuccess = {
                            _actions.value = Event(Action.NavigateToAddressFragment(it))
                        },
                        onError = { }
                    )
            }
            is KycAutocompleteAddressIntents.UpdateSearchText -> {
                return interactor.searchForAddresses(intent.addressSearchText, intent.countryCode)
                    .subscribeBy(
                        onSuccess = {
                            _actions.value = Event(Action.UpdateSearchList(it))
                        },
                        onError = { }
                    )
            }
        }

        return null
    }
}

open class Event<out T : Any>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     */
    fun getIfNotHandled(): T? {
        if (hasBeenHandled) {
            return null
        }

        hasBeenHandled = true
        return content
    }
}

/**
 * Similar to [LiveData.observe], but only calls [onChanged] if the value
 * emitted is non-null.
 */
inline fun <T : Any> LiveData<T>.observeNonNull(
    owner: LifecycleOwner,
    crossinline onChanged: (T) -> Unit
) {
    observe(
        owner,
        { t ->
            if (t != null) {
                onChanged(t)
            }
        }
    )
}

inline fun <T : Any> LiveData<Event<T>>.handleEvents(
    owner: LifecycleOwner,
    crossinline onEvent: (T) -> Unit
) {
    observeNonNull(owner) {
        val content = it.getIfNotHandled()
        if (content != null) {
            onEvent(content)
        }
    }
}