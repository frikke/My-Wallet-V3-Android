package com.blockchain.componentlib.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.R
import com.blockchain.componentlib.icons.Bell
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.system.DialogueCard
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.preferences.RuntimePermissionsPrefs
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.koin.androidx.compose.get

@Immutable
sealed class RuntimePermission(
    val name: String,
    val coolOffMillis: Long
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    object Notification : RuntimePermission(
        name = Manifest.permission.POST_NOTIFICATIONS,
        coolOffMillis = TimeUnit.DAYS.toMillis(30)
    )
}

enum class PermissionRequestDeniedReason {
    User, System, CoolOff
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RuntimePermission(
    permission: RuntimePermission,
    grantedContent: @Composable () -> Unit = {},
    deniedContent: @Composable (reason: PermissionRequestDeniedReason) -> Unit = {},
) {
    val runtimePermissionsPrefs: RuntimePermissionsPrefs = get()

    var showDialog by remember(permission.name) { mutableStateOf(false) }
    var permissionRequested by remember(permission.name) { mutableStateOf(false) }

    var userDeniedPermission by remember(permission.name) { mutableStateOf(false) }
    val permissionState = rememberPermissionState(permission.name) { isAccepted ->
        userDeniedPermission = !isAccepted
    }

    when {
        permissionState.status.isGranted -> {
            grantedContent()
        }

        userDeniedPermission -> {
            runtimePermissionsPrefs.update(permission)
            deniedContent(PermissionRequestDeniedReason.User)
        }

        !runtimePermissionsPrefs.canAskAgain(permission) -> {
            deniedContent(PermissionRequestDeniedReason.CoolOff)
        }

        !permissionState.status.shouldShowRationale -> {
            // if permission is not granted + !shouldShowRationale -> means we cannot ask, should go to settings
            deniedContent(PermissionRequestDeniedReason.System)
        }

        else -> {
            // is not granted, and showRationale is true -> we can ask for it
            showDialog = true
        }
    }

    if (showDialog && !permissionRequested) {
        DialogueCard(
            icon = Icons.Filled.Bell.withTint(AppColors.primary),
            title = stringResource(com.blockchain.stringResources.R.string.permission_notifications_title),
            body = stringResource(com.blockchain.stringResources.R.string.permission_notifications_description),
            firstButton = DialogueButton(
                text = stringResource(com.blockchain.stringResources.R.string.common_dont_allow),
                onClick = {
                    runtimePermissionsPrefs.update(permission)
                    userDeniedPermission = true
                    showDialog = false
                }
            ),
            secondButton = DialogueButton(
                text = stringResource(com.blockchain.stringResources.R.string.common_ok),
                onClick = {
                    permissionRequested = true
                    permissionState.launchPermissionRequest()
                    showDialog = false
                }
            ),
            onDismissRequest = {
                runtimePermissionsPrefs.update(permission)
                userDeniedPermission = true
                showDialog = false
            }
        )
    }
}

private fun RuntimePermissionsPrefs.canAskAgain(permission: RuntimePermission): Boolean {
    val now = Calendar.getInstance().timeInMillis

    return when (permission) {
        RuntimePermission.Notification -> now - notificationLastRequestMillis > permission.coolOffMillis
    }
}

private fun RuntimePermissionsPrefs.update(permission: RuntimePermission) {
    val now = Calendar.getInstance().timeInMillis

    when (permission) {
        RuntimePermission.Notification -> notificationLastRequestMillis = now
    }
}
