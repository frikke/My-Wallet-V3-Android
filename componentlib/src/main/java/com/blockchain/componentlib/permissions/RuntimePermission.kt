package com.blockchain.componentlib.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Bell
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.system.DialogueCard
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.preferences.RuntimePermissionsPrefs
import com.blockchain.stringResources.R
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
    val coolOffMillis: Long,

    val icon: ImageResource.Local,
    @StringRes val title: Int,
    @StringRes val description: Int
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    object Notification : RuntimePermission(
        name = Manifest.permission.POST_NOTIFICATIONS,
        coolOffMillis = TimeUnit.DAYS.toMillis(30),
        icon = Icons.Filled.Bell,
        title = R.string.permission_notifications_title,
        description = R.string.permission_notifications_description
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

    // if user denies the permission with "do not ask again", we flag it locally, because we can only base
    // our checks on isGranted and shouldShowRationale

    // with user rejecting every time:
    // first run  -> isGranted=false, shouldShowRationale=false
    // second run -> isGranted=false, shouldShowRationale=true
    // third run  -> isGranted=false, shouldShowRationale=false
    // -> we need a way to flag if user requested before or not
    // and use that in combination with shouldShowRationale
    // to know if it's the first time or user rejected for good
    when {
        permissionState.status.isGranted -> {
            grantedContent()
        }

        userDeniedPermission -> {
            if (!permissionState.status.shouldShowRationale) {
                // we cannot request again - unless user enables from settings
                // see below conditions
                runtimePermissionsPrefs.updateDoNotAskAgain(
                    permission = permission,
                    doNotAskAgain = true
                )
            }
            runtimePermissionsPrefs.update(permission)
            deniedContent(PermissionRequestDeniedReason.User)
        }

        !runtimePermissionsPrefs.canAskAgain(permission) -> {
            deniedContent(PermissionRequestDeniedReason.CoolOff)
        }

        runtimePermissionsPrefs.isDoNotAskAgainSet(permission) && !permissionState.status.shouldShowRationale -> {
            // if user set donoasagain, and shouldShowRationale is true
            // means user enabled it from settings
            // (and off again at some point since isGranted is false at this point)
            deniedContent(PermissionRequestDeniedReason.System)
        }

        else -> {
            runtimePermissionsPrefs.updateDoNotAskAgain(
                permission = permission,
                doNotAskAgain = false
            )
            showDialog = true
        }
    }

    if (showDialog && !permissionRequested) {
        DialogueCard(
            icon = permission.icon.withTint(AppColors.primary),
            title = stringResource(permission.title),
            body = stringResource(permission.description),
            firstButton = DialogueButton(
                text = stringResource(R.string.common_dont_allow),
                onClick = {
                    runtimePermissionsPrefs.update(permission)
                    userDeniedPermission = true
                    showDialog = false
                }
            ),
            secondButton = DialogueButton(
                text = stringResource(R.string.common_ok),
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

private fun RuntimePermissionsPrefs.updateDoNotAskAgain(
    permission: RuntimePermission,
    doNotAskAgain: Boolean
) {
    when (permission) {
        RuntimePermission.Notification -> notificationDoNotAskAgain = doNotAskAgain
    }
}

private fun RuntimePermissionsPrefs.isDoNotAskAgainSet(permission: RuntimePermission): Boolean {
    return when (permission) {
        RuntimePermission.Notification -> notificationDoNotAskAgain
    }
}
