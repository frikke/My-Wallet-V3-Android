package com.blockchain.commonarch.presentation.mvi_v2

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Internal model state.
 */
interface ModelState

/**
 * Emitted view state for view consumption
 */
interface ViewState

/**
 * Intent representing an action from the user
 */
interface Intent {
    fun isValidFor(modelState: ModelState): Boolean = true
}

/**
 * NavigationEvent represents a navigation event that is triggered by a Model
 */
interface NavigationEvent

/**
 * This interface should be implemented by the class that handles the navigation
 */
interface NavigationRouter<T : NavigationEvent> {
    fun route(navigationEvent: T)
}

sealed interface ModelConfigArgs {
    interface ParcelableArgs : Parcelable, ModelConfigArgs
    object NoArgs : ModelConfigArgs
}

abstract class MviViewModel<TIntent : Intent,
    TViewState : ViewState,
    TModelState : ModelState,
    NavEvent : NavigationEvent,
    TArgs : ModelConfigArgs>(
    val initialState: TModelState
) : ViewModel() {

    /**
     *  [NavigationEvent] flow, subscribers only get notified on new emissions,
     *  not on initial subscription, no initial state
     */
    private val _navigationEventFlow = MutableSharedFlow<NavEvent>()
    val navigationEventFlow: Flow<NavEvent>
        get() = _navigationEventFlow

    /**
     * Internal model state. In this property, we persist whatever state needs to be persisted in the
     * model but UI doesn't care for.
     */

    private var _modelState: TModelState = initialState

    protected val modelState: TModelState
        get() = _modelState

    /**
     * Method gets called when the Model gets binded to the UI.
     * Based on the arguments that the UI provides we might need
     * to update the [modelState] and [viewState]. Normally, these args would come
     * from the arguments of a fragment or from some initial state passed to a composable.
     * @param args Arguments provided when UI gets created
     */
    abstract fun viewCreated(args: TArgs)

    /* private val mutex = Mutex() */

    /**
     * Called by the Viewmodel whenever states [modelState] and [viewState] need to get updated.
     * @param stateUpdate a lambda that generates a new [modelState]
     */
    protected fun updateState(stateUpdate: (state: TModelState) -> TModelState) {
        viewModelScope.launch {
            /*    mutex.withLock {*/
            _modelState = stateUpdate(_modelState)
            _viewState.value = reduce(_modelState)
        }
        /* } */
    }

    /**
     * Called by the viewmodel to trigger a navigation event
     */
    protected fun navigate(navigationEvent: NavEvent) {
        viewModelScope.launch {
            _navigationEventFlow.emit(navigationEvent)
        }
    }

    /**
     * [_viewState] flow always has a value
     */
    private val _viewState by lazy {
        MutableStateFlow(reduce(initialState))
    }

    val viewState: Flow<TViewState>
        get() = _viewState

    /**
     * Method that should be override in every Model created. In this method, base on the latest internal
     * model state, we create a new immutable [viewState]
     * @param state model latest internal state
     */
    protected abstract fun reduce(state: TModelState): TViewState

    /**
     * Called by the UI to feed the model with Intents
     * @param intent the UI originated intent
     */
    fun onIntent(intent: TIntent) {
        viewModelScope.launch {
            if (intent.isValidFor(modelState)) {
                Timber.d("Model: Process Intent ****> : ${intent.javaClass.simpleName}")
                handleIntent(modelState, intent)
            } else {
                Timber.d("Model: Dropping Intent ****> : ${intent.javaClass.simpleName}")
            }
        }
    }

    /**
     * Method that should be override in every Model created. In this method we
     * handle the processed intent based on the internal modelState and we decide how to [updateState] or [navigate].
     * @param intent The processed intent
     * @param modelState The latest model internal state
     */
    abstract suspend fun handleIntent(modelState: TModelState, intent: TIntent)
}
