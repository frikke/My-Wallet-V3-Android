package piuk.blockchain.android.ui.ssl

import com.blockchain.core.utils.SSLVerifyUtil
import piuk.blockchain.android.ui.base.BasePresenter

class SSLVerifyPresenter(
    private val sslVerifyUtil: SSLVerifyUtil
) : BasePresenter<SSLVerifyView>() {

    override fun onViewReady() {
        view.showWarningPrompt()
    }

    fun validateSSL() {
        sslVerifyUtil.validateSSL()
    }
}
