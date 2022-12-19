package com.blockchain.componentlib.system

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager

@Composable
fun EmbeddedFragment(
    fragment: Fragment,
    fragmentManager: FragmentManager,
    modifier: Modifier = Modifier,
    tag: String
) {
    val viewId by rememberSaveable { mutableStateOf(View.generateViewId()) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            fragmentManager.findFragmentById(viewId)?.view
                ?.also { (it.parent as? ViewGroup)?.removeView(it) }
                ?: FragmentContainerView(context)
                    .apply { id = viewId }
                    .also {
                        fragmentManager.beginTransaction().replace(viewId, fragment, tag).commit()
                    }
        },
        update = {
        }
    )
}
