package com.blockchain.analytics.data

import android.content.Context
import com.blockchain.analytics.AnalyticsLocalPersistence
import com.blockchain.api.services.NabuAnalyticsEvent
import com.blockchain.nabu.filesystem.QueueFile
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnalyticsFileLocalPersistence(context: Context) : AnalyticsLocalPersistence {

    private val json = Json {
        encodeDefaults = true
    }

    private val queueFile: QueueFile by lazy {
        val folder: File = context.getDir(DIR_NAME, Context.MODE_PRIVATE)
        createQueueFile(folder) ?: throw IllegalStateException("File system failed to initialised")
    }

    override fun size(): Single<Long> = Single.just(queueFile.size())

    override fun save(item: NabuAnalyticsEvent): Completable = Completable.fromAction {
        queueFile.add(json.encodeToString(item).toByteArray())
    }

    override fun removeOldestItems(n: Int): Completable = Completable.fromAction {
        if (!queueFile.isEmpty && n >= queueFile.size()) {
            queueFile.remove(n)
        }
    }

    override fun clear(): Completable = Completable.fromAction {
        queueFile.clear()
    }

    override fun getAllItems(): Single<List<NabuAnalyticsEvent>> {
        return Single.fromCallable {
            queueFile.read(queueFile.size()).map {
                json.decodeFromString(it)
            }
        }
    }

    override fun getOldestItems(n: Int): Single<List<NabuAnalyticsEvent>> {
        return Single.fromCallable {
            queueFile.read(n.toLong()).map {
                json.decodeFromString(it)
            }
        }
    }

    private fun createQueueFile(folder: File): QueueFile? {
        createDirectory(folder)
        val file = File(folder, FILE_NAME)
        return try {
            QueueFile(file)
        } catch (e: IOException) {
            if (file.delete()) {
                QueueFile(file)
            } else {
                null
            }
        }
    }

    private fun createDirectory(location: File) {
        if (!(location.exists() || location.mkdirs() || location.isDirectory)) {
            throw IOException("Could not create directory at $location")
        }
    }

    companion object {
        private const val DIR_NAME = "analytics-disk-queue"
        private const val FILE_NAME = "analytics.json"
    }
}
