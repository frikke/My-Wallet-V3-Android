package piuk.blockchain.android.fileutils.data

import com.google.firebase.storage.FirebaseStorage
import org.koin.dsl.module
import piuk.blockchain.android.fileutils.data.repository.FileRepository
import piuk.blockchain.android.fileutils.domain.service.FileService

val fileUtilsDataModule = module {

    single {
        FirebaseStorage.getInstance()
    }

    single<FileService> {
        FileRepository(
            firebaseStorage = get()
        )
    }
}
