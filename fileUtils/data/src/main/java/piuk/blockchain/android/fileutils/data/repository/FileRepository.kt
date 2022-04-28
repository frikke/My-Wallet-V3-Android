package piuk.blockchain.android.fileutils.data.repository

import com.blockchain.outcome.Outcome
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import piuk.blockchain.android.fileutils.domain.service.FileService

class FileRepository(
    private val firebaseStorage: FirebaseStorage
) : FileService {

    override suspend fun downloadFirebaseFile(absolutePath: String, fileGsLink: String): Outcome<Throwable, File> {
        return firebaseStorage
            .getReferenceFromUrl(fileGsLink)
            .run {
                suspendCoroutine { continuation ->
                    with(File(absolutePath)) {
                        if (exists()) {
                            continuation.resume(Outcome.Success(this))
                        } else {
                            getFile(this).addOnSuccessListener {
                                continuation.resume(Outcome.Success(this))
                            }.addOnFailureListener {
                                continuation.resume(Outcome.Failure(Throwable()))
                            }
                        }
                    }
                }
            }
    }
}
