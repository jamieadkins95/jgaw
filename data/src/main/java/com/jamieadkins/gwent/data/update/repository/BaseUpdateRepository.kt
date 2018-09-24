package com.jamieadkins.gwent.data.update.repository

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jamieadkins.gwent.domain.update.repository.UpdateRepository
import io.reactivex.Completable
import io.reactivex.Single
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type

abstract class BaseUpdateRepository(private val filesDirectory: File) : UpdateRepository {

    private val storage = FirebaseStorage.getInstance()
    val gson = Gson()

    override fun performUpdate(): Completable {
        return isUpdateAvailable()
            .flatMapCompletable { updateAvailable ->
                if (updateAvailable) {
                    internalPerformUpdate()
                } else {
                    Completable.complete()
                }
            }
    }

    abstract fun internalPerformUpdate(): Completable

    protected fun getRemoteLastUpdated(patch: String, fileName: String): Single<Long> {
        return getMetaData(getStorageReference(patch, fileName))
            .map { it.updatedTimeMillis }
    }

    protected fun getFileFromFirebase(storageRef: StorageReference, outputFileName: String): Single<File> {
        return Single.create<File> { emitter ->
            val file = File(filesDirectory, outputFileName)
            val taskSnapshotStorageTask = storageRef.getFile(file)
                .addOnSuccessListener { _ ->
                    emitter.onSuccess(file)
                }
                .addOnFailureListener { e ->
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }

            emitter.setCancellable { taskSnapshotStorageTask.cancel() }
        }
    }

    protected fun getMetaData(storageRef: StorageReference): Single<StorageMetadata> {
        return Single.create<StorageMetadata> { emitter ->
            storageRef.metadata
                .addOnSuccessListener { taskSnapshot ->
                    emitter.onSuccess(taskSnapshot)
                }
                .addOnFailureListener { e ->
                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                    }
                }
        }
    }

    protected fun getStorageReference(patch: String, fileName: String): StorageReference {
        return getStorageReference(patch).child(fileName)
    }

    protected fun getStorageReference(patch: String): StorageReference {
        return storage.reference.child("card-data").child(patch)
    }

    protected fun <T> parseJsonFile(file: File, type: Type = genericType<T>()): Single<T> {
        return Single.fromCallable {
            val bufferedReader = BufferedReader(FileReader(file))
            gson.fromJson<T>(bufferedReader, type)
        }
    }

    fun <T> genericType() = object : TypeToken<T>() {}.type
}