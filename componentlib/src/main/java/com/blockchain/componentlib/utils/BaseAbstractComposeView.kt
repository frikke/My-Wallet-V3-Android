package com.blockchain.componentlib.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.blockchain.componentlib.R

/**
 * The issue with Compose XML preview is that Compose needs a lifecycleOwner and a savedStateRegistry to work, both of these
 * are added by AppCompatActivity at runtime and accessed by Compose via a parentView tag.
 * Because the preview editor in Android Studio provides it's own windows and doesn't provide these classes we have to provide
 * them ourselves to the parentView
 */
abstract class BaseAbstractComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var savedStateRegistry: SavedStateRegistryController

    init {
        if (isInEditMode) {
            lifecycleRegistry = LifecycleRegistry(this as LifecycleOwner)
            savedStateRegistry = SavedStateRegistryController.create(this as SavedStateRegistryOwner)
        }
    }

    @Deprecated(message = "Do not use, these are needed for Compose XML preview", level = DeprecationLevel.ERROR)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    @Deprecated(message = "Do not use, these are needed for Compose XML preview", level = DeprecationLevel.ERROR)
    override fun getSavedStateRegistry(): SavedStateRegistry = savedStateRegistry.savedStateRegistry

    private var ownerView: View? = null
    private fun getLastParent(): View {
        var lastParent: View = this
        while (true) {
            val parent = lastParent.parent
            if (parent is View) lastParent = parent
            else break
        }
        return lastParent
    }

    private fun addViewTreeLifecycleOwner() {
        ownerView?.setTag(R.id.view_tree_lifecycle_owner, this as LifecycleOwner)
    }

    private fun addSavedStateRegistryOwner() {
        ownerView?.setTag(R.id.view_tree_saved_state_registry_owner, this as SavedStateRegistryOwner)
    }

    private fun removeViewTreeLifecycleOwner() {
        ownerView?.setTag(R.id.view_tree_lifecycle_owner, null)
    }

    private fun removeSavedStateRegistryOwner() {
        ownerView?.setTag(R.id.view_tree_saved_state_registry_owner, null)
    }

    override fun onAttachedToWindow() {
        if (isInEditMode) {
            ownerView = getLastParent()
            addViewTreeLifecycleOwner()
            addSavedStateRegistryOwner()
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
            savedStateRegistry.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        if (isInEditMode) {
            removeViewTreeLifecycleOwner()
            removeSavedStateRegistryOwner()
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        super.onDetachedFromWindow()
    }
}
