package com.jamieadkins.gwent.data.update.repository

import android.content.res.AssetManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.jamieadkins.gwent.card.data.ApiMapper
import com.jamieadkins.gwent.card.data.ArtApiMapper
import com.jamieadkins.gwent.card.data.model.FirebaseCardResult
import com.jamieadkins.gwent.database.GwentDatabase
import com.jamieadkins.gwent.domain.card.repository.CardRepository
import com.jamieadkins.gwent.domain.patch.PatchRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Named

class CardUpdateRepository @Inject constructor(
    private val database: GwentDatabase,
    @Named("files") filesDirectory: File,
    private val patchRepository: PatchRepository,
    private val cardMapper: ApiMapper,
    private val artMapper: ArtApiMapper,
    private val preferences: RxSharedPreferences,
    private val assetManager: AssetManager,
    private val cardRepository: CardRepository
) : BaseUpdateRepository(filesDirectory, preferences) {

    override val FILE_NAME = "cards.json"

    override fun isUpdateAvailable(): Observable<Boolean> {
        val databaseOutOfDate = preferences.getInteger(LAST_DATABASE_VERSION_KEY)
            .asObservable()
            .first(0)
            .map { it < GwentDatabase.DATABASE_VERSION }
            .onErrorReturnItem(false)

        val cardsFileOutOfDate = patchRepository.getLatestRoachPatch()
            .flatMap { patch ->
                Single.zip(
                    getRemoteLastUpdated(patch, FILE_NAME),
                    getLocalLastUpdated(patch, FILE_NAME),
                    BiFunction { remote: Long, local: Long ->
                        remote > local
                    }
                )
            }
            .onErrorReturnItem(false)

        return updateStateChanges
            .flatMapSingle {
                Single.zip(
                    cardsFileOutOfDate, databaseOutOfDate,
                    BiFunction { cardsFileNeedsUpdating: Boolean, databaseNeedsUpdating: Boolean ->
                        cardsFileNeedsUpdating || databaseNeedsUpdating
                    }
                )
            }
    }

    override fun internalPerformFirstTimeSetup(): Completable {
        return Single.fromCallable {
            with(assetManager.open(FILE_NAME)) {
                val reader = InputStreamReader(this, "UTF-8")
                gson.fromJson(reader, FirebaseCardResult::class.java)
            }
        }.flatMapCompletable(::updateCardDatabase)
    }

    override fun internalPerformUpdate(): Completable {
        return patchRepository.getLatestRoachPatch()
            .flatMap { getFileFromFirebase(getStorageReference(it, FILE_NAME), FILE_NAME) }
            .observeOn(Schedulers.io())
            .flatMap { parseJsonFile<FirebaseCardResult>(it, FirebaseCardResult::class.java) }
            .flatMapCompletable { updateCardDatabase(it) }
            // Invalidate the card memory cache that has cards from the old patch.
            .doOnComplete { cardRepository.invalidateMemoryCache() }
            .andThen(updateLastUpdated())
            // Finally, note that we are up to date with the database schema.
            .doOnComplete { preferences.getInteger(LAST_DATABASE_VERSION_KEY).set(GwentDatabase.DATABASE_VERSION) }
    }

    override fun hasDoneFirstTimeSetup(): Observable<Boolean> {
        return database.cardDao().count().toObservable().map { it > 0 }
    }

    private fun updateCardDatabase(cardList: FirebaseCardResult): Completable {
        return Completable.fromCallable {
            val cards = cardMapper.map(cardList)
            database.cardDao().insertCards(cards)
            database.artDao().insertArt(artMapper.map(cardList))
        }
    }

    private fun updateLastUpdated(): Completable {
        return patchRepository.getLatestRoachPatch()
            .flatMapCompletable { updateLocalLastUpdated(it, FILE_NAME) }
    }

    companion object {
        private const val LAST_DATABASE_VERSION_KEY = "DB_VERSION_KEY"
    }
}