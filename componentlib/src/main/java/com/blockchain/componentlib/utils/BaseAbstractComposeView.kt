package com.blockchain.componentlib.utils

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

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
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private lateinit var fakeSavedStateRegistryOwner: SavedStateRegistryOwner
    private lateinit var fakeViewModelStoreOwner: ViewModelStoreOwner

    init {
        if (isInEditMode) {
            // Taken from androidx.compose.ui.tooling.ComposeViewAdapter.kt
            fakeSavedStateRegistryOwner = object : SavedStateRegistryOwner {
                override val lifecycle = LifecycleRegistry.createUnsafe(this)
                private val controller = SavedStateRegistryController.create(this).apply {
                    performRestore(Bundle())
                }

                init {
                    lifecycle.currentState = Lifecycle.State.RESUMED
                }

                override val savedStateRegistry: SavedStateRegistry
                    get() = controller.savedStateRegistry
            }
            fakeViewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }

            setViewTreeLifecycleOwner(fakeSavedStateRegistryOwner)
            setViewTreeSavedStateRegistryOwner(fakeSavedStateRegistryOwner)
            setViewTreeViewModelStoreOwner(fakeViewModelStoreOwner)
        }
    }

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

    override fun onAttachedToWindow() {
        if (isInEditMode) {
            ownerView = getLastParent()
            setViewTreeLifecycleOwner(fakeSavedStateRegistryOwner)
            setViewTreeSavedStateRegistryOwner(fakeSavedStateRegistryOwner)
            setViewTreeViewModelStoreOwner(fakeViewModelStoreOwner)
        }
        super.onAttachedToWindow()
    }
}
