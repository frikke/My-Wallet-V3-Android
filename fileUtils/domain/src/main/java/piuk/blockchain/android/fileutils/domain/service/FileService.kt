package piuk.blockchain.android.fileutils.domain.service

import com.blockchain.outcome.Outcome
import java.io.File

interface FileService {
    /**
     * @param absolutePath Absolute file path, use
     * ```
     * Context.getFilePath(fileName, extension)
     * ```
     * @param fileGsLink The Storage Location generated when uploading a file to Firebase Storage
     */
    suspend fun downloadFirebaseFile(absolutePath: String, fileGsLink: String): Outcome<Throwable, File>
}