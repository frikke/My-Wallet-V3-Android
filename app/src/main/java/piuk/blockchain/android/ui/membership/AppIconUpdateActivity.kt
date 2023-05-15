package piuk.blockchain.android.ui.membership

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.ui.launcher.LauncherActivity

// This class needs to live outside of the membership module so it can access the [LauncherActivity]
class AppIconUpdateActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            ComposeView(this).apply {
                setContent {
                    AppTheme {
                        Column {
                            PrimaryButton(text = "Launcher Original", onClick = {
                                with(packageManager) {
                                    enableAlias(LauncherActivity::class.java)
                                    disableAlias(LauncherOneAlias::class.java)
                                    disableAlias(LauncherTwoAlias::class.java)
                                }
                            })
                            PrimaryButton(text = "Launcher one", onClick = {
                                with(packageManager) {
                                    enableAlias(LauncherOneAlias::class.java)
                                    disableAlias(LauncherTwoAlias::class.java)
                                    disableAlias(LauncherActivity::class.java)
                                }
                            })
                            PrimaryButton(text = "Launcher two", onClick = {
                                with(packageManager) {
                                    enableAlias(LauncherTwoAlias::class.java)
                                    disableAlias(LauncherOneAlias::class.java)
                                    disableAlias(LauncherActivity::class.java)
                                }
                            })
                        }
                    }
                }
            }
        )
    }

    private fun PackageManager.enableAlias(aliasName: Class<*>) =
        setComponentEnabledSetting(
            ComponentName(
                this@AppIconUpdateActivity,
                aliasName
            ),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

    private fun PackageManager.disableAlias(aliasName: Class<*>) =
        setComponentEnabledSetting(
            ComponentName(
                this@AppIconUpdateActivity,
                aliasName
            ),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

    companion object {
        fun newIntent(context: Context) =
            Intent(context, AppIconUpdateActivity::class.java)
    }
}

class LauncherOneAlias
class LauncherTwoAlias
