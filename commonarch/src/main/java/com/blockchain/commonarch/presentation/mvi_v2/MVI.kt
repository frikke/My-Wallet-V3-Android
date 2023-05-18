package com.blockchain.commonarch.presentation.mvi_v2

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Internal model state.
 */
interface ModelState

/**
 * Emitted view state for view consumption
 */
@Stable
interface ViewState

/**
 * Intent representing an action from the user
 */
interface Intent<TModelState : ModelState> {
    fun isValidFor(modelState: TModelState): Boolean = true
}

/**
 * NavigationEvent represents a navigation event that is triggered by a Model
 */
interface NavigationEvent
object EmptyNavEvent : NavigationEvent

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

abstract class MviViewModel<
    TIntent : Intent<TModelState>,
    TViewState : ViewState,
    TModelState : ModelState,
    NavEvent : NavigationEvent,
    TArgs : ModelConfigArgs
    >(
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

    private val _modelState = MutableStateFlow(initialState)

    protected val modelState: TModelState
        get() = _modelState.value

    /**
     * Method gets called when the Model gets binded to the UI.
     * Based on the arguments that the UI provides we might need
     * to update the [modelState] and [viewState]. Normally, these args would come
     * from the arguments of a fragment or from some initial state passed to a composable.
     * @param args Arguments provided when UI gets created
     */
    abstract fun viewCreated(args: TArgs)

    /**
     * Called by the Viewmodel whenever states [modelState] and [viewState] need to get updated.
     * @param stateUpdate a lambda that generates a new [modelState]
     */
    protected fun updateState(stateUpdate: TModelState.() -> TModelState) {
        _modelState.value = stateUpdate(modelState)
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
     * [viewState] flow always has a value
     */
    val viewState: StateFlow<TViewState>
        get() = _modelState.map {
            it.reduce()
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            initialState.reduce().also {
                Timber.e("Reducing initial state $it for ViewModel ${this.javaClass.simpleName}")
            }
        )

    /**
     * Method that should be override in every Model created. In this method, base on the latest internal
     * model state, we create a new immutable [viewState]
     * @param state model latest internal state
     */
    protected abstract fun TModelState.reduce(): TViewState

    /**
     * Called by the UI to feed the model with Intents
     * @param intent the UI originated intent
     */
    @SuppressLint("BinaryOperationInTimber")
    fun onIntent(intent: TIntent) {
        viewModelScope.launch {
            if (intent.isValidFor(modelState)) {
                Timber.d(
                    "Model ${this@MviViewModel.javaClass.simpleName}:" +
                        " Process Intent ****> : ${intent.javaClass.simpleName}"
                )
                handleIntent(modelState, intent)
            } else {
                Timber.d(
                    "Model ${this@MviViewModel.javaClass.simpleName}:" +
                        " Dropping Intent ****> : ${intent.javaClass.simpleName}"
                )
            }
        }
    }

    /**
     * Method that should be override in every Model created. In this method we
     * handle the processed intent based on the internal modelState and we decide how to [updateState] or [navigate].
     * @param intent The processed intent
     * @param modelState The latest model internal state
     */
    protected abstract suspend fun handleIntent(modelState: TModelState, intent: TIntent)
}
