package piuk.blockchain.android.fileutils.domain.usecase

import com.blockchain.outcome.Outcome
import java.io.File
import piuk.blockchain.android.fileutils.domain.service.FileService

class DownloadFileUseCase(private val service: FileService) {
    suspend operator fun invoke(absolutePath: String, fileGsLink: String): Outcome<Throwable, File> =
        service.downloadFirebaseFile(absolutePath = absolutePath, fileGsLink = fileGsLink)
}
