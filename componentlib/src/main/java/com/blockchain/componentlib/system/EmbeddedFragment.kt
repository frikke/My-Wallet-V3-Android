package com.blockchain.componentlib.system

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

@Composable
fun EmbeddedFragment(
    fragment: Fragment,
    fragmentManager: FragmentManager,
    modifier: Modifier = Modifier,
    tag: String
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                id = ViewCompat.generateViewId()
            }
        },
        update = {
            val fragmentAlreadyAdded = fragmentManager.findFragmentByTag(tag) != null

            if (!fragmentAlreadyAdded) {
                fragmentManager.beginTransaction().replace(it.id, fragment, tag).commit()
            }
        }
    )
}
