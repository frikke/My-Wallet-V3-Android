package piuk.blockchain.android.ui.auth.newlogin.data

import com.blockchain.domain.auth.SecureChannelService
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module
import piuk.blockchain.android.ui.auth.newlogin.data.repository.SecureChannelRepository

val secureChannelDataModule = module {
    scope(payloadScopeQualifier) {
        scoped<SecureChannelService> {
            SecureChannelRepository(
                secureChannelPrefs = get(),
                authPrefs = get(),
                payloadManager = get(),
                walletApi = get()
            )
        }
    }
}
