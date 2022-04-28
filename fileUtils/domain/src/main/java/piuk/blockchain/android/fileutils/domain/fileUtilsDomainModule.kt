package piuk.blockchain.android.fileutils.domain

import org.koin.dsl.module
import piuk.blockchain.android.fileutils.domain.usecase.DownloadFileUseCase

val fileUtilsDomainModule = module {
    single {
        DownloadFileUseCase(service = get())
    }
}
