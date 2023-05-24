package piuk.blockchain.android.util

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.blockchain.analytics.TraitsService
import com.blockchain.walletmode.WalletMode
import org.koin.dsl.bind
import org.koin.dsl.module

class AccessibilityTraitsRepository(private val context: Context) : TraitsService {
    override suspend fun traits(): Map<String, String> {
        return mapOf(
            "accessibility_enabled" to isAccessibilityEnabled().toString()
        )
    }

    private fun isAccessibilityEnabled(): Boolean =
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.let {
            it.isTouchExplorationEnabled
        } ?: false
}

val accessibilityModule = module {
    factory {
        AccessibilityTraitsRepository(context = get())
    }.bind(TraitsService::class)
}
