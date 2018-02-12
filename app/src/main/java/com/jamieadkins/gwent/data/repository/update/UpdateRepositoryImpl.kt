package com.jamieadkins.gwent.data.repository.update

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.jamieadkins.gwent.BuildConfig
import com.jamieadkins.gwent.Constants
import com.jamieadkins.gwent.StoreManager
import com.jamieadkins.gwent.data.card.CardsApi
import com.jamieadkins.gwent.data.repository.FirebaseCardResult
import com.jamieadkins.gwent.data.repository.FirebasePatchResult
import com.jamieadkins.gwent.data.repository.card.CardMapper
import com.jamieadkins.gwent.database.GwentDatabaseProvider
import com.jamieadkins.gwent.database.entity.PatchVersionEntity
import com.jamieadkins.gwent.main.GwentApplication
import com.nytimes.android.external.store3.base.impl.BarCode
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import com.google.firebase.storage.*
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader


class UpdateRepositoryImpl : UpdateRepository {

    private companion object {
        const val CARD_FILE_NAME = "cards"
        const val CATEGORIES_FILE_NAME = "categories"
        const val KEYWORDS_FILE_NAME = "keywords"
    }

    private val storage = FirebaseStorage.getInstance()
    private val database = GwentDatabaseProvider.getDatabase(GwentApplication.INSTANCE.applicationContext)
    private val cardsApi = Retrofit.Builder()
            .baseUrl(Constants.CARDS_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(StoreManager.provideGson()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .validateEagerly(BuildConfig.DEBUG)
            .build()
            .create(CardsApi::class.java)

    override fun isUpdateAvailable(): Single<Boolean> {
        return getLatestPatch()
                .map {
                    val storedPatch = getCachedPatch(it.patch)
                    storedPatch == null || it.patch != storedPatch.patch || it.lastUpdated > storedPatch.lastUpdated
                }
                .doOnSuccess { update -> if (update) log("Update Available.") else log("Up to date.") }
    }

    private fun getLatestPatch(): Single<PatchVersionEntity> {
        val barcode = BarCode(Constants.CACHE_KEY, StoreManager.generateId("patch", BuildConfig.CARD_DATA_VERSION))
        return StoreManager.getDataOnce<FirebasePatchResult>(barcode, cardsApi.fetchPatch(BuildConfig.CARD_DATA_VERSION), FirebasePatchResult::class.java, 10)
                .flatMap { patch ->
                    getLastUpdated(patch.patch).map { Pair(patch, it) }
                }
                .map { PatchVersionEntity(it.first.patch, it.first.name, it.second) }
                .doOnSuccess { log("Latest Patch: " + it.patch + " " + it.lastUpdated) }
                .observeOn(Schedulers.io())
    }

    private fun getLastUpdated(patch: String): Single<Long> {
        return getMetaData(getStorageReference(patch, CARD_FILE_NAME))
                .map { it.updatedTimeMillis }
    }

    private fun getCachedPatch(patch: String): PatchVersionEntity? {
        val cached = database.patchDao().getPatchVersion(patch)
        log("Database Patch: " + cached?.patch + " " + cached?.lastUpdated)
        return cached
    }

    override fun performUpdate(): Completable {
        return getLatestPatch()
                .flatMap { getFileFromFirebase(getStorageReference(it.patch, CARD_FILE_NAME)) }
                .flatMapCompletable { performUpdate(it) }
    }

    private fun performUpdate(file: File): Completable {
        return getLatestPatch()
                .flatMap { patch -> parseJsonFile(file).map { Pair(patch, it) } }
                .flatMapCompletable { updateDatabase(it.first, it.second) }
    }

    private fun updateDatabase(newPatch: PatchVersionEntity, cardList: FirebaseCardResult): Completable {
        return Completable.fromCallable {
            val cards = CardMapper.cardEntityListFromApiResult(cardList)
            log("Inserting " + cards.size + " cards into database.")
            database.cardDao().insertCards(cards)
            database.patchDao().insertPatchVersion(newPatch)
        }
    }

    override fun getNewCardData(): Single<File> {
        return getLatestPatch()
                .flatMap { getFileFromFirebase(getStorageReference(it.patch, CARD_FILE_NAME)) }
    }

    private fun getFileFromFirebase(storageRef: StorageReference): Single<File> {
        return Single.create<File> { emitter ->
            val file = File(GwentApplication.INSTANCE.filesDir, "cards.json")
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

    private fun getMetaData(storageRef: StorageReference): Single<StorageMetadata> {
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

    private fun getStorageReference(patch: String, fileName: String): StorageReference {
        return storage.reference.child("card-data").child(patch).child("$fileName.json")
    }

    private fun parseJsonFile(file: File): Single<FirebaseCardResult> {
        return Single.fromCallable {
            log("Parsing JSON from ${file.absolutePath}")
            val bufferedReader = BufferedReader(FileReader(file))
            Gson().fromJson(bufferedReader, FirebaseCardResult::class.java)
        }
    }

    private fun log(message: String) {
        Crashlytics.log(message)
        Timber.d(message)
    }
}